package io.halkyon.resource.page;

import java.sql.Date;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.halkyon.model.Application;
import io.halkyon.model.Cluster;
import io.halkyon.services.BindApplicationService;
import org.jboss.resteasy.annotations.Form;

import io.halkyon.Templates;
import io.halkyon.model.Claim;
import io.halkyon.model.Service;
import io.halkyon.services.ClaimStatus;
import io.halkyon.services.ClaimingServiceJob;
import io.halkyon.utils.AcceptedResponseBuilder;
import io.quarkus.qute.TemplateInstance;

@Path("/claims")
public class ClaimResource {
    private final Validator validator;
    private final ClaimingServiceJob claimingService;

    @Inject
    BindApplicationService bindService;

    @Inject
    public ClaimResource(Validator validator, ClaimingServiceJob claimingService){
        this.validator = validator;
        this.claimingService = claimingService;
    }

    @GET
    @Path("/new")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance claim() {
        return Templates.Claims.form(new Claim())
                .data("services",Service.findAvailableServices())
                .data("title","Claim form");
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
        return Response.ok(Templates.Claims.table(claims)
                .data("items", claims.size()))
                .build();
    }

    private TemplateInstance showList(List<Claim> claims) {
        return Templates.Claims.list(claims)
                .data("services", Service.listAll())
                .data("items", io.halkyon.model.Claim.count());
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
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
    public Response add(@Form io.halkyon.model.Claim claim, @HeaderParam("HX-Request") boolean hxRequest) {
        Set<ConstraintViolation<Claim>> errors = validator.validate(claim);
        AcceptedResponseBuilder response = AcceptedResponseBuilder.withLocation("/claim");

        if (errors.size() > 0) {
            response.withErrors(errors);
        } else {
            if (claim.status == null) {
                claim.status = ClaimStatus.NEW.toString();
            }

            claimingService.claimService(claim);
            response.withSuccessMessage(claim.id);
        }

        // Return as HTML the template rendering the item for HTMX
        return response.build();
    }

    @GET
    @Path("/claim/{id}")
    @Consumes(MediaType.TEXT_HTML)
    @Produces(MediaType.TEXT_HTML)
    public Object edit(@PathParam("id") Long id) {
        Claim claim = Claim.findById(id);
        if (claim == null) {
            throw new NotFoundException(String.format("Claim not found for id: %d%n", id));
        }
        return Templates.Claims.form(claim);
    }

    @DELETE
    @Path("/{id}")
    @Transactional
    public TemplateInstance delete(@PathParam("id") Long id) {
        Claim claim = Claim.findById(id);
        if(claim.applicationId !=null){
            bindService.unBindApplication(Application.findById(claim.applicationId), claim);
        }
        Claim.deleteById(id);
        return showList(Claim.listAll());
    }

}
