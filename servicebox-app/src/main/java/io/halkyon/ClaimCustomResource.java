package io.halkyon;


import io.halkyon.model.Claim;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Path("/claims")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ClaimCustomResource {
    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance list(List<Claim> claims);
    }

    private TemplateInstance showList(List<Claim> claims) {
        return Templates.list(claims).data("itemsLeft", Claim.count());
    }

    @GET
    public TemplateInstance list() {
        return showList(Claim.listAll()).data("all",true);
    }
    @GET
    @Path("/{name}")
    public Claim get(String name) {
        return Claim.findByName(name);
    }
}
