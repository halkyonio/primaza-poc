package io.halkyon.services;

import static io.halkyon.utils.StringUtils.equalsIgnoreCase;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;

import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceList;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.halkyon.model.Cluster;

@ApplicationScoped
public class KubernetesClientService {
    /**
     * Get the deployments that are installed in the cluster.
     * TODO: For OpenShift, we should support DeploymentConfig.
     */
    public List<Deployment> getDeploymentsInCluster(Cluster cluster) {
        return getClientForCluster(cluster).apps().deployments().list().getItems();
    }

    /**
     * Check whether a service with <protocol>:<port> is running in the cluster.
     */
    public boolean isServiceRunningInCluster(Cluster cluster, String protocol, String servicePort) {
        ServiceList services = getClientForCluster(cluster).services().list();
        for (Service service : services.getItems()) {
            boolean found = service.getSpec().getPorts().stream()
                    .anyMatch(p -> equalsIgnoreCase(p.getProtocol(), protocol)
                            && String.valueOf(p.getPort()).equals(servicePort));
            if (found) {
                return true;
            }
        }

        return false;
    }

    private KubernetesClient getClientForCluster(Cluster cluster) {
        Config config;
        if (cluster.kubeConfig != null && !cluster.kubeConfig.isEmpty()) {
            config = Config.fromKubeconfig(cluster.kubeConfig);
        } else {
            config = Config.empty();
        }

        config.setMasterUrl(cluster.url);

        return new DefaultKubernetesClient(config);
    }
}
