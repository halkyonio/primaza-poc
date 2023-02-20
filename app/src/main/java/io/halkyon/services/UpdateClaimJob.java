package io.halkyon.services;

import static java.util.Optional.ofNullable;

import java.util.Arrays;
import java.util.Collections;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import io.halkyon.exceptions.ClusterConnectException;
import io.halkyon.model.Claim;
import io.halkyon.model.Service;
import io.halkyon.utils.StringUtils;
import io.quarkus.scheduler.Scheduled;

/**
 * The claiming service will poll every new and pending claim to update the claim status according to the following
 * logic:
 *
 * A claim can request a service registered in Primaza in the form of `<service name>-<service version>`, for example:
 * "mysql-3.6". If there is a service that matches the criteria of service name, plus service version, then the claim is
 * linked to this service. Next, if this service has been found in a cluster (the service available flag is true), and
 * there is at least one service credential, then the claim status will change to "Bindable"
 *
 * Otherwise, the status will be "pending".
 *
 * After a number of attempts have been made to find a suitable service, the claim status will change to "error".
 */
@ApplicationScoped
public class UpdateClaimJob {

    public static final String ERROR_MESSAGE_NO_SERVICE_REGISTERED = "Service '%s' not registered";
    public static final String ERROR_MESSAGE_NO_CREDENTIALS_REGISTERED = "Service '%s' has no credentials";
    public static final String ERROR_MESSAGE_NO_SERVICE_FOUND_IN_CLUSTER = "Could not find a service with protocol '%s'";

    private static final Logger LOG = Logger.getLogger(UpdateClaimJob.class);

    @ConfigProperty(name = "primaza.update-claim-job.max-attempts")
    int maxAttempts;

    @Inject
    BindApplicationService bindApplicationService;

    /**
     * This method will be executed at every `${primaza.update-claim-job.poll-every}`. It will loop over the new and
     * pending claims and try to link the service if the criteria matches.
     */
    @Transactional
    @Scheduled(every = "${primaza.update-claim-job.poll-every}", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    public void execute() {
        Claim.find("status in :statuses",
                Collections.singletonMap("statuses",
                        Arrays.asList(ClaimStatus.NEW.toString(), ClaimStatus.PENDING.toString())))
                .list().forEach(e -> updateClaim((Claim) e));
    }

    @Transactional(Transactional.TxType.REQUIRED)
    public void updateClaim(Claim claim) {
        if (claim.service == null) {
            Service service = findService(claim.serviceRequested);
            if (service != null) {
                claim.type = service.type;
                claim.service = service;
            } else {
                // claim.status = ClaimStatus.PENDING.toString();
                incrementAttempts(claim, String.format(ERROR_MESSAGE_NO_SERVICE_REGISTERED, claim.serviceRequested));
                claim.persist();
                return;
            }
        }

        if (claim.service != null) {
            boolean serviceAvailable = claim.service.available;
            boolean hasCredentials = claim.service.credentials.size() > 0;
            if (claim.type == null) {
                claim.type = claim.service.type;
            }
            if (claim.service.available && hasCredentials) {
                claim.status = ClaimStatus.BINDABLE.toString();
            } else if (serviceAvailable) {
                incrementAttempts(claim, String.format(ERROR_MESSAGE_NO_CREDENTIALS_REGISTERED, claim.service.name));
            } else {
                incrementAttempts(claim,
                        String.format(ERROR_MESSAGE_NO_SERVICE_FOUND_IN_CLUSTER, claim.service.endpoint));
            }
        }

        if (ClaimStatus.BINDABLE.toString().equals(claim.status) && claim.application != null) {
            try {
                bindApplicationService.bindApplication(claim);
            } catch (ClusterConnectException e) {
                LOG.error("Could bind application because there was connection errors. Cause: " + e.getMessage());
            }
        }

        claim.persist();
    }

    private void incrementAttempts(Claim claim, String errorMessage) {
        int attempts = ofNullable(claim.attempts).orElse(0);
        if (attempts >= maxAttempts) {
            claim.status = ClaimStatus.ERROR.toString();
            claim.errorMessage = errorMessage + " after " + maxAttempts + " attempts";
        } else {
            claim.status = ClaimStatus.PENDING.toString();
            claim.attempts = attempts + 1;
        }
    }

    private Service findService(String serviceRequested) {
        return Service.listAll().stream()
                .filter(s -> StringUtils.equalsIgnoreCase(s.name + "-" + s.version, serviceRequested)).findFirst()
                .orElse(null);
    }
}
