package io.halkyon.resource.page;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/")
public class AppResource {
    @GET
    public Response home() {
        return Response.status(Response.Status.TEMPORARY_REDIRECT).header("Location", "/home").build();
    }

    @GET
    @Path("/hello")
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        return "Hello from Primaza Service Box app";
    }
}