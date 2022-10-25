package io.halkyon.template;


import io.halkyon.model.Claim;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import org.jboss.resteasy.annotations.Form;

import javax.transaction.Transactional;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

@Path("/claims")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ClaimPage {
    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance list(List<Claim> claims);
        public static native TemplateInstance item(Claim claim);
    }

    @GET
    public TemplateInstance list() {
        return showList(Claim.listAll()).data("all",true);
    }

    private TemplateInstance showList(List<Claim> claims) {
        return Templates.list(claims).data("items", Claim.count());
    }

    @GET
    @Path("/{name}")
    public Claim findByName(@PathParam("name") String name) {
        return Claim.findByName(name);
    }

    @POST
    @Transactional
    @Consumes("application/x-www-form-urlencoded")
    public Response add(@Form Claim claim, @HeaderParam("HX-Request") boolean hxRequest) {
        claim.persist();
        // Return as HTML the template rendering the item for HTMX
        return Response.accepted(Templates.item(claim)).status(Response.Status.CREATED).header("Location", "/claims").build();
    }
}
