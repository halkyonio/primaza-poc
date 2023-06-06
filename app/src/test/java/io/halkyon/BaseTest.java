package io.halkyon;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import jakarta.inject.Inject;

import org.mockito.Mockito;

import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.halkyon.exceptions.ClusterConnectException;
import io.halkyon.model.Cluster;
import io.halkyon.services.KubernetesClientService;
import io.halkyon.utils.ApplicationNameMatcher;
import io.halkyon.utils.ClusterNameMatcher;
import io.halkyon.utils.WebPageExtension;
import io.quarkus.scheduler.Scheduler;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.mockito.InjectMock;

@QuarkusTestResource(WebPageExtension.class)
public abstract class BaseTest {

    WebPageExtension.PageManager page;

    @InjectMock
    KubernetesClientService mockKubernetesClientService;

    @Inject
    Scheduler scheduler;

    protected void configureMockApplicationWithEmptyFor(Cluster cluster) {
        try {
            Mockito.when(
                    mockKubernetesClientService.getDeploymentsInCluster(argThat(new ClusterNameMatcher(cluster.name))))
                    .thenReturn(Collections.emptyList());
        } catch (ClusterConnectException e) {
            throw new RuntimeException(e);
        }
    }

    protected void configureMockApplicationFor(String clusterName, String appName, String appImage,
            String appNamespace) {
        try {
            Mockito.when(
                    mockKubernetesClientService.getDeploymentsInCluster(argThat(new ClusterNameMatcher(clusterName))))
                    .thenReturn(Arrays.asList(
                            new DeploymentBuilder().withNewMetadata().withName(appName).withNamespace(appNamespace)
                                    .endMetadata().withNewSpec().withNewTemplate().withNewSpec().addNewContainer()
                                    .withImage(appImage).endContainer().endSpec().endTemplate().endSpec().build()));
            Mockito.when(mockKubernetesClientService.getIngressHost(argThat(new ApplicationNameMatcher(appName))))
                    .thenReturn("");
        } catch (ClusterConnectException e) {
            throw new RuntimeException(e);
        }
    }

    protected void configureMockApplicationIngressFor(String appName, String appIngress) {
        try {
            Mockito.when(mockKubernetesClientService.getIngressHost(argThat(new ApplicationNameMatcher(appName))))
                    .thenReturn(appIngress);
        } catch (ClusterConnectException e) {
            throw new RuntimeException(e);
        }
    }

    protected void configureMockServiceFor(String clusterName, String protocol, String servicePort,
            String serviceNamespace) {
        configureMockServiceFor(clusterName, protocol, servicePort,
                new ServiceBuilder().withNewMetadata().withNamespace(serviceNamespace).endMetadata());
    }

    protected void configureMockServiceWithIngressFor(String clusterName, String protocol, String servicePort,
            String serviceNamespace, String serviceExternalIp) {
        configureMockServiceFor(clusterName, protocol, servicePort,
                new ServiceBuilder().withNewMetadata().withNamespace(serviceNamespace).endMetadata().withNewStatus()
                        .withNewLoadBalancer().addNewIngress().withIp(serviceExternalIp).endIngress().endLoadBalancer()
                        .endStatus());
    }

    protected void configureMockServiceFor(String clusterName, String protocol, String servicePort,
            ServiceBuilder serviceBuilder) {
        try {
            Mockito.when(mockKubernetesClientService.getServiceInCluster(argThat(new ClusterNameMatcher(clusterName)),
                    eq(protocol), eq(servicePort))).thenReturn(Optional.of(serviceBuilder.build()));
        } catch (ClusterConnectException e) {
            throw new RuntimeException(e);
        }
    }

    protected void pauseScheduler() {
        scheduler.pause();
        await().atMost(30, SECONDS).until(() -> !scheduler.isRunning());
    }
}
