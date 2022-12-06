package io.halkyon.services;

import static java.util.Optional.ofNullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.transaction.Transactional;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.halkyon.model.Claim;
import io.halkyon.model.Service;
import io.quarkus.scheduler.Scheduled;

/**
 * The claiming service will poll new or pending claims and try to find an available service.
 * A claim can request a service in the form of `<service name>:<service version>`, for example: "mysql:3.6".
 * If there is an available service that matches the criteria of service name, plus service version, this service will
 * be linked to the claim and the claim status will change to "claimed". Otherwise, the status will be "pending".
 * After a number of attempts have been made to find a suitable service, the claim status will change to "error".
 */
@ApplicationScoped
public class ClaimingServiceJob {

    @ConfigProperty(name = "servicebox.claiming-service-job.max-attempts")
    int maxAttempts;


    /**
     * This method will be executed at every `${servicebox.claiming-service-job.poll-every}`.
     * First, it will collect the list of all available services, and then will loop over the new or pending claims to link
     * the service if the criteria matches.
     */
    @Transactional
    @Scheduled(every="${servicebox.claiming-service-job.poll-every}", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    public void execute() {
        Map<String, Service> allServices = listAllAvailableServices();
        Claim.find("status in :statuses", Collections.singletonMap("statuses",
                        Arrays.asList(ClaimStatus.NEW.toString(), ClaimStatus.PENDING.toString())))
                .list()
                .forEach(e -> claimService((Claim) e, allServices));
    }

    @Transactional(Transactional.TxType.REQUIRED)
    public void claimService(Claim claim) {
        claimService(claim, listAllAvailableServices());
    }

    private Map<String, Service> listAllAvailableServices() {
        Map<String, Service> services = new HashMap<>();
        Service.list("available=true")
                .stream().map(Service.class::cast)
                .forEach(s -> {
                    // combination by service name + version
                    String serviceNameWithVersion = s.name + "-" + s.version;
                    services.put(serviceNameWithVersion, s);
                    services.put(serviceNameWithVersion.toLowerCase(Locale.ROOT), s);
                    services.put(serviceNameWithVersion.toUpperCase(Locale.ROOT), s);
                });

        return services;
    }

    private void claimService(Claim claim, Map<String, Service> services) {
        Service service = services.get(claim.serviceRequested);
        if (service == null) {
            int attempts = ofNullable(claim.attempts).orElse(0);
            if (attempts >= maxAttempts) {
                claim.status = ClaimStatus.ERROR.toString();
            } else {
                claim.attempts = attempts + 1;
                claim.status = ClaimStatus.PENDING.toString();
            }

        } else {
            claim.status = ClaimStatus.BIND.toString();
            claim.type = service.type;
            claim.database = service.database;
            claim.service = service;
        }

        claim.persist();
    }
}
