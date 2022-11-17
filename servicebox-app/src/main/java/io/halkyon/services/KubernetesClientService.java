package io.halkyon.services;

import static io.halkyon.utils.StringUtils.equalsIgnoreCase;

import javax.enterprise.context.ApplicationScoped;

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
}
