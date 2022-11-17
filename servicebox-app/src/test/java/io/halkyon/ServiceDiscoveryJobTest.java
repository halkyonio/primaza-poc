package io.halkyon;

import static io.halkyon.utils.TestUtils.createCluster;
import static io.halkyon.utils.TestUtils.createService;
import static io.restassured.RestAssured.given;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;

import javax.inject.Inject;
import javax.ws.rs.core.MediaType;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.halkyon.model.Cluster;
import io.halkyon.model.Service;
import io.halkyon.services.ClaimStatus;
import io.halkyon.services.KubernetesClientService;
import io.halkyon.services.ServiceDiscoveryJob;
import io.halkyon.utils.ClusterNameMatcher;
import io.halkyon.utils.WebPageExtension;
import io.quarkus.scheduler.Scheduler;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;

@QuarkusTest
@QuarkusTestResource(WebPageExtension.class)
public class ServiceDiscoveryJobTest {

    @InjectMock
    KubernetesClientService mockKubernetesClientService;

    @Inject
    ServiceDiscoveryJob job;

    @Inject
    Scheduler scheduler;

    WebPageExtension.PageManager page;

    @Test
    public void testJobShouldMarkClaimAsErrorAfterMaxAttemptsExceeded(){
        pauseScheduler();
        Service service = createService("ServiceDiscoveryJobTest", "any", "host:1111");

        // When we run the job once:
        // Then:
        // - the service "ServiceDiscoveryJobTest" should keep the deployed flag to false because it's not deployed in any cluster yet
        // When we install the service in a cluster
        // And we run the job again.
        // Then:
        // - the service "ServiceDiscoveryJobTest" should be updated with deployed=true and be linked to the cluster where was installed.
        job.execute();
        given()
                .contentType(MediaType.APPLICATION_JSON)
                .get("/services/name/" + service.name)
                .then()
                .statusCode(200)
                .body("deployed", is(false));
        Cluster cluster = createCluster("dummy-cluster-1", "master:port");
        configureMockServiceFor(cluster.name, "host", "1111");

        job.execute();
        given()
                .contentType(MediaType.APPLICATION_JSON)
                .get("/services/name/" + service.name)
                .then()
                .statusCode(200)
                .body("deployed", is(true))
                .body("cluster.name", is(cluster.name));
        thenServiceIsInTheDeployedServicePage(service.name);
    }

    @Test
    public void testShouldDiscoveryServiceWhenNewServiceIsCreated(){
        pauseScheduler();
        String serviceName = "ServiceDiscoveryJobTest2";
        Cluster cluster = createCluster("dummy-cluster-2", "master:port");
        configureMockServiceFor(cluster.name, "host", "2222");
        given()
                .header("HX-Request", true)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .formParam("name", serviceName)
                .formParam("version", "any")
                .formParam("endpoint", "host:2222")
                .when().post("/services")
                .then()
                .statusCode(201);
        given()
                .contentType(MediaType.APPLICATION_JSON)
                .get("/services/name/" + serviceName)
                .then()
                .statusCode(200)
                .body("deployed", is(true))
                .body("cluster.name", is(cluster.name));
        thenServiceIsInTheDeployedServicePage(serviceName);
    }

    @Test
    public void testShouldDiscoveryServiceWhenNewClusterIsCreated(){
        pauseScheduler();
        Service service = createService("ServiceDiscoveryJobTest3", "any", "host:3333");
        configureMockServiceFor("dummy-cluster-3", "host", "3333");
        given()
                .header("HX-Request", true)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .multiPart("name", "dummy-cluster-3")
                .multiPart("environment", "TEST")
                .multiPart("url", "master:port")
                .when().post("/clusters")
                .then()
                .statusCode(201);
        given()
                .contentType(MediaType.APPLICATION_JSON)
                .get("/services/name/" + service.name)
                .then()
                .statusCode(200)
                .body("deployed", is(true))
                .body("cluster.name", is("dummy-cluster-3"));
        thenServiceIsInTheDeployedServicePage(service.name);
    }

    private void thenServiceIsInTheDeployedServicePage(String expectedServiceName) {
        page.goTo("/services/discovered");
        page.assertContentContains(expectedServiceName);
    }

    private void configureMockServiceFor(String clusterName, String serviceName, String servicePort) {
        Mockito.when(mockKubernetesClientService.isServiceRunningInCluster(argThat(new ClusterNameMatcher(clusterName)), eq(serviceName), eq(servicePort)))
                .thenReturn(true);
    }

    private void pauseScheduler() {
        scheduler.pause();
        await().atMost(30, SECONDS).until(() -> !scheduler.isRunning());
    }
}
