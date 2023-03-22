package io.halkyon.resource;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import io.quarkus.vault.VaultKVSecretEngine;

@Path("/hello")
public class GreetingResource {

    @Inject
    VaultKVSecretEngine kvSecretEngine;

    @GET
    @Path("/secrets/{vault-path}")
    @Produces(MediaType.TEXT_PLAIN)
    public String getSecrets(@PathParam("vault-path") String vaultPath) {
        return kvSecretEngine.readSecret("primaza/" + vaultPath).toString();
    }

}
