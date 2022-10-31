package io.halkyon.resource.page;

import io.halkyon.Templates;
import io.halkyon.service.ClaimStatus;
import io.halkyon.service.ClaimValidator;
import io.halkyon.utils.AcceptedResponseBuilder;
import io.quarkus.qute.TemplateInstance;
import org.jboss.resteasy.annotations.Form;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.sql.Date;
import java.util.List;

@Path("/claims")
public class ClaimResource {
    ClaimValidator claimValidator;

    @Inject
    public ClaimResource(ClaimValidator claimValidator){
        this.claimValidator = claimValidator;
    }

    @GET
    @Path("/new")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance claim() {
        return Templates.claimForm().data("title","Claim form");
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    @Consumes(MediaType.APPLICATION_JSON)
    public TemplateInstance list() {
        return showList(io.halkyon.model.Claim.listAll()).data("all", true);
    }

    private TemplateInstance showList(List<io.halkyon.model.Claim> claims) {
        return Templates.claimList(claims).data("items", io.halkyon.model.Claim.count());
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

            claim.persist();
            response.withSuccessMessage(claim.id);
        }

        // Return as HTML the template rendering the item for HTMX
        return response.build();
    }

}
