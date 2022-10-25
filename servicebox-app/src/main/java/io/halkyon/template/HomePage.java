package io.halkyon.template;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/")
public class HomePage {
    @GET
    public Response home() {
        return Response.status(Response.Status.TEMPORARY_REDIRECT).header("Location", "/claims").build();
    }

    @GET
    @Path("/hello")
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        return "Hello from Primaza Service Box App";
    }
}