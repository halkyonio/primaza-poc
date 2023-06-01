package io.halkyon.utils;

import static io.restassured.RestAssured.given;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;

import java.util.Optional;

import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.MediaType;

import org.mockito.Mockito;

import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.halkyon.exceptions.ClusterConnectException;
import io.halkyon.model.Application;
import io.halkyon.model.Claim;
import io.halkyon.model.Cluster;
import io.halkyon.model.Credential;
import io.halkyon.model.Service;
import io.halkyon.services.KubernetesClientService;

public final class TestUtils {
    private TestUtils() {
    }

    public static Cluster createClusterWithServiceAvailable(String clusterName, String url,
            KubernetesClientService mockKubernetesClientService, String protocolAvailable, String portAvailable) {
        Cluster cluster = createCluster(clusterName, url);
        mockServiceIsAvailableInCluster(mockKubernetesClientService, clusterName, protocolAvailable, portAvailable);
        return cluster;
    }

    public static Cluster createCluster(String clusterName, String url) {
        given().contentType(MediaType.MULTIPART_FORM_DATA).multiPart("name", clusterName)
                .multiPart("environment", "PROD").multiPart("excludedNamespaces", "kube-system,ingress")
                .multiPart("url", url).when().post("/clusters").then().statusCode(201);

        return given().contentType(MediaType.APPLICATION_JSON).get("/clusters/name/" + clusterName).then()
                .statusCode(200).extract().as(Cluster.class);
    }

    public static Service createService(String serviceName, String serviceVersion, String serviceType) {
        return createService(serviceName, serviceVersion, serviceType, "tcp:5672");
    }

    public static Service createServiceWithCredential(String serviceName, String serviceVersion, String serviceType,
            String endpoint) {
        Service service = createService(serviceName, serviceVersion, serviceType, endpoint);
        createCredential(serviceName + "-credential", service.id, "username", "password", null);
        return service;
    }

    public static Service createService(String serviceName, String serviceVersion, String serviceType,
            String endpoint) {
        given().contentType(MediaType.APPLICATION_FORM_URLENCODED).formParam("name", serviceName)
                .formParam("version", serviceVersion).formParam("type", serviceType).formParam("endpoint", endpoint)
                .when().post("/services").then().statusCode(201);

        return given().contentType(MediaType.APPLICATION_JSON)
                .get("/services/name/" + serviceName + "/version/" + serviceVersion).then().statusCode(200).extract()
                .as(Service.class);
    }

    public static Claim createClaim(String claimName, String serviceRequested) {
        given().contentType(MediaType.APPLICATION_FORM_URLENCODED).formParam("name", claimName)
                .formParam("serviceRequested", serviceRequested).formParam("status", "new")
                .formParam("applicationId", "1").formParam("description", "claim for testing purposes").when()
                .post("/claims").then().statusCode(201);
        return given().contentType(MediaType.APPLICATION_JSON).get("/claims/name/" + claimName).then().statusCode(200)
                .extract().as(Claim.class);

    }

    @Transactional
    public static Application createApplication(String applicationName) {
        Application app = new Application();
        app.name = applicationName;
        app.namespace = applicationName + "-test-ns";
        app.image = "localhost:5000/amunozhe/atomic-fruits:1.0.0";

        Cluster cluster = new Cluster();
        cluster.name = applicationName + "-cluster";
        app.cluster = cluster;
        app.persist();
        return app;
    }

    public static Credential createCredential(String credentialName, long serviceId, String username, String password,
            String vaultPath) {
        given().contentType(MediaType.APPLICATION_FORM_URLENCODED).formParam("name", credentialName)
                .formParam("serviceId", serviceId).formParam("username", username).formParam("password", password)
                .formParam("vaultKvPath", vaultPath).when().post("/credentials").then().statusCode(201);

        return given().contentType(MediaType.APPLICATION_JSON).get("/credentials/name/" + credentialName).then()
                .statusCode(200).extract().as(Credential.class);
    }

    public static void mockServiceIsAvailableInCluster(KubernetesClientService mockKubernetesClientService,
            String clusterName, String protocol, String servicePort) {
        mockServiceIsAvailableInCluster(mockKubernetesClientService, clusterName, protocol, servicePort, "ns1");
    }

    public static void mockServiceIsAvailableInCluster(KubernetesClientService mockKubernetesClientService,
            String clusterName, String protocol, String servicePort, String serviceNamespace) {
        try {
            Mockito.when(mockKubernetesClientService.getServiceInCluster(argThat(new ClusterNameMatcher(clusterName)),
                    eq(protocol), eq(servicePort)))
                    .thenReturn(Optional.of(new ServiceBuilder().withNewMetadata().withNamespace(serviceNamespace)
                            .endMetadata().build()));
        } catch (ClusterConnectException ignored) {
            // ignore exceptions
        }
    }

    public static Application findApplicationByName(String appName) {
        return given().contentType(MediaType.APPLICATION_JSON).get("/applications/name/" + appName).then()
                .statusCode(200).extract().as(Application.class);
    }
}
