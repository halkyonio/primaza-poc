package io.halkyon.resource.page;

import java.sql.Date;
import java.util.List;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.jboss.resteasy.annotations.Form;

import io.halkyon.Templates;
import io.halkyon.model.Claim;
import io.halkyon.services.ClaimStatus;
import io.halkyon.services.ClaimValidator;
import io.halkyon.services.ClaimingJobService;
import io.halkyon.utils.AcceptedResponseBuilder;
import io.quarkus.qute.TemplateInstance;

@Path("/claims")
public class ClaimResource {
    private final ClaimValidator claimValidator;
    private final ClaimingJobService claimingService;

    @Inject
    public ClaimResource(ClaimValidator claimValidator, ClaimingJobService claimingService){
        this.claimValidator = claimValidator;
        this.claimingService = claimingService;
    }

    @GET
    @Path("/new")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance claim() {
        return Templates.Claims.form().data("title","Claim form");
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    @Consumes(MediaType.APPLICATION_JSON)
    public TemplateInstance list() {
        return showList(Claim.listAll()).data("all", true);
    }

    private TemplateInstance showList(List<Claim> claims) {
        return Templates.Claims.list(claims).data("items", io.halkyon.model.Claim.count());
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/name/{name}")
    public io.halkyon.model.Claim findByName(@PathParam("name") String name) {
        return io.halkyon.model.Claim.findByName(name);
    }

    @POST
    @Transactional
    @Consumes("application/x-www-form-urlencoded")
    @Produces(MediaType.TEXT_HTML)
    public Response add(@Form io.halkyon.model.Claim claim, @HeaderParam("HX-Request") boolean hxRequest) {
        List<String> errors = claimValidator.validateForm(claim);
        AcceptedResponseBuilder response = AcceptedResponseBuilder.withLocation("/claim");

        if (errors.size() > 0) {
            response.withErrors(errors);
        } else {
            if (claim.created == null) {
                claim.created = new Date(System.currentTimeMillis());
            }
            if (claim.status == null) {
                claim.status = ClaimStatus.NEW.toString();
            }

            claimingService.claimService(claim);
            response.withSuccessMessage(claim.id);
        }

        // Return as HTML the template rendering the item for HTMX
        return response.build();
    }

}
