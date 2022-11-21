package io.halkyon;

import static io.halkyon.utils.TestUtils.createCluster;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.argThat;

import java.util.Arrays;
import java.util.Collections;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.halkyon.model.Cluster;
import io.halkyon.services.ApplicationDiscoveryJob;
import io.halkyon.services.KubernetesClientService;
import io.halkyon.utils.ClusterNameMatcher;
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
    ApplicationDiscoveryJob job;

    @Inject
    Scheduler scheduler;

    @Test
    public void testDiscoverApplications(){
        String prefix = "ApplicationsPageTest.testDiscoverApplications.";
        pauseScheduler();
        // create data
        Cluster cluster = createCluster(prefix + "cluster", "host:port1");
        configureMockApplicationFor(cluster, prefix + "app", "ns1", "image1");
        // test the job
        job.execute();
        // now the deployment should be listed in the page
        page.goTo("/applications");
        page.assertContentContains(prefix + "app");
        page.assertContentContains("image1");
        page.assertContentContains(prefix + "cluster");
        // and cluster should have only one application
        assertEquals(1, Cluster.findByName(cluster.name).applications.size());

        // test the same application is not created again
        job.execute();
        // the mocked application is ignored and cluster should still have only one application
        assertEquals(1, Cluster.findByName(cluster.name).applications.size());

        // test that uninstalled deployments are deleted in database
        configureMockApplicationWithEmptyFor(cluster);
        // test the job
        job.execute();
        // now the deployment should NOT be listed in the page
        page.goTo("/applications");
        page.assertContentDoesNotContain(prefix + "app");
        page.assertContentDoesNotContain(prefix + "image");
        page.assertContentDoesNotContain(prefix + "cluster");
    }

    private void configureMockApplicationWithEmptyFor(Cluster cluster) {
        Mockito.when(mockKubernetesClientService.getDeploymentsInCluster(argThat(new ClusterNameMatcher(cluster.name))))
                .thenReturn(Collections.emptyList());
    }

    private void configureMockApplicationFor(Cluster cluster, String appName, String appNamespace, String appImage) {
        Mockito.when(mockKubernetesClientService.getDeploymentsInCluster(argThat(new ClusterNameMatcher(cluster.name))))
                .thenReturn(Arrays.asList(new DeploymentBuilder()
                        .withNewMetadata().withName(appName).withNamespace(appNamespace).endMetadata()
                        .withNewSpec().withNewTemplate().withNewSpec()
                        .addNewContainer().withImage(appImage).endContainer()
                        .endSpec().endTemplate().endSpec()
                        .build()));
    }

    private void pauseScheduler() {
        scheduler.pause();
        await().atMost(30, SECONDS).until(() -> !scheduler.isRunning());
    }
}
