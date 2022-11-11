package io.halkyon.services;

import io.halkyon.model.Claim;
import io.halkyon.model.ProtocolAndPort;
import io.halkyon.model.Service;
import io.quarkus.scheduler.Scheduled;
import io.smallrye.mutiny.Uni;
import io.smallrye.stork.Stork;
import io.smallrye.stork.api.Metadata;
import io.smallrye.stork.api.MetadataKey;
import io.smallrye.stork.api.ServiceDefinition;
import io.smallrye.stork.api.ServiceInstance;
import io.smallrye.stork.servicediscovery.kubernetes.KubernetesConfiguration;
import io.smallrye.stork.servicediscovery.kubernetes.KubernetesMetadataKey;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.enterprise.context.ApplicationScoped;
import javax.transaction.Transactional;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static com.fasterxml.jackson.databind.type.LogicalType.Collection;
import static io.smallrye.mutiny.operators.uni.UniBlockingAwait.await;
import static io.smallrye.stork.servicediscovery.kubernetes.KubernetesMetadataKey.META_K8S_SERVICE_ID;
import static java.util.Optional.ofNullable;

/**
 * The claiming service will poll new or pending claims and try to find an available service.
 * A claim can request a service in the form of `<service name>:<service version>`, for example: "mysql:3.6".
 * If there is an available service that matches the criteria of service name, plus service version, this service will
 * be linked to the claim and the claim status will change to "claimed". Otherwise, the status will be "pending".
 * After a number of attempts have been made to find a suitable service, the claim status will change to "error".
 */
@ApplicationScoped
public class ServiceDiscoveryJob {


    /**
     * This method will be executed at every `${servicebox.claiming-service.poll-every}`.
     * First, it will collect the list of all available services, and then will loop over the new or pending claims to link
     * the service if the criteria matches.
     */
    @Transactional
    @Scheduled(every="${servicebox.claiming-service.poll-every}", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    public void execute() {
        List<Service> services = Service.listAll();
        Stork stork = Stork.getInstance();
        for (Service service : services) {
            KubernetesConfiguration kubernetesConfiguration = new KubernetesConfiguration().withApplication(service.name);
            stork.defineIfAbsent(service.name, ServiceDefinition.of(kubernetesConfiguration));
            io.smallrye.stork.api.Service storkService = stork.getService(service.name);

            List<ServiceInstance> instances = storkService.getServiceDiscovery().getServiceInstances().await().indefinitely();
            ProtocolAndPort protocolAndPort = parseToProtocolAndPort(service.endpoint);
            ServiceInstance matching = findMatching(instances, protocolAndPort);
            if (matching == null) {
                Service.deleteById(service.id);
            }
        }
    }

    /**
     * Extracts the protocol and port values from an endpoint string in form of protocol:port.
     *
     * @param endpoint endpoint as protocol:port
     * @return {@link ProtocolAndPort}
     */
    public static ProtocolAndPort parseToProtocolAndPort(String endpoint) {
        String[] endpointParts = endpoint.split("\\:");
        ProtocolAndPort protocolAndPort = new ProtocolAndPort(endpointParts[0].toUpperCase(), Integer.valueOf(endpointParts[1]));
        return protocolAndPort;

    }

        /**
         * Finds a matching instance for a given port and protocol
         *
         * @param serviceInstances the list of instances
         * @param protocolAndPort the structure representing the protocol and port for an endpoint
         * @return the found instance or {@code null} if none matches
         */
        public static ServiceInstance findMatching (List<ServiceInstance> serviceInstances, ProtocolAndPort protocolAndPort) {
            if (protocolAndPort.protocol == null) {
                throw new NullPointerException("Protocol cannot be null");
            }
            for (ServiceInstance instance : serviceInstances) {
                Metadata<KubernetesMetadataKey> k8sMetadata = (Metadata<KubernetesMetadataKey>) instance.getMetadata();
                String svcProtocol="";
                if(k8sMetadata.getMetadata().get(KubernetesMetadataKey.META_K8S_PORT_PROTOCOL)!=null){
                    svcProtocol= (String) k8sMetadata.getMetadata().get(KubernetesMetadataKey.META_K8S_PORT_PROTOCOL);
                }
                if (protocolAndPort.protocol.equals(svcProtocol) && protocolAndPort.port == instance.getPort()) {
                    return instance;
                }
            }
            return null;
        }



    }


