package io.halkyon;

import static org.awaitility.Awaitility.await;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkus.kubernetes.client.runtime.KubernetesClientUtils;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class ClaimReconcilerTest {

    public static final String TEST_CLAIM = "test-claim";

    protected final KubernetesClient client = KubernetesClientUtils.createClient();

    @Test
    void reconcileShouldBind() {
        final var claim = new Claim();
        final var metadata = new ObjectMetaBuilder().withName(TEST_CLAIM).withNamespace(client.getNamespace()).build();
        claim.setMetadata(metadata);

        String prefix = "ApplicationsPageTest.testBindApplication.";
        String clusterName = prefix + "cluster";
        String claimName = prefix + "claim";
        String serviceName = prefix + "service";
        String credentialName = prefix + "credential";
        String credentialType = "basic";
        String appName = prefix + "app";

        claim.getSpec().setApplication(prefix + "app");
        // claim.getSpec().setCredential(credentialName);
        claim.getSpec().setService(serviceName);

        client.resource(claim).create();

        await().ignoreException(NullPointerException.class).atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {

            // check that we create the secret
            final var secret = client.secrets().inNamespace(metadata.getNamespace()).withName(appName + "-secret")
                    .get();

            Deployment deployment = client.apps().deployments().inNamespace(metadata.getNamespace()).withName(appName)
                    .get();

        });

    }
}
