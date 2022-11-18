package io.halkyon.resource.page;

import io.halkyon.Templates;
import io.halkyon.services.RepositoryService;
import io.quarkus.qute.TemplateInstance;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

@Path("/")
public class HomeResource {

    @Inject
    RepositoryService repository;
    
    @GET
    public Response index() {
        return Response.status(Response.Status.TEMPORARY_REDIRECT).header("Location", "/home").build();
    }

    @GET
    @Path("/home")
    public TemplateInstance home() {
        return Templates.Index.home()
                .data("title","Home page")
                .data("repository",repository);
    }
}
