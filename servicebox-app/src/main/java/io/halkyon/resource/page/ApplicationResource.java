package io.halkyon.resource.page;

import java.util.List;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.halkyon.Templates;
import io.halkyon.model.Application;
import io.halkyon.model.Claim;
import io.halkyon.services.BindApplicationService;
import io.quarkus.qute.TemplateInstance;

@Path("/applications")
public class ApplicationResource {

    @Inject
    BindApplicationService bindService;

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance list() {
        List<Application> applications = Application.listAll();
        return Templates.Applications.list(applications)
                .data("items", applications.size());
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    @Path("/polling")
    public TemplateInstance pollingApplications() {
        List<Application> applications = Application.listAll();
        return Templates.Applications.listTable(applications)
                .data("items", applications.size());
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    @Path("/bind/{id}")
    public TemplateInstance bindApplicationModal(@PathParam("id") long applicationId) {
        return Templates.Applications.bind(Application.findById(applicationId), Claim.listAvailable()).data("claims", Claim.listAll());
    }

    @Transactional
    @POST
    @Produces(MediaType.TEXT_HTML)
    @Path("/bind/{id}")
    public Response doBindApplication(@PathParam("id") long applicationId, @FormParam("claimId") long claimId) {
        Application application = Application.findById(applicationId);
        Claim claim = Claim.findById(claimId);
        claim.application = application;
        claim.persist();
        application.claim = claim;
        application.persist();
        bindService.bindApplication(application, claim);
        return Response.ok().build();
    }
}
