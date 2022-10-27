package io.halkyon.resource.page;

import io.halkyon.Templates;
import io.quarkus.qute.TemplateInstance;
import org.jboss.resteasy.annotations.Form;

import javax.transaction.Transactional;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import java.sql.Date;


@Path("/service")
public class ServiceResource {
    @GET
    public TemplateInstance service() {
        return Templates.claimForm().data("tile","Claim form");
    }

    @POST
    @Transactional
    @Consumes("application/x-www-form-urlencoded")
    public Response add(@Form io.halkyon.model.Service service, @HeaderParam("HX-Request") boolean hxRequest) {

        if (service.created == null) {
            service.created = new Date(System.currentTimeMillis());
        }

        service.persist();
        // Return as HTML the template rendering the item for HTMX
        return Response.accepted(Templates.serviceItem(service)).status(Response.Status.CREATED).header("Location", "/services").build();
    }
}
