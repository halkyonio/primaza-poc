package io.halkyon.resource.page;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import io.halkyon.Templates;
import io.halkyon.model.Application;
import io.quarkus.qute.TemplateInstance;

@Path("/applications")
public class ApplicationResource {

    @GET
    @Produces(MediaType.TEXT_HTML)
    @Consumes(MediaType.APPLICATION_JSON)
    public TemplateInstance list() {
        List<Application> applications = Application.listAll();
        return Templates.Applications.list(applications)
                .data("items", applications.size());
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/polling")
    public TemplateInstance pollingApplications() {
        List<Application> applications = Application.listAll();
        return Templates.Applications.listTable(applications)
                .data("items", applications.size());
    }
}
