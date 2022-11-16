package io.halkyon.services;

import javax.enterprise.context.ApplicationScoped;

import io.fabric8.kubernetes.api.model.ListOptions;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceList;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.halkyon.model.Cluster;

@ApplicationScoped
public class KubernetesClientService {

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

    public boolean isServiceRunningInCluster(Cluster cluster, String serviceName, String servicePort) {
        ServiceList services = getClientForCluster(cluster).services().list();
        for (Service service : services.getItems()) {
            if (service.getMetadata().getName().equals(serviceName)) {
                return service.getSpec().getPorts().stream()
                        .anyMatch(p -> String.valueOf(p.getPort()).equals(servicePort));
            }
        }

        return false;
    }
}
