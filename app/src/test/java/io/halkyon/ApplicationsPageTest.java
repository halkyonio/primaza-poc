package io.halkyon;

import static io.halkyon.utils.TestUtils.createClaim;
import static io.halkyon.utils.TestUtils.createCluster;
import static io.halkyon.utils.TestUtils.createCredential;
import static io.halkyon.utils.TestUtils.createService;
import static io.halkyon.utils.TestUtils.findApplicationByName;
import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.MediaType;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import io.halkyon.exceptions.ClusterConnectException;
import io.halkyon.model.Application;
import io.halkyon.model.Claim;
import io.halkyon.model.Cluster;
import io.halkyon.model.Service;
import io.halkyon.services.ApplicationDiscoveryJob;
import io.halkyon.services.ClaimService;
import io.halkyon.services.ClaimStatus;
import io.halkyon.services.ServiceDiscoveryJob;
import io.halkyon.utils.ApplicationNameMatcher;
import io.halkyon.utils.SecretDataMatcher;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.vault.VaultKVSecretEngine;

@QuarkusTest
public class ApplicationsPageTest extends BaseTest {

    @Inject
    ApplicationDiscoveryJob applicationDiscoveryJob;

    @Inject
    ClaimService claimingServiceJob;

    @Inject
    ServiceDiscoveryJob serviceDiscoveryJob;

    @Inject
    VaultKVSecretEngine kvSecretEngine;

    @Test
    public void shouldRejectClustersWithInvalidUrl() {
        String prefix = "ApplicationsPageTest.shouldRejectClustersWithInvalidUrl.";
        String clusterName = prefix + "cluster";
        given().header("HX-Request", true).contentType(MediaType.MULTIPART_FORM_DATA).multiPart("name", clusterName)
                .multiPart("environment", "PROD").multiPart("excludedNamespaces", "kube-system,ingress")
                .multiPart("url", "host:port").when().post("/clusters").then().statusCode(400);
    }

