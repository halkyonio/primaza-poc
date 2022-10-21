package io.halkyon;


import io.halkyon.model.Claim;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/claims")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ClaimCustomResource {

    @GET
    @Path("/{name}")
    public Claim get(String name) {
        return Claim.findByName(name);
    }

}
