package io.halkyon.services;

import static io.halkyon.utils.StringUtils.equalsIgnoreCase;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;

import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentList;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.RollableScalableResource;
import io.fabric8.kubernetes.client.dsl.ServiceResource;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.halkyon.model.Application;
import io.halkyon.model.Claim;
import io.halkyon.model.Cluster;
import org.jboss.logging.Logger;

@ApplicationScoped
public class KubernetesClientService {

    private static String SERVICE_BINDING_PATH = "/bindings";
    private static Logger LOG = Logger.getLogger(KubernetesClientService.class);
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
     * Deleting the Kubernetes Secret
     */
    public void deleteSecretInNamespace(Application application, Claim claim) {
        KubernetesClient client = getClientForCluster(application.cluster);
        String secretName = application.name + "-" + claim.name;
        client.secrets()
              .inNamespace(application.namespace)
              .delete(new SecretBuilder()
                      .withNewMetadata()
                      .withName(secretName)
                      .withNamespace(application.namespace)
                      .endMetadata().build());
    }

    /**
     * Add the secret into the specified cluster and namespace.
     */
    public void unMountSecretVolumeEnvInApplication(Application application, Claim claim) {
        KubernetesClient client = getClientForCluster(application.cluster);
        String secretName = application.name + "-" + claim.name;

        // Get the Deployment resource
        Deployment deployment = client.apps().deployments()
                .inNamespace(application.namespace)
                .withName(application.name)
                .get();

        // Remove the Volume pointing to the Secret
        Deployment newDeployment = new DeploymentBuilder(deployment)
                .accept(ContainerBuilder.class, container -> {
                    container.removeMatchingFromEnv(e -> Objects.equals("SERVICE_BINDING_ROOT", e.getName()));
                })
                .accept(PodSpecBuilder.class, podSpec -> {
                    podSpec.removeMatchingFromVolumes(v -> secretName.equals(v.getName()));
                    podSpec.removeMatchingFromVolumes(v -> Objects.equals(secretName, v.getName()));
                })
                .build();

        // Update deployment
        client.apps().deployments().createOrReplace(deployment);
    }

    /**
     * Add the secret into the specified cluster and namespace.
     */
    public void mountSecretInApplication(Application application, Claim claim, Map<String, String> secretData) {
        KubernetesClient client = getClientForCluster(application.cluster);

        /**
         * Create the secret containing the key/value defined according to the workload projection spec
         */
        String secretName = application.name + "-" + claim.name;
        client.secrets().create(new SecretBuilder()
                .withNewMetadata()
                .withName(secretName)
                .withNamespace(application.namespace)
                .endMetadata()
                .withData(secretData)
                .build());

        /**
         * Get the Deployment resource to be updated
         */
        Deployment deployment = client.apps().deployments()
                .inNamespace(application.namespace)
                .withName(application.name)
                .get();

        /**
         * Add a volumeMount to the container able to mount the path to
         * access the secret under "/SERVICE_BINDING_PATH/secretName"
         *
         * Pass as ENV the property "SERVICE_BINDING_PATH"
         * pointing to the mount dir (e.g /bindings)
         *
         * Mount the secret
         */
        Deployment newDeployment = new DeploymentBuilder(deployment)
                .accept(ContainerBuilder.class, container -> {
                    container.addNewVolumeMount().withName(secretName).withMountPath(SERVICE_BINDING_PATH + "/" + secretName).endVolumeMount();
                    container.removeMatchingFromEnv(e -> Objects.equals("SERVICE_BINDING_ROOT", e.getName()));
                    container.addNewEnv().withName("SERVICE_BINDING_ROOT").withValue(SERVICE_BINDING_PATH).endEnv();
                  })
                .accept(PodSpecBuilder.class, podSpec ->  {
                    podSpec.removeMatchingFromVolumes(v -> Objects.equals(secretName, v.getName()));
                    podSpec.addNewVolume().withName(secretName).withNewSecret().withSecretName(secretName).endSecret().endVolume();
                  })
                .build();

        LOG.info(Serialization.asYaml(newDeployment));

        // update deployment
        client.apps().deployments().createOrReplace(newDeployment);
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
