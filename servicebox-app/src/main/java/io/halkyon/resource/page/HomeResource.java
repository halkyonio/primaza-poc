package io.halkyon.resource.page;

import io.halkyon.Templates;
import io.halkyon.ApplicationProperties;
import io.quarkus.qute.TemplateInstance;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

@Path("/")
public class HomeResource {
    
    @GET
    public Response index() {
        return Response.status(Response.Status.TEMPORARY_REDIRECT).header("Location", "/home").build();
    }

    @GET
    @Path("/home")
    public TemplateInstance home() {
        return Templates.Index.home()
                .data("title","Home page");
    }
}
