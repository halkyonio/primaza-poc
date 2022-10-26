package io.halkyon.resource.page;

import io.halkyon.Templates;
import io.halkyon.service.ClaimStatus;
import io.halkyon.service.ClaimValidator;
import io.quarkus.qute.TemplateInstance;
import org.jboss.resteasy.annotations.Form;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.sql.Date;
import java.util.List;
import java.util.Map;

@Path("/claims")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ClaimsResource {

    @GET
    public TemplateInstance list() {
        return showList(io.halkyon.model.Claim.listAll()).data("all", true);
    }

    private TemplateInstance showList(List<io.halkyon.model.Claim> claims) {
        return Templates.claimList(claims).data("items", io.halkyon.model.Claim.count());
    }

    @GET
    @Path("/{name}")
    public io.halkyon.model.Claim findByName(@PathParam("name") String name) {
        return io.halkyon.model.Claim.findByName(name);
    }
}
