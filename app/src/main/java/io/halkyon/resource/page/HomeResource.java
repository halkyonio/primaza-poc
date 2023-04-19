package io.halkyon.resource.page;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

import io.halkyon.Templates;
import io.quarkus.qute.TemplateInstance;

@Path("/")
public class HomeResource {

    @GET
    public Response index() {
        return Response.status(Response.Status.TEMPORARY_REDIRECT).header("Location", "/home").build();
    }

    @GET
    @Path("/home")
    public TemplateInstance home() {
        return Templates.Index.home();
    }
}