    @Test
    public void testDiscoverApplications() throws ClusterConnectException {
        String prefix = "ApplicationsPageTest.testDiscoverApplications.";
        String expectedAppIngressHost = "ingress-app-host";
        pauseScheduler();
        // create data
        Cluster cluster = createCluster(prefix + "cluster", "host:9999");
        configureMockApplicationFor(cluster.name, prefix + "app", "image1", "ns1");
        configureMockApplicationIngressFor(prefix + "app", expectedAppIngressHost);
        // test the job
        applicationDiscoveryJob.execute();
        // now the deployment should be listed in the page
        assertApplicationIsDiscovered(prefix, cluster);

        // test the same application is not created again
        applicationDiscoveryJob.execute();
        // the mocked application is ignored and cluster should still have only one application
        Set<Application> appsInCluster = Cluster.findByName(cluster.name).applications;
        assertEquals(1, appsInCluster.size());
        assertEquals(expectedAppIngressHost, appsInCluster.iterator().next().ingress);

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
    public void testEditClusterToExcludeNamespaceWithExistingAppFromPage() throws ClusterConnectException {
        String prefix = "ApplicationsPageTest.testEditClusterToExcludeNamespace.";
        pauseScheduler();
        // create data
        String namespaceToExclude = prefix + "ns";
        Cluster cluster = createCluster(prefix + "cluster", "host:9999");
        configureMockApplicationFor(cluster.name, prefix + "app", "image1", namespaceToExclude);
        // test the job
        applicationDiscoveryJob.execute();
        // now the deployment should be listed in the page
        assertApplicationIsDiscovered(prefix, cluster);
        // because we're not using a real connection, we need to mock the excluded namespaces update behaviour by doing:
        configureMockApplicationWithEmptyFor(cluster);
        // Now, let's go to the clusters page to edit the cluster
        page.goTo("/clusters");
        // Ensure our data is listed
        page.assertContentContains(cluster.name);
        // Let's change the owner
        page.clickById("btn-cluster-edit-" + cluster.id);
        page.assertPathIs("/clusters/" + cluster.id);
        page.assertContentContains("Update Cluster");
        page.assertContentContains(cluster.name);
        page.type("clusterExcludedNamespaces", cluster.excludedNamespaces + "," + namespaceToExclude);
        page.clickById("cluster-button");
        // Verify the entity was properly updated:
        page.assertContentContains("Updated successfully for id: " + cluster.id);
        // Go back to the clusters list and check whether the owner is displayed
        page.goTo("/clusters");
        page.assertContentContains(cluster.name);
        page.assertContentContains(cluster.excludedNamespaces + "," + namespaceToExclude);
        // Now, the application should be gone:
        page.goTo("/applications");
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            page.refresh();
            page.assertContentDoesNotContain(prefix + "app");
            page.assertContentDoesNotContain("image1");
            page.assertContentDoesNotContain(prefix + "cluster");
        });
        // and cluster should have no applications
        assertNoApplicationsInCluster(cluster);
    }

    @Test
    public void testBindApplication() throws ClusterConnectException {
        pauseScheduler();
        // names
        String prefix = "ApplicationsPageTest.testBindApplication.";
        String clusterName = prefix + "cluster";
        String claimName = prefix + "claim";
        String serviceName = prefix + "service";
        String credentialName = prefix + "credential";
        String credentialType = "basic";
        String appName = prefix + "app";
        // mock data
        configureMockServiceFor(clusterName, "testbind", "1111", "ns1");
        configureMockApplicationFor(clusterName, appName, "image2", "ns1");
        // create data
        Service service = createService(serviceName, "version", "type", "testbind:1111");
        createCredential(credentialName, credentialType, service.id, "user1", "pass1", null);
        createCluster(clusterName, "host:9999");
        serviceDiscoveryJob.execute(); // this action will change the service to available
        Claim claim = createClaim(claimName, serviceName + "-version");
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
        verify(mockKubernetesClientService, times(1)).mountSecretInApplication(
                argThat(new ApplicationNameMatcher(appName)),
                argThat(new SecretDataMatcher(expectedUrl, "user1", "pass1")));
        // and application should have been rolled out.
        verify(mockKubernetesClientService, times(1)).rolloutApplication(argThat(new ApplicationNameMatcher(appName)));
    }

    @Test
    public void testBindApplicationUsingServiceFromAnotherCluster() throws ClusterConnectException {
        pauseScheduler();
        // names
        String prefix = "ApplicationsPageTest.testBindApplicationUsingServiceFromAnotherCluster.";
        String clusterNameOfApplication = prefix + "cluster-app";
        String clusterNameOfService = prefix + "cluster-svc";
        String claimName = prefix + "claim";
        String serviceName = prefix + "service";
        String credentialName = prefix + "credential";
        String credentialType = "basic";
        String appName = prefix + "app";
        String externalServiceIp = serviceName + "ip";

        // mock data
        configureMockServiceWithIngressFor(clusterNameOfService, "testbind", "1111", "ns1", externalServiceIp);
        configureMockApplicationFor(clusterNameOfApplication, appName, "image2", "ns1");

        // create data
        Service service = createService(serviceName, "version", "type", "testbind:1111");
        createCredential(credentialName, credentialType, service.id, "user1", "pass1", null);
        createCluster(clusterNameOfService, "host:9999");
        createCluster(clusterNameOfApplication, "host:9999");
        serviceDiscoveryJob.execute(); // this action will change the service to available
        Claim claim = createClaim(claimName, serviceName + "-version");
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

        // Verify the Claim has been updated with service credential and url
        Claim actualClaim = given().contentType(MediaType.APPLICATION_JSON).get("/claims/name/" + claimName).then()
                .statusCode(200).extract().as(Claim.class);

        assertNotNull(actualClaim.credential);
        assertEquals("user1", actualClaim.credential.username);
        assertEquals("pass1", actualClaim.credential.password);
        assertEquals(ClaimStatus.BOUND.toString(), actualClaim.status);

        // protocol://externalServiceIp:port
        assertNotNull(actualClaim.url);
        String expectedUrl = "testbind://" + externalServiceIp + ":1111";
        assertEquals(expectedUrl, actualClaim.url);

        // then secret should have been generated
        verify(mockKubernetesClientService, times(1)).mountSecretInApplication(
                argThat(new ApplicationNameMatcher(appName)),
                argThat(new SecretDataMatcher(expectedUrl, "user1", "pass1")));
        // and application should have been rolled out.
        verify(mockKubernetesClientService, times(1)).rolloutApplication(argThat(new ApplicationNameMatcher(appName)));
    }

    @Test
    public void testCreateClaimFromApplicationsPage() throws ClusterConnectException {
        pauseScheduler();
        // names
        String prefix = "ApplicationsPageTest.testCreateClaimFromApplicationsPage.";
        String clusterName = prefix + "cluster";
        String claimName = prefix + "claim";
        String appName = prefix + "app";
        // mock data
        configureMockApplicationFor(clusterName, appName, "image2", "ns1");
        // create data
        createCluster(clusterName, "host:9999");
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
        // click on new claim button
        page.clickById("application-new-claim-button");
        // claim form should be displayed
        page.assertPathIs("/claims/new?applicationId=" + app.id);
        // add new claim
        page.type("name", claimName);
        page.type("claim_serviceRequested", "service-version");
        page.clickById("claim-button");
        // Verify the entity was properly created:
        page.assertContentContains("Created successfully for id");
        page.clickById("back");
        page.assertPathIs("/applications");
        // claim is pending which is expected
        page.assertContentContains("pending");
    }

    @Test
    @Disabled("Disable until version 3.4.0 of Vault extension is released because there is an SSL problem")
    public void testBindApplicationGettingCredentialsFromVault() throws ClusterConnectException {
        pauseScheduler();
        // names
        String prefix = "ApplicationsPageTest.testBindApplicationGettingCredentialsFromVault.";
        String clusterName = prefix + "cluster";
        String claimName = prefix + "claim";
        String serviceName = prefix + "service";
        String credentialName = prefix + "credential";
        String credentialType = "vault";
        String appName = prefix + "app";
        String username = "user1";
        String password = "pass1";
        String database = "database1";
        // mock data
        configureMockServiceFor(clusterName, "testbind", "1111", "ns1");
        configureMockApplicationFor(clusterName, appName, "image2", "ns1");
        // create data
        Service service = createService(serviceName, "version", "type", "testbind:1111");
        createCredential(credentialName, credentialType, service.id, null, null, "myapps/app");
        createCluster(clusterName, "host:9999");

        Map<String, String> newsecrets = new HashMap<>();
        newsecrets.put("username", username);
        newsecrets.put("password", password);
        newsecrets.put("database", database);
        kvSecretEngine.writeSecret("myapps/app", newsecrets);
        Map<String, String> secret = kvSecretEngine.readSecret("myapps/app");
        String secrets = new TreeMap<>(secret).toString();
        assertEquals("{database=database1, password=pass1, username=user1}", secrets);

        serviceDiscoveryJob.execute(); // this action will change the service to available
        Claim claim = createClaim(claimName, serviceName + "-version");
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
        verify(mockKubernetesClientService, times(1)).mountSecretInApplication(
                argThat(new ApplicationNameMatcher(appName)),
                argThat(new SecretDataMatcher(expectedUrl, "user1", "pass1")));
        // and application should have been rolled out.
        verify(mockKubernetesClientService, times(1)).rolloutApplication(argThat(new ApplicationNameMatcher(appName)));
    }

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void assertNoApplicationsInCluster(Cluster cluster) {
        assertEquals(0, Cluster.findByName(cluster.name).applications.size());
    }

    private void assertApplicationIsDiscovered(String prefix, Cluster cluster) {
        page.goTo("/applications");
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            page.refresh();
            page.assertContentContains(prefix + "app");
            page.assertContentContains("image1");
            page.assertContentContains(prefix + "cluster");
        });
        // and cluster should have only one application
        assertEquals(1, Cluster.findByName(cluster.name).applications.size());
    }
}
