package io.halkyon.resource.page;

import io.halkyon.Templates;
import io.halkyon.services.RepositoryService;
import io.quarkus.qute.TemplateInstance;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("/home")
public class HomeResource {

    @Inject
    RepositoryService repository;
    @GET
    public TemplateInstance home() {
        return Templates.App.home()
                .data("title","Home page")
                .data("repository",repository);
    }
}
