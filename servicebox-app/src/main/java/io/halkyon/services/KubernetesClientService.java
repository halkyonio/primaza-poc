package io.halkyon.services;

import static io.halkyon.utils.StringUtils.equalsIgnoreCase;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;

import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentList;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.RollableScalableResource;
import io.fabric8.kubernetes.client.dsl.ServiceResource;
import io.halkyon.model.Application;
import io.halkyon.model.Claim;
import io.halkyon.model.Cluster;

@ApplicationScoped
public class KubernetesClientService {
    /**
     * Get the deployments that are installed in the cluster.
     * TODO: For OpenShift, we should support DeploymentConfig: https://github.com/halkyonio/primaza-poc/issues/136
     */
    public List<Deployment> getDeploymentsInCluster(Cluster cluster) {
        var r = getClientForCluster(cluster).apps().deployments().inAnyNamespace();
        String[] nss = cluster.namespaces.split(",");
        for (var ns : nss) {
            r = (MixedOperation<Deployment, DeploymentList, RollableScalableResource<Deployment>>) r.withoutField("metadata.namespace", ns);
        }
        return r.list().getItems();
//        return getClientForCluster(cluster).apps().deployments().list().getItems();
    }

    /**
     * Check whether a service with <protocol>:<port> is running in the cluster.
     * Exclude the services installed under listed namespaces
     */
    public Optional<Service> getServiceInCluster(Cluster cluster, String protocol, String servicePort) {
        var r = getClientForCluster(cluster).services().inAnyNamespace();
        String[] nss = cluster.namespaces.split(",");
        for (var ns : nss) {
            r = (MixedOperation<Service, ServiceList, ServiceResource<Service>>) r.withoutField("metadata.namespace", ns);
        }
        ServiceList services = r.list();
        for (Service service : services.getItems()) {
            boolean found = service.getSpec().getPorts().stream()
                    .anyMatch(p -> equalsIgnoreCase(p.getProtocol(), protocol)
                            && String.valueOf(p.getPort()).equals(servicePort));
            if (found) {
                return Optional.of(service);
            }
        }

        return Optional.empty();
    }

    /**
     * Add the secret into the specified cluster and namespace.
     */
    public void mountSecretInApplication(Application application, Claim claim, Map<String, String> secretData) {
        KubernetesClient client = getClientForCluster(application.cluster);

        // create secret
        String secretName = application.name + "-" + claim.name;
        client.secrets().create(new SecretBuilder()
                .withNewMetadata()
                .withName(secretName)
                .withNamespace(application.namespace)
                .endMetadata()
                .withData(secretData)
                .build());

        // add volume in the deployment
        Deployment deployment = client.apps().deployments()
                .inNamespace(application.namespace)
                .withName(application.name)
                .get();

        PodSpec pod = deployment.getSpec().getTemplate().getSpec();
        pod.getVolumes().removeIf(v -> Objects.equals(secretName, v.getName()));
        pod.getVolumes().add(new VolumeBuilder()
                .withName(secretName)
                .withNewSecret()
                .withSecretName(secretName)
                .endSecret()
                .build());

        // Add secret values as env var values
        for (Container container : pod.getContainers()) {
            container.getEnvFrom().removeIf(e -> e.getSecretRef() != null
                    && Objects.equals(e.getSecretRef().getName(), secretName));
            container.getEnvFrom().add(new EnvFromSourceBuilder()
                    .withNewSecretRef(secretName, false)
                    .build());
        }

        // update deployment
        client.apps().deployments().createOrReplace(deployment);
    }

    /**
     * Perform a rollout for the specified application.
     */
    public void rolloutApplication(Application application) {
        getClientForCluster(application.cluster)
                .apps().deployments()
                .inNamespace(application.namespace)
                .withName(application.name)
                .rolling().restart();
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
