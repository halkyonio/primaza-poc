package io.halkyon.services;

import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import org.jboss.logging.Logger;

import io.halkyon.model.Cluster;
import io.halkyon.model.Service;
import io.halkyon.utils.StringUtils;
import io.quarkus.scheduler.Scheduled;

/**
 * The service discovery job will loop over the registered services and clusters and check whether a service is
 * available in a cluster. If so, it will update the service entity available and cluster fields accordingly.
 */
@ApplicationScoped
public class ServiceDiscoveryJob {

    private static Logger LOG = Logger.getLogger(ServiceDiscoveryJob.class);

    @Inject
    KubernetesClientService kubernetesClientService;

    /**
     * This method will be executed at every `${primaza.discovery-service-job.poll-every}`. First, it will collect the
     * list of all services and clusters, and then will loop over the services to check whether the service name (from
     * the first part of the service.endpoint field) is installed in one cluster. If so, then it will check whether
     * service port (from the second part of the service.endpoint field) is declared in the found kubernetes service
     * resource.
     */
    @Transactional
    @Scheduled(every = "${primaza.discovery-service-job.poll-every}", concurrentExecution = Scheduled.ConcurrentExecution.SKIP, skipExecutionIf = Scheduled.ApplicationNotRunning.class)
    public void execute() {
        List<Service> services = Service.listAll();
        for (Service service : services) {
            if (linkServiceInCluster(service)) {
                service.persist();
            }
        }
    }

    @Transactional(Transactional.TxType.REQUIRED)
    public void discoverServices(Cluster cluster) {
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
        if (service.isStandalone()) {
            return false;
        }

        boolean updated = false;
        if (service.cluster == null || !getServiceInCluster(service, service.cluster).isPresent()) {
            service.available = false;
            List<Cluster> clusters = Cluster.listAll();
            for (Cluster cluster : clusters) {
                LOG.debugf("Checking after the service: %s, %s, %s", service.name, service.getProtocol(),
                        service.getPort());
                if (updateServiceIfFoundInCluster(service, cluster)) {
                    LOG.debugf("Service: %s, %s found within namespace: %s of the cluster: %s", service.name,
                            service.getPort(), service.namespace, service.cluster.name);
                    updated = true;
                    break;
                }
            }
        }

        return updated;
    }

    private boolean updateServiceIfFoundInCluster(Service service, Cluster cluster) {
        return getServiceInCluster(service, cluster).map(s -> {
            service.available = true;
            service.cluster = cluster;
            service.namespace = s.getMetadata().getNamespace();
            findExternalIpFromService(s).ifPresent(ip -> service.externalEndpoint = ip);
            cluster.services.add(service);
            return true;
        }).orElse(false);
    }

    private Optional<String> findExternalIpFromService(io.fabric8.kubernetes.api.model.Service s) {
        if (s.getStatus() == null || s.getStatus().getLoadBalancer() == null
                || s.getStatus().getLoadBalancer().getIngress() == null) {
            return Optional.empty();
        }

        // IP detection rules:
        // 1.- Try Ingress IP
        // 2.- Try Ingress Hostname
        return s.getStatus().getLoadBalancer().getIngress().stream()
                .map(ingress -> StringUtils.defaultIfBlank(ingress.getIp(), ingress.getHostname()))
                .filter(StringUtils::isNotEmpty).findFirst();
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
