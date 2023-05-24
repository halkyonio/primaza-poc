package io.halkyon.resource.page;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.Form;

import io.halkyon.Templates;
import io.halkyon.exceptions.ClusterConnectException;
import io.halkyon.model.Application;
import io.halkyon.model.Claim;
import io.halkyon.model.Service;
import io.halkyon.resource.requests.ClaimRequest;
import io.halkyon.services.BindApplicationService;
import io.halkyon.services.ClaimStatus;
import io.halkyon.services.KubernetesClientService;
import io.halkyon.services.UpdateClaimJob;
import io.halkyon.utils.AcceptedResponseBuilder;
import io.halkyon.utils.FilterableQueryBuilder;
import io.halkyon.utils.StringUtils;
import io.quarkus.qute.TemplateInstance;

@Path("/claims")
public class ClaimResource {

    private static final Logger LOG = Logger.getLogger(ClaimResource.class);

    private final Validator validator;
    private final UpdateClaimJob claimingService;
    private final BindApplicationService bindService;

    @Inject
    KubernetesClientService kubernetesClientService;

    @Inject
    public ClaimResource(Validator validator, UpdateClaimJob claimingService, BindApplicationService bindService) {
        this.validator = validator;
        this.claimingService = claimingService;
        this.bindService = bindService;
    }

