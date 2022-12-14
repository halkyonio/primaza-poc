package io.halkyon.resource.page;

import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.jboss.resteasy.annotations.Form;

import io.halkyon.Templates;
import io.halkyon.model.Application;
import io.halkyon.model.Claim;
import io.halkyon.model.Service;
import io.halkyon.resource.requests.ClaimRequest;
import io.halkyon.services.BindApplicationService;
import io.halkyon.services.ClaimStatus;
import io.halkyon.services.UpdateClaimJob;
import io.halkyon.utils.AcceptedResponseBuilder;
import io.quarkus.qute.TemplateInstance;

@Path("/claims")
public class ClaimResource {
    private final Validator validator;
    private final UpdateClaimJob claimingService;
    private final BindApplicationService bindService;

    @Inject
    public ClaimResource(Validator validator, UpdateClaimJob claimingService, BindApplicationService bindService) {
        this.validator = validator;
        this.claimingService = claimingService;
        this.bindService = bindService;
    }

    @GET
    @Path("/new")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance claim() {
        return Templates.Claims.form(new Claim(), Service.listAll()).data("title", "Claim form");
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    @Consumes(MediaType.APPLICATION_JSON)
    public TemplateInstance list() {
        return showList(Claim.listAll()).data("all", true);
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    @Path("/filter")
    public Response filter(@QueryParam("name") String name, @QueryParam("servicerequested") String serviceRequested) {
        List<Claim> claims = Claim.getClaims(name, serviceRequested);
        return Response.ok(Templates.Claims.table(claims).data("items", claims.size())).build();
    }

    private TemplateInstance showList(List<Claim> claims) {
        return Templates.Claims.list(claims).data("services", Service.listAll()).data("items",
                io.halkyon.model.Claim.count());
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
            claim.name = claimRequest.name;
            claim.owner = claimRequest.owner;
            claim.description = claimRequest.description;
            claim.serviceRequested = claimRequest.serviceRequested;
            claim.status = ClaimStatus.NEW.toString();

            claimingService.updateClaim(claim);
            response.withSuccessMessage(claim.id);
        }

        // Return as HTML the template rendering the item for HTMX
        return response.build();
    }

    @GET
    @Path("/{id:[0-9]+}")
    @Consumes(MediaType.TEXT_HTML)
    @Produces(MediaType.TEXT_HTML)
    public Object edit(@PathParam("id") Long id) {
        Claim claim = Claim.findById(id);
        if (claim == null) {
            throw new NotFoundException(String.format("Claim not found for id: %d%n", id));
        }

        return Templates.Claims.form(claim, Service.listAll());
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
            claim.name = claimRequest.name;
            claim.owner = claimRequest.owner;
            claim.description = claimRequest.description;
            claim.serviceRequested = claimRequest.serviceRequested;
            claim.status = ClaimStatus.NEW.toString();

            claimingService.updateClaim(claim);
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
        if (claim.applicationId != null) {
            bindService.unBindApplication(Application.findById(claim.applicationId), claim);
        }
        Claim.deleteById(id);
        return showList(Claim.listAll());
    }

}
