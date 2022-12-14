package io.halkyon.services;

import static io.halkyon.utils.StringUtils.equalsIgnoreCase;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;

import org.jboss.logging.Logger;

import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.api.model.PodSpecBuilder;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.FilterWatchListDeletable;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.halkyon.model.Application;
import io.halkyon.model.Claim;
import io.halkyon.model.Cluster;

@ApplicationScoped
public class KubernetesClientService {
    private static Logger LOG = Logger.getLogger(KubernetesClientService.class);

    private static final String SERVICE_BINDING_ROOT = "SERVICE_BINDING_ROOT";
    private static final String SERVICE_BINDING_ROOT_DEFAULT_VALUE = "/bindings";

    /**
     * Get the deployments that are installed in the cluster. TODO: For OpenShift, we should support DeploymentConfig:
     * https://github.com/halkyonio/primaza-poc/issues/136
     */
    public List<Deployment> getDeploymentsInCluster(Cluster cluster) {
        return filterByCluster(getClientForCluster(cluster).apps().deployments(), cluster);
    }

    /**
     * Check whether a service with <protocol>:<port> is running in the cluster. Exclude the services installed under
     * listed namespaces
     */
    public Optional<Service> getServiceInCluster(Cluster cluster, String protocol, String servicePort) {
        List<Service> services = filterByCluster(getClientForCluster(cluster).services(), cluster);
        for (Service service : services) {
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
     * Deleting the Kubernetes Secret
     */
    public void deleteSecretInNamespace(Application application, Claim claim) {
        KubernetesClient client = getClientForCluster(application.cluster);
        String secretName = application.name + "-" + claim.name;
        client.secrets().inNamespace(application.namespace).delete(new SecretBuilder().withNewMetadata()
                .withName(secretName).withNamespace(application.namespace).endMetadata().build());
    }

    /**
     * Add the secret into the specified cluster and namespace.
     */
    public void unMountSecretVolumeEnvInApplication(Application application, Claim claim) {
        KubernetesClient client = getClientForCluster(application.cluster);
        String secretName = application.name + "-" + claim.name;

        // Get the Deployment resource
        Deployment deployment = client.apps().deployments().inNamespace(application.namespace)
                .withName(application.name).get();

        // Remove the Volume pointing to the Secret
        Deployment newDeployment = new DeploymentBuilder(deployment).accept(ContainerBuilder.class, container -> {
            container.removeMatchingFromEnv(e -> Objects.equals(SERVICE_BINDING_ROOT, e.getName()));
            container.removeMatchingFromVolumeMounts(v -> Objects.equals(secretName, v.getName()));
        }).accept(PodSpecBuilder.class, podSpec -> {
            podSpec.removeMatchingFromVolumes(v -> Objects.equals(secretName, v.getName()));
        }).build();

        logIfDebugEnabled(newDeployment);

        // Update deployment
        client.apps().deployments().createOrReplace(newDeployment);
    }

    /**
     * Add the secret into the specified cluster and namespace.
     */
    public void mountSecretInApplication(Application application, Claim claim, Map<String, String> secretData) {
        KubernetesClient client = getClientForCluster(application.cluster);

        // create secret
        String secretName = (application.name + "-" + claim.name).toLowerCase(Locale.ROOT);
        client.secrets().createOrReplace(new SecretBuilder().withNewMetadata().withName(secretName)
                .withNamespace(application.namespace).endMetadata().withData(secretData).build());

        /**
         * Get the Deployment resource to be updated
         */
        Deployment deployment = client.apps().deployments().inNamespace(application.namespace)
                .withName(application.name).get();

        /*
         * Add a volumeMount to the container able to mount the path to access the secret under
         * "/SERVICE_BINDING_ROOT/secretName"
         *
         * Pass as ENV the property "SERVICE_BINDING_ROOT" pointing to the mount dir (e.g /bindings)
         *
         * Mount the secret
         */
        Deployment newDeployment = new DeploymentBuilder(deployment).accept(ContainerBuilder.class, container -> {
            container.addNewVolumeMount().withName(secretName)
                    .withMountPath(SERVICE_BINDING_ROOT_DEFAULT_VALUE + "/" + secretName).endVolumeMount();
            container.removeMatchingFromEnv(e -> Objects.equals(SERVICE_BINDING_ROOT, e.getName()));
            container.addNewEnv().withName(SERVICE_BINDING_ROOT).withValue(SERVICE_BINDING_ROOT_DEFAULT_VALUE).endEnv();
        }).accept(PodSpecBuilder.class, podSpec -> {
            podSpec.removeMatchingFromVolumes(v -> Objects.equals(secretName, v.getName()));
            podSpec.addNewVolume().withName(secretName).withNewSecret().withSecretName(secretName).endSecret()
                    .endVolume();
        }).build();

        logIfDebugEnabled(newDeployment);

        // update deployment
        client.apps().deployments().createOrReplace(newDeployment);
    }

    /**
     * Perform a rollout for the specified application.
     */
    public void rolloutApplication(Application application) {
        getClientForCluster(application.cluster).apps().deployments().inNamespace(application.namespace)
                .withName(application.name).rolling().restart();
    }

    private void logIfDebugEnabled(Deployment newDeployment) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Deployment changes to be applied: " + Serialization.asYaml(newDeployment));
        }
    }

    private <E extends HasMetadata, L extends KubernetesResourceList<E>> List<E> filterByCluster(
            MixedOperation<E, L, ?> operation, Cluster cluster) {
        FilterWatchListDeletable<E, L> filter = operation.inAnyNamespace();
        String[] excludedNamespaces = cluster.excludedNamespaces.split(",");
        for (var excludedNamespace : excludedNamespaces) {
            filter = filter.withoutField("metadata.namespace", excludedNamespace);
        }

        return filter.list().getItems();
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