    @GET
    @Path("/new")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance claim(@QueryParam("applicationId") Long applicationId) {
        Map<String, Object> optional = new HashMap<>();
        if (applicationId != null) {
            optional.put("applicationId", applicationId);
        }

        return Templates.Claims.form("Claim form", new Claim(), Service.listAll(), optional);
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance list() {
        return Templates.Claims.list("Claims", Claim.listAll(), Service.listAll(), Claim.count(),
                Collections.emptyMap());
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    @Path("/filter")
    public Response filter(@QueryParam("name") String name, @QueryParam("serviceRequested") String serviceRequested,
            @QueryParam("owner") String owner, @QueryParam("status") String status) {
        FilterableQueryBuilder query = new FilterableQueryBuilder();
        if (!StringUtils.isNullOrEmpty(name)) {
            query.containsIgnoreCase("name", name);
        }

        if (!StringUtils.isNullOrEmpty(serviceRequested)) {
            query.containsIgnoreCase("serviceRequested", serviceRequested);
        }

        if (!StringUtils.isNullOrEmpty(owner)) {
            query.containsIgnoreCase("owner", owner);
        }

        if (!StringUtils.isNullOrEmpty(status)) {
            query.equals("status", status);
        }

        List<Claim> claims = Claim.list(query.build(), query.getParameters());
        return Response.ok(Templates.Claims.table(claims, claims.size(), query.getFilterAsMap())).build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/name/{name}")
    public io.halkyon.model.Claim findByName(@PathParam("name") String name) {
        Claim claim = Claim.findByName(name);
        if (claim == null) {
            throw new NotFoundException("Claim with name " + name + " does not exist.");
        }
        return claim;

    }

    @POST
    @Transactional
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_HTML)
    public Response add(@Form ClaimRequest claimRequest, @HeaderParam("HX-Request") boolean hxRequest) {
        Set<ConstraintViolation<ClaimRequest>> errors = validator.validate(claimRequest);
        AcceptedResponseBuilder response = AcceptedResponseBuilder.withLocation("/claims");

        if (errors.size() > 0) {
            response.withErrors(errors);
        } else {
            Claim claim = new Claim();

            doUpdateClaim(claim, claimRequest);

            response.withSuccessMessage(claim.id);
        }

        // Return as HTML the template rendering the item for HTMX
        return response.build();
    }

    @GET
    @Path("/{id:[0-9]+}")
    @Consumes(MediaType.TEXT_HTML)
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance edit(@PathParam("id") Long id) {
        Claim claim = Claim.findById(id);
        if (claim == null) {
            throw new NotFoundException(String.format("Claim not found for id: %d%n", id));
        }

        return Templates.Claims.form("Claim " + id + " form", claim, Service.listAll(), Collections.emptyMap());
    }

    @PUT
    @Path("/{id:[0-9]+}")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_HTML)
    @Transactional
    public Object edit(@PathParam("id") Long id, @Form ClaimRequest claimRequest) {
        Claim claim = Claim.findById(id);
        if (claim == null) {
            throw new NotFoundException(String.format("Claim not found for id: %d%n", id));
        }

        Set<ConstraintViolation<ClaimRequest>> errors = validator.validate(claimRequest);
        AcceptedResponseBuilder response = AcceptedResponseBuilder.withLocation("/claims/" + claim.id);

        if (errors.size() > 0) {
            response.withErrors(errors);
        } else {
            doUpdateClaim(claim, claimRequest);
            response.withUpdateSuccessMessage(claim.id);
        }

        // Return as HTML the template rendering the item for HTMX
        return response.build();
    }

    @DELETE
    @Path("/{id}")
    @Transactional
    public TemplateInstance delete(@PathParam("id") Long id) {
        Claim claim = Claim.findById(id);
        try {
            if (claim.application != null) {
                bindService.unBindApplication(claim);
            }
            Claim.deleteById(id);
            return list();
        } catch (ClusterConnectException ex) {
            throw new InternalServerErrorException("Cannot delete the claim because can't connect with the cluster "
                    + ex.getCluster().name + " where is deployed. Cause: " + ex.getMessage());
        }
    }

    private void doUpdateClaim(Claim claim, ClaimRequest request) {
        claim.name = request.name;
        claim.owner = request.owner;
        claim.description = request.description;
        if (StringUtils.isNotEmpty(request.serviceId)) {
            claim.service = Service.findById(Long.parseLong(request.serviceId));
        } else if (StringUtils.isNotEmpty(request.serviceRequested)) {
            claim.serviceRequested = request.serviceRequested;
        }

        claim.status = ClaimStatus.NEW.toString();

        if (request.applicationId != null) {
            claim.application = Application.findById(request.applicationId);
        }

        claimingService.updateClaim(claim);

        // TODO: Logic to be reviewed
        if (claim.service != null && claim.service.installable != null && claim.service.installable
                && claim.application != null) {
            claim.service.cluster = claim.application.cluster;
            claim.service.namespace = claim.application.namespace;
            claim.persist();
            try {
                System.out.println("Service is installable using crossplane. Let's do it :-)");
                kubernetesClientService.createCrossplaneHelmRelease(claim.application.cluster, claim.service);
                if (kubernetesClientService.getServiceInCluster(claim.application.cluster, claim.service.getProtocol(),
                        claim.service.getPort()).isPresent()) {
                    claim.service.cluster = claim.application.cluster;
                }
            } catch (ClusterConnectException ex) {
                throw new InternalServerErrorException(
                        "Can't deploy the service with the cluster " + ex.getCluster() + ". Cause: " + ex.getMessage());
            }
        }

        // TODO: We must find the new service created (= name & namespace + port), otherwise the url returned by
        // generateUrlByClaimService(claim) will be null
        if (claim.service != null) {
            LOG.infof("Service name: %s", claim.service.name == null ? "" : claim.service.name);
            LOG.infof("Service namespace: %s", claim.service.namespace == null ? "" : claim.service.namespace);
            LOG.infof("Service port: %s", claim.service.getPort() == null ? "" : claim.service.getPort());
            LOG.infof("Service protocol: %s", claim.service.getProtocol() == null ? "" : claim.service.getProtocol());
        }

        if (claim.service != null && claim.service.credentials != null && claim.application != null) {
            try {
                bindService.bindApplication(claim);
            } catch (ClusterConnectException e) {
                LOG.error("Could bind application because there was connection errors. Cause: " + e.getMessage());
            }
        }
    }

}
