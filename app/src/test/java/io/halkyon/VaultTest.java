package io.halkyon;

import static io.halkyon.utils.TestUtils.createClaim;
import static io.halkyon.utils.TestUtils.createCluster;
import static io.halkyon.utils.TestUtils.createCredential;
import static io.halkyon.utils.TestUtils.createService;
import static io.halkyon.utils.TestUtils.findApplicationByName;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import javax.inject.Inject;
import javax.ws.rs.core.MediaType;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.halkyon.exceptions.ClusterConnectException;
import io.halkyon.model.Application;
import io.halkyon.model.Claim;
import io.halkyon.model.Cluster;
import io.halkyon.model.Service;
import io.halkyon.services.ApplicationDiscoveryJob;
import io.halkyon.services.ClaimStatus;
import io.halkyon.services.KubernetesClientService;
import io.halkyon.services.ServiceDiscoveryJob;
import io.halkyon.services.UpdateClaimJob;
import io.halkyon.utils.ApplicationNameMatcher;
import io.halkyon.utils.ClaimNameMatcher;
import io.halkyon.utils.ClusterNameMatcher;
import io.halkyon.utils.SecretDataMatcher;
import io.halkyon.utils.WebPageExtension;
import io.quarkus.scheduler.Scheduler;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.quarkus.vault.VaultKVSecretEngine;

@QuarkusTest
@QuarkusTestResource(WebPageExtension.class)
public class VaultTest {

    WebPageExtension.PageManager page;

    @InjectMock
    KubernetesClientService mockKubernetesClientService;

    @Inject
    ApplicationDiscoveryJob applicationDiscoveryJob;

    @Inject
    UpdateClaimJob claimingServiceJob;

    @Inject
    ServiceDiscoveryJob serviceDiscoveryJob;

    @Inject
    Scheduler scheduler;

    @Inject
    VaultKVSecretEngine kvSecretEngine;

    @Test
    public void testBindApplication() throws ClusterConnectException {
        pauseScheduler();
        // names
        String prefix = "ApplicationsPageTest.testBindApplication.";
        String clusterName = prefix + "cluster";
        String claimName = prefix + "claim";
        String serviceName = prefix + "service";
        String credentialName = prefix + "credential";
        String appName = prefix + "app";
        String username = "user1";
        String password = "pass1";
        // mock data
        configureMockServiceFor(clusterName, "testbind", "1111", "ns1");
        configureMockApplicationFor(clusterName, appName, "image2", "ns1");
        // create data
        Service service = createService(serviceName, "version", "type", "testbind:1111");
        createCredential(credentialName, service.id, "user1", "pass1", "myapps/app");
        createCluster(clusterName, "host:port");

        Map<String, String> newsecrets = new HashMap<>();
        newsecrets.put(username, password);
        kvSecretEngine.writeSecret("myapps/app", newsecrets);
        Map<String, String> secret = kvSecretEngine.readSecret("myapps/app");
        String secrets = new TreeMap<>(secret).toString();
        assertEquals("{user1=pass1}", secrets);

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
        Application app = findApplicationByName(appName);
        page.clickById("btn-application-bind-" + app.id);
        // modal should be displayed
        page.assertContentContains("Bind Application " + appName);
        // select our claim
        page.select("application_bind_claim", claimName);
        // click on bind
        page.clickById("application-bind-button");

        // Verify the Claim has been updated with service credential and url, also status to BOUND
        Claim actualClaim = given().contentType(MediaType.APPLICATION_JSON).get("/claims/name/" + claimName).then()
                .statusCode(200).extract().as(Claim.class);

        assertNotNull(actualClaim.credential);
        assertEquals("user1", actualClaim.credential.username);
        assertEquals("pass1", actualClaim.credential.password);
        assertEquals(ClaimStatus.BOUND.toString(), actualClaim.status);

        // protocol://service_name:port
        assertNotNull(actualClaim.url);
        String expectedUrl = "testbind://" + serviceName + ":1111";
        assertEquals(expectedUrl, actualClaim.url);

        // then secret should have been generated
        verify(mockKubernetesClientService, times(1)).mountSecretInApplication(argThat(new ClaimNameMatcher(claimName)),
                argThat(new SecretDataMatcher(expectedUrl, "user1", "pass1")));
        // and application should have been rolled out.
        verify(mockKubernetesClientService, times(1)).rolloutApplication(argThat(new ApplicationNameMatcher(appName)));
    }

    private void configureMockApplicationWithEmptyFor(Cluster cluster) throws ClusterConnectException {
        Mockito.when(mockKubernetesClientService.getDeploymentsInCluster(argThat(new ClusterNameMatcher(cluster.name))))
                .thenReturn(Collections.emptyList());
    }

    private void configureMockApplicationFor(String clusterName, String appName, String appImage, String appNamespace)
            throws ClusterConnectException {
        Mockito.when(mockKubernetesClientService.getDeploymentsInCluster(argThat(new ClusterNameMatcher(clusterName))))
                .thenReturn(Arrays
                        .asList(new DeploymentBuilder().withNewMetadata().withName(appName).withNamespace(appNamespace)
                                .endMetadata().withNewSpec().withNewTemplate().withNewSpec().addNewContainer()
                                .withImage(appImage).endContainer().endSpec().endTemplate().endSpec().build()));
    }

    private void configureMockServiceFor(String clusterName, String protocol, String servicePort,
            String serviceNamespace) throws ClusterConnectException {
        configureMockServiceFor(clusterName, protocol, servicePort,
                new ServiceBuilder().withNewMetadata().withNamespace(serviceNamespace).endMetadata());
    }

    private void configureMockServiceWithIngressFor(String clusterName, String protocol, String servicePort,
            String serviceNamespace, String serviceExternalIp) throws ClusterConnectException {
        configureMockServiceFor(clusterName, protocol, servicePort,
                new ServiceBuilder().withNewMetadata().withNamespace(serviceNamespace).endMetadata().withNewStatus()
                        .withNewLoadBalancer().addNewIngress().withIp(serviceExternalIp).endIngress().endLoadBalancer()
                        .endStatus());
    }

    private void configureMockServiceFor(String clusterName, String protocol, String servicePort,
            ServiceBuilder serviceBuilder) throws ClusterConnectException {
        Mockito.when(mockKubernetesClientService.getServiceInCluster(argThat(new ClusterNameMatcher(clusterName)),
                eq(protocol), eq(servicePort))).thenReturn(Optional.of(serviceBuilder.build()));
    }

    private void pauseScheduler() {
        scheduler.pause();
        await().atMost(30, SECONDS).until(() -> !scheduler.isRunning());
    }
}
