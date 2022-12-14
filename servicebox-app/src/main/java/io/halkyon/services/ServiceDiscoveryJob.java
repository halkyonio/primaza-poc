package io.halkyon.services;

import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.PersistenceException;
import javax.transaction.Transactional;
import org.hibernate.exception.ConstraintViolationException;

import javax.ws.rs.ClientErrorException;

import org.jboss.logging.Logger;

import io.halkyon.model.Cluster;
import io.halkyon.model.Service;
import io.quarkus.scheduler.Scheduled;

/**
 * The service discovery job will loop over the registered services and clusters and check whether a service is available in
 * a cluster. If so, it will update the service entity available and cluster fields accordingly.
 */
@ApplicationScoped
public class ServiceDiscoveryJob {

    private static Logger LOG = Logger.getLogger(ServiceDiscoveryJob.class);

    @Inject
    KubernetesClientService kubernetesClientService;

    /**
     * This method will be executed at every `${servicebox.discovery-service-job.poll-every}`.
     * First, it will collect the list of all services and clusters, and then will loop over the services to check whether
     * the service name (from the first part of the service.endpoint field) is installed in one cluster. If so, then it will
     * check whether service port (from the second part of the service.endpoint field) is declared in the found kubernetes
     * service resource.
     */
    @Transactional
    @Scheduled(every="${servicebox.discovery-service-job.poll-every}", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    public void execute() {
        List<Service> services = Service.listAll();
        for (Service service : services) {
            if (linkServiceInCluster(service)) {
                service.persist();
            }
        }
    }

    @Transactional(Transactional.TxType.REQUIRED)
    public void checkCluster(Cluster cluster) {
        List<Service> services = Service.listAll();
        boolean updated = false;
        for (Service service : services) {
            if (service.cluster == null && updateServiceIfFoundInCluster(service, cluster)) {
                updated = true;
            }
        }

        if (updated) {
            cluster.persist();
        }
    }

    @Transactional(Transactional.TxType.REQUIRED)
    public boolean linkServiceInCluster(Service service) {
        boolean updated = false;
        if (service.cluster == null || !getServiceInCluster(service, service.cluster).isPresent()) {
            service.available = false;
            List<Cluster> clusters = Cluster.listAll();
            for (Cluster cluster : clusters) {
                if (updateServiceIfFoundInCluster(service, cluster)) {
                    updated = true;
                    break;
                }
            }
        }

        return updated;
    }

    private boolean updateServiceIfFoundInCluster(Service service, Cluster cluster) {
        Optional<io.fabric8.kubernetes.api.model.Service> serviceInCluster = getServiceInCluster(service, cluster);
        if (serviceInCluster.isPresent()) {
            service.available = true;
            service.cluster = cluster;
            service.namespace = serviceInCluster.get().getMetadata().getNamespace();
            cluster.services.add(service);

            return true;
        }

        return false;
    }

    private Optional<io.fabric8.kubernetes.api.model.Service> getServiceInCluster(Service service, Cluster cluster) {
        try {
            return kubernetesClientService.getServiceInCluster(cluster, service.getProtocol(), service.getPort());
        } catch (Exception ex) {
            LOG.error("Error trying to discovery the service " + service.name + " in the registered clusters", ex);
        }

        return Optional.empty();
    }
}
