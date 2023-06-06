package io.halkyon.services;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import org.jboss.logging.Logger;

import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.halkyon.model.Application;
import io.halkyon.model.Cluster;
import io.quarkus.scheduler.Scheduled;

/**
 * The application discovery job will loop over the registered clusters and register all the installed Kubernetes
 * deployments in a cluster.
 */
@ApplicationScoped
public class ApplicationDiscoveryJob {

    private static Logger LOG = Logger.getLogger(ApplicationDiscoveryJob.class);

    @Inject
    KubernetesClientService kubernetesClientService;

    @Transactional
    @Scheduled(every = "${primaza.discovery-application-job.poll-every:5s}", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    public void execute() {
        List<Cluster> clusters = Cluster.listAll();
        for (Cluster cluster : clusters) {
            discoverApplications(cluster);
        }
    }

    @Transactional(Transactional.TxType.REQUIRED)
    public void discoverApplications(Cluster cluster) {
        try {
            Set<String> deploymentsSeen = new HashSet<>();
            // save or update applications in cluster
            List<Deployment> deployments = kubernetesClientService.getDeploymentsInCluster(cluster);
            for (Deployment deployment : deployments) {
                String name = deployment.getMetadata().getName();
                String namespace = deployment.getMetadata().getNamespace();
                deploymentsSeen.add(name + ":" + namespace);
                String image = getImageInDeployment(deployment);

                Application application = cluster.getApplicationByNameAndNamespace(name, namespace);
                if (application == null) {
                    application = new Application();
                    application.name = name;
                    application.namespace = namespace;
                    application.cluster = cluster;
                    application.ingress = kubernetesClientService.getIngressHost(application);
                }

                if (!Objects.equals(application.image, image)) {
                    application.image = image;
                    application.persist();
                }
            }

            // delete not existing applications
            for (Application application : cluster.applications) {
                if (!deploymentsSeen.contains(application.name + ":" + application.namespace)) {
                    application.delete();
                }
            }
        } catch (Exception ex) {
            LOG.error("Error discovering applications in cluster '" + cluster.name + "'", ex);
        }
    }

    private String getImageInDeployment(Deployment deployment) {
        return deployment.getSpec().getTemplate().getSpec().getContainers().stream().map(c -> c.getImage())
                .collect(Collectors.joining(", "));
    }
}
