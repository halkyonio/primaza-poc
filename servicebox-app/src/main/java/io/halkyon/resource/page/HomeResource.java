package io.halkyon.resource.page;

import io.halkyon.Templates;
import io.quarkus.qute.TemplateInstance;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("/home")
public class HomeResource {
    @GET
    public TemplateInstance home() {
        return Templates.home().data("title","Home page");
    }
}
