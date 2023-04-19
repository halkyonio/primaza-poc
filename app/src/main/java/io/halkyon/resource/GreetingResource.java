package io.halkyon.resource;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

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
