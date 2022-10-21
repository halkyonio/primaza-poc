package io.halkyon;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

@Path("/")
public class AppResource {

    @GET
    public Response home() {
        return Response.status(Response.Status.TEMPORARY_REDIRECT).header("Location", "/claims").build();
    }
}