package io.halkyon.resource.page;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

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
        return Templates.Index.home().data("title", "Home page");
    }
}
