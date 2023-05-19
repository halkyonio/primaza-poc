package io.halkyon;

import static io.halkyon.utils.StringUtils.getHostFromUrl;
import static io.halkyon.utils.StringUtils.getPortFromUrl;
import static io.halkyon.utils.StringUtils.toBase64;
import static java.net.HttpURLConnection.HTTP_INTERNAL_ERROR;
import static java.net.HttpURLConnection.HTTP_OK;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.server.mock.KubernetesServer;
import io.halkyon.exceptions.ClusterConnectException;
import io.halkyon.model.Application;
import io.halkyon.model.Claim;
import io.halkyon.model.Cluster;
import io.halkyon.services.KubernetesClientService;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kubernetes.client.KubernetesTestServer;
import io.quarkus.test.kubernetes.client.WithKubernetesTestServer;

@WithKubernetesTestServer(crud = false)
@QuarkusTest
public class KubernetesClientServiceTest {

    @KubernetesTestServer
    KubernetesServer mockServer;

    @Inject
    KubernetesClientService kubernetesClientService;

    @Test
    public void checkThatSecretIsRemovedWhenMountingFails() throws ClusterConnectException, URISyntaxException {
        // Given an application, his corresponding deployment and a secret
        Cluster cluster = new Cluster();
        cluster.name = "mockServer";
        cluster.url = mockServer.getClient().getMasterUrl().toString();
        cluster.namespace = mockServer.getClient().getNamespace();

        Application app = new Application();
        app.name = "test2-app";
        app.namespace = "test";
        app.image = "localhost:5000/amunozhe/atomic-fruits:1.0.0";
        app.cluster = cluster;

        Claim claim = new Claim();
        claim.name = "test2-claim";
        claim.application = app;
        app.claim = claim;

        String secretName = "test2-app-secret";
        Map<String, String> secretData = new HashMap<>();
        String url = "testbind://consul:1111";
        secretData.put("host", toBase64(getHostFromUrl(url)));
        secretData.put("port", toBase64(getPortFromUrl(url)));
        secretData.put("url", toBase64(url));
        secretData.put("username", toBase64("username"));
        secretData.put("password", toBase64("password"));

        Deployment deployment = buildNewDeployment("test2-app", "atomic-fruits:1.0.0");

        // Kubernetes client will retry if an error is thrown, we avoid this because we are explicitly triggering this
        // error above.
        System.setProperty("kubernetes.request.retry.backoffLimit", "0");

        new KubernetesClientBuilder().withConfig(
                new ConfigBuilder(mockServer.getClient().getConfiguration()).withRequestRetryBackoffLimit(0).build())
                .build();

        // We will work in `Expectations mode` in order to set up expectations for the behavior we want when a certain
        // Kubernetes resource endpoints the KubernetesClient requests:
        // - secret creation on the cluster goes well, the secret is created
        mockServer.expect().post().withPath("/api/v1/namespaces/test/secrets")
                .andReturn(HTTP_OK, new SecretBuilder().withNewMetadata().withName(secretName).withNamespace("test")
                        .endMetadata().withData(secretData).build())
                .once();
        // - the application deployment is found for secret mounting
        mockServer.expect().get().withPath("/apis/apps/v1/namespaces/test/deployments/test2-app")
                .andReturn(HTTP_OK, deployment).once();

        // - the secret mounting fails
        mockServer.expect().post().withPath("/apis/apps/v1/namespaces/test/deployments")
                .andReturn(HttpURLConnection.HTTP_INTERNAL_ERROR, "{error: impossible to patch deployment}").once();

        // - the secret is deleted because the mounting failed.
        mockServer.expect().delete().withPath("/api/v1/namespaces/test/secrets/test2-app-secret")
                .andReturn(HTTP_INTERNAL_ERROR, "{error: unexpected response status code 500}").once();

        Exception exception = assertThrows(KubernetesClientException.class,
                () -> kubernetesClientService.mountSecretInApplication(claim.application, secretData));

        assertTrue(exception.getMessage().contains("Failure executing: DELETE"));

    }

    @Test
    public void checkThatSecretMountingGoesWell() throws ClusterConnectException, URISyntaxException {
        // Given an application, his corresponding deployment and a secret
        String prefix = "checkThatSecretMountingGoesWell";

        Cluster cluster = new Cluster();
        cluster.name = "mockServer";
        cluster.url = mockServer.getClient().getMasterUrl().toString();
        cluster.namespace = mockServer.getClient().getNamespace();

        Application app = new Application();
        app.name = prefix + "-app";
        app.namespace = "test";
        app.image = "localhost:5000/amunozhe/atomic-fruits:1.0.0";
        app.cluster = cluster;

        Claim claim = new Claim();
        claim.name = prefix + "-claim";
        claim.application = app;
        app.claim = claim;

        String secretName = prefix + "-app" + prefix + "-claim";
        Map<String, String> secretData = new HashMap<>();
        String url = "testbind://consul:1111";
        secretData.put("host", toBase64(getHostFromUrl(url)));
        secretData.put("port", toBase64(getPortFromUrl(url)));
        secretData.put("url", toBase64(url));
        secretData.put("username", toBase64("username"));
        secretData.put("password", toBase64("password"));

        Deployment deployment = buildNewDeployment(app.name, "atomic-fruits:1.0.0");

        // We will work in `Expectations mode` in order to set up expectations for the behavior we want when a certain
        // Kubernetes resource endpoints the KubernetesClient requests:
        // - secret creation on the cluster goes well, the secret is created
        mockServer.expect().post().withPath("/api/v1/namespaces/test/secrets")
                .andReturn(HTTP_OK, new SecretBuilder().withNewMetadata().withName(secretName).withNamespace("test")
                        .endMetadata().withData(secretData).build())
                .once();
        // - the application deployment is found for secret mounting
        mockServer.expect().get().withPath("/apis/apps/v1/namespaces/test/deployments/" + app.name)
                .andReturn(HTTP_OK, deployment).once();

        // - the secret mounting goes well
        mockServer.expect().post().withPath("/apis/apps/v1/namespaces/test/deployments")
                .andReturn(HttpURLConnection.HTTP_OK, deployment).once();

        // - We set up an error if a secret remove is done. That shouldn't happen
        mockServer.expect().delete().withPath("/api/v1/namespaces/test/secrets/" + secretName)
                .andReturn(HTTP_INTERNAL_ERROR, "{error: unexpected response status code 500}").once();

        kubernetesClientService.mountSecretInApplication(claim.application, secretData);

    }

    private static Deployment buildNewDeployment(String name, String image) {
        return new DeploymentBuilder().withNewMetadata().withName(name).addToAnnotations("foo", "bar")
                .addToAnnotations("app", "nginx").endMetadata().withNewSpec().withNewTemplate().withNewSpec()
                .addNewContainer().withName(name + "-container").withImage(image).endContainer().endSpec().endTemplate()
                .endSpec().build();
    }
}
