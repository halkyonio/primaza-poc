package io.halkyon;


import io.halkyon.model.Claim;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import org.jboss.resteasy.annotations.Form;

import javax.transaction.Transactional;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

@Path("/claims")
//@Produces(MediaType.APPLICATION_JSON)
//@Consumes(MediaType.APPLICATION_JSON)
public class ClaimCustomResource {
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
    public Claim get(String name) {
        return Claim.findByName(name);
    }

    @POST
    @Transactional
    public Response add(@Form Claim claim) {
        claim.persist();
        // Return as HTML the template rendering the item
        return Response.ok(Templates.item(claim)).header("Location", "/claims").build();
    }
}
