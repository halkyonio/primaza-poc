package io.halkyon;

import static io.halkyon.utils.TestUtils.createCluster;
import static io.halkyon.utils.TestUtils.createService;
import static io.halkyon.utils.TestUtils.mockServiceIsAvailableInCluster;
import static io.restassured.RestAssured.given;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.inject.Inject;
import jakarta.ws.rs.core.MediaType;

import org.junit.jupiter.api.Test;

import io.halkyon.model.Cluster;
import io.halkyon.model.Service;
import io.halkyon.services.KubernetesClientService;
import io.halkyon.services.ServiceDiscoveryJob;
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
    public void testJobShouldMarkClaimAsErrorAfterMaxAttemptsExceeded() {
        pauseScheduler();
        Service service = createService("ServiceDiscoveryJobTest", "Api", "any", "host:1111");

        // When we run the job once:
        // Then:
        // - the service "ServiceDiscoveryJobTest" should keep the available flag to false because it's not available in
        // any cluster yet
        // When we install the service in a cluster
        // And we run the job again.
        // Then:
        // - the service "ServiceDiscoveryJobTest" should be updated with available=true and be linked to the cluster
        // where was installed.
        job.execute();
        given().contentType(MediaType.APPLICATION_JSON).get("/services/name/" + service.name).then().statusCode(200)
                .body("available", is(false));
        Cluster cluster = createCluster("dummy-cluster-1", "master:port");
        configureMockServiceFor(cluster.name, "host", "1111", "ns1");

        job.execute();
        service = given().contentType(MediaType.APPLICATION_JSON).get("/services/name/" + service.name).then()
                .statusCode(200).extract().as(Service.class);
        assertTrue(service.available);
        assertNotNull(service.cluster);
        assertEquals(cluster.name, service.cluster.name);
        // TODO this should be asserted in a the KuberentesClientServiceTest
        // thenServiceIsInTheAvailableServicePage(service);
    }

    @Test
    public void testShouldDiscoveryServiceWhenNewServiceIsCreated() {
        pauseScheduler();
        String serviceName = "ServiceDiscoveryJobTest2";
        Cluster cluster = createCluster("dummy-cluster-2", "master:port");
        configureMockServiceFor(cluster.name, "host", "2222", "ns1");
        given().header("HX-Request", true).contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .formParam("name", serviceName).formParam("version", "any").formParam("type", "Api")
                .formParam("endpoint", "host:2222").when().post("/services").then().statusCode(201);
        Service service = given().contentType(MediaType.APPLICATION_JSON).get("/services/name/" + serviceName).then()
                .statusCode(200).extract().as(Service.class);
        assertTrue(service.available);
        assertNotNull(service.cluster);
        assertEquals(cluster.name, service.cluster.name);
        // TODO this should be asserted in a the KuberentesClientServiceTest
        // thenServiceIsInTheAvailableServicePage(service);
    }

    @Test
    public void testShouldDiscoveryServiceWhenNewClusterIsCreated() {
        pauseScheduler();
        Service service = createService("ServiceDiscoveryJobTest3", "Api", "any", "host:3333");
        configureMockServiceFor("dummy-cluster-3", "host", "3333", "ns1");
        given().header("HX-Request", true).contentType(MediaType.MULTIPART_FORM_DATA)
                .multiPart("name", "dummy-cluster-3").multiPart("environment", "TEST")
                .multiPart("excludedNamespaces", "kube-system,ingress").multiPart("url", "master:port").when()
                .post("/clusters").then().statusCode(201);
        service = given().contentType(MediaType.APPLICATION_JSON).get("/services/name/" + service.name).then()
                .statusCode(200).extract().as(Service.class);
        assertTrue(service.available);
        assertNotNull(service.cluster);
        assertEquals("dummy-cluster-3", service.cluster.name);
        // TODO this should be asserted in a the KuberentesClientServiceTest
        // thenServiceIsInTheAvailableServicePage(service);
    }

    private void thenServiceIsInTheAvailableServicePage(Service expectedService) {
        page.goTo("/services/discovered");
        page.assertContentContains(expectedService.name);
        page.assertContentContains(expectedService.getProtocol() + "://" + expectedService.name + "."
                + expectedService.namespace + ":" + expectedService.getPort());
    }

    private void configureMockServiceFor(String clusterName, String protocol, String servicePort,
            String serviceNamespace) {
        mockServiceIsAvailableInCluster(mockKubernetesClientService, clusterName, protocol, servicePort,
                serviceNamespace);
    }

    private void pauseScheduler() {
        scheduler.pause();
        await().atMost(30, SECONDS).until(() -> !scheduler.isRunning());
    }
}
