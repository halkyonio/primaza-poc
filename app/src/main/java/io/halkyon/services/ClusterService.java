package io.halkyon.services;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;

import org.jboss.logging.Logger;

import io.halkyon.exceptions.ClusterConnectException;
import io.halkyon.model.Cluster;
import io.halkyon.resource.page.ClusterResource;

@ApplicationScoped
public class ClusterService {

    private static final Logger LOG = Logger.getLogger(ClusterResource.class);

    @Inject
    KubernetesClientService kubernetesClientService;
    @Inject
    ServiceDiscoveryJob serviceDiscoveryJob;
    @Inject
    ApplicationDiscoveryJob applicationDiscoveryJob;

    @Transactional
    public void doSave(Cluster cluster) {
        if (cluster.id != null) {
            Cluster edited = cluster;
            cluster = Cluster.findById(cluster.id);
            if (cluster == null) {
                throw new NotFoundException(String.format("Cluster not found for id: %d%n", cluster.id));
            }
            cluster = mergeEntities(cluster, edited);

        }
        try {
            kubernetesClientService.getClientForCluster(cluster);
            cluster.status = ClusterStatus.OK;
            cluster.persist();
            serviceDiscoveryJob.discoverServices(cluster);
            applicationDiscoveryJob.discoverApplications(cluster);
        } catch (ClusterConnectException e) {
            LOG.error("Could not connect with the cluster '" + e.getCluster().name + "'. Caused by: " + e.getMessage());
        }
    }

    private Cluster mergeEntities(Cluster old, Cluster edited) {
        old.name = !old.name.equals(edited.name) ? edited.name : old.name;
        old.url = !old.url.equals(edited.url) ? edited.url : old.url;
        old.environment = !old.environment.equals(edited.environment) ? edited.environment : old.environment;
        old.kubeConfig = old.kubeConfig != null && !old.kubeConfig.equals(edited.kubeConfig) ? edited.kubeConfig
                : old.kubeConfig;
        old.token = old.token != null && !old.token.equals(edited.token) ? edited.token : old.token;
        old.namespace = old.namespace != null && !old.namespace.equals(edited.namespace) ? edited.namespace
                : old.namespace;
        old.excludedNamespaces = old.excludedNamespaces != null
                && !old.excludedNamespaces.equals(edited.excludedNamespaces) ? edited.excludedNamespaces
                        : old.excludedNamespaces;
        return old;
    }
}
