package io.halkyon;

import static io.halkyon.utils.TestUtils.createClaim;
import static io.halkyon.utils.TestUtils.createCluster;
import static io.halkyon.utils.TestUtils.createCredential;
import static io.halkyon.utils.TestUtils.createService;
import static io.restassured.RestAssured.given;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.Duration;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Optional;

import javax.inject.Inject;
import javax.ws.rs.core.MediaType;

import io.halkyon.model.Claim;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.halkyon.model.Cluster;
import io.halkyon.model.Service;
import io.halkyon.services.ApplicationDiscoveryJob;
import io.halkyon.services.ClaimingServiceJob;
import io.halkyon.services.KubernetesClientService;
import io.halkyon.services.ServiceDiscoveryJob;
import io.halkyon.utils.ApplicationNameMatcher;
import io.halkyon.utils.ClaimNameMatcher;
import io.halkyon.utils.ClusterNameMatcher;
import io.halkyon.utils.SecretDataMatcher;
import io.halkyon.utils.WebPageExtension;
import io.quarkus.scheduler.Scheduler;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;

@QuarkusTest
@QuarkusTestResource(WebPageExtension.class)
public class ApplicationsPageTest {

    WebPageExtension.PageManager page;

    @InjectMock
    KubernetesClientService mockKubernetesClientService;

    @Inject
    ApplicationDiscoveryJob applicationDiscoveryJob;

    @Inject
    ClaimingServiceJob claimingServiceJob;

    @Inject
    ServiceDiscoveryJob serviceDiscoveryJob;

    @Inject
    Scheduler scheduler;

    @Test
    public void testDiscoverApplications(){
        String prefix = "ApplicationsPageTest.testDiscoverApplications.";
        pauseScheduler();
        // create data
        Cluster cluster = createCluster(prefix + "cluster", "host:port");
        configureMockApplicationFor(cluster.name, prefix + "app", "image1", "ns1");
        // test the job
        applicationDiscoveryJob.execute();
        // now the deployment should be listed in the page
        page.goTo("/applications");
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            page.refresh();
            page.assertContentContains(prefix + "app");
            page.assertContentContains("image1");
            page.assertContentContains(prefix + "cluster");
        });
        // and cluster should have only one application
        assertEquals(1, Cluster.findByName(cluster.name).applications.size());

        // test the same application is not created again
        applicationDiscoveryJob.execute();
        // the mocked application is ignored and cluster should still have only one application
        assertEquals(1, Cluster.findByName(cluster.name).applications.size());

        // test that uninstalled deployments are deleted in database
        configureMockApplicationWithEmptyFor(cluster);
        // test the job
        applicationDiscoveryJob.execute();
        // now the deployment should NOT be listed in the page
        page.goTo("/applications");
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            page.refresh();
            page.assertContentDoesNotContain(prefix + "app");
        });
    }

    @Test
    public void testBindApplication(){
        pauseScheduler();
        // names
        String prefix = "ApplicationsPageTest.testBindApplication.";
        String clusterName = prefix + "cluster";
        String claimName = prefix + "claim";
        String serviceName = prefix + "service";
        String credentialName = prefix + "credential";
        String appName = prefix + "app";
        // mock data
        configureMockServiceFor(clusterName, "testbind", "1111", "ns1");
        configureMockApplicationFor(clusterName, appName, "image2", "ns1");
        // create data
        Service service = createService(serviceName, "version", "type", "testbind:1111");
        createCredential(credentialName, service.id, "user1", "pass1");
        createCluster(clusterName, "host:port");
        serviceDiscoveryJob.execute(); // this action will change the service to available
        createClaim(claimName, serviceName + "-version");
        claimingServiceJob.execute(); // this action will link the claim with the above service
        // test the job to find applications
        applicationDiscoveryJob.execute();
        // now the deployment should be listed in the page
        page.goTo("/applications");
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            page.refresh();
            page.assertContentContains(appName);
        });

        // click on bind button
        page.clickByName("btn-application-bind");
        // modal should be displayed
        page.assertContentContains("Bind Application " + appName);
        // select our claim
        page.select("application_bind_claim", claimName);
        // click on bind
        page.clickById("application-bind-button");

        //Verify the Claim has been updated with service credential and url
        Claim actualClaim = given()
                .contentType(MediaType.APPLICATION_JSON)
                .get("/claims/name/" + claimName)
                .then()
                .statusCode(200)
                .extract().as(Claim.class);

        assertNotNull(actualClaim.credential);
        assertEquals("user1", actualClaim.credential.username);
        assertEquals("pass1",actualClaim.credential.password);

        //protocol://service_name:port
        assertNotNull(actualClaim.url);
        assertEquals("testbind://" + serviceName + ":1111",actualClaim.url);

        // then secret should have been generated
        String url = serviceName + ":1111";
        String urlBase64 = Base64.getEncoder().encodeToString(url.getBytes());
        String userBase64 = Base64.getEncoder().encodeToString("user1".getBytes());
        String pwdBase64 = Base64.getEncoder().encodeToString("pass1".getBytes());
        String typeBase64 = Base64.getEncoder().encodeToString("type1".getBytes());
        verify(mockKubernetesClientService, times(1))
                .mountSecretInApplication(argThat(new ApplicationNameMatcher(appName)),
                        argThat(new ClaimNameMatcher(claimName)),
                        argThat(new SecretDataMatcher(urlBase64,userBase64,pwdBase64,typeBase64)));
        // and application should have been rolled out.
        verify(mockKubernetesClientService, times(1))
                .rolloutApplication(argThat(new ApplicationNameMatcher(appName)));
    }

    private void configureMockApplicationWithEmptyFor(Cluster cluster) {
        Mockito.when(mockKubernetesClientService.getDeploymentsInCluster(argThat(new ClusterNameMatcher(cluster.name))))
                .thenReturn(Collections.emptyList());
    }

    private void configureMockApplicationFor(String clusterName, String appName, String appImage, String appNamespace) {
        Mockito.when(mockKubernetesClientService.getDeploymentsInCluster(argThat(new ClusterNameMatcher(clusterName))))
                .thenReturn(Arrays.asList(new DeploymentBuilder()
                        .withNewMetadata().withName(appName).withNamespace(appNamespace).endMetadata()
                        .withNewSpec().withNewTemplate().withNewSpec()
                        .addNewContainer().withImage(appImage).endContainer()
                        .endSpec().endTemplate().endSpec()
                        .build()));
    }

    private void configureMockServiceFor(String clusterName, String protocol, String servicePort, String serviceNamespace) {
        Mockito.when(mockKubernetesClientService.getServiceInCluster(argThat(new ClusterNameMatcher(clusterName)), eq(protocol), eq(servicePort)))
                .thenReturn(Optional.of(new ServiceBuilder()
                        .withNewMetadata().withNamespace(serviceNamespace).endMetadata()
                        .build()));
    }

    private void pauseScheduler() {
        scheduler.pause();
        await().atMost(30, SECONDS).until(() -> !scheduler.isRunning());
    }
}
