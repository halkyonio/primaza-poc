package io.halkyon.resource.page;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotAcceptableException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.halkyon.Templates;
import io.halkyon.exceptions.ClusterConnectException;
import io.halkyon.model.Application;
import io.halkyon.model.Claim;
import io.halkyon.services.BindApplicationService;
import io.halkyon.utils.FilterableQueryBuilder;
import io.halkyon.utils.StringUtils;
import io.quarkus.qute.TemplateInstance;

@Path("/applications")
public class ApplicationResource {

    @Inject
    BindApplicationService bindService;

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance list() {
        List<Application> applications = Application.listAll();
        return Templates.Applications.list("Applications", applications, Application.count(), Collections.emptyMap());
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    @Path("/filter")
    public Response filter(@QueryParam("name") String name, @QueryParam("namespace") String namespace,
            @QueryParam("image") String image, @QueryParam("cluster.name") String clusterName) {
        FilterableQueryBuilder query = new FilterableQueryBuilder();
        if (!StringUtils.isNullOrEmpty(name)) {
            query.containsIgnoreCase("name", name);
        }

        if (!StringUtils.isNullOrEmpty(namespace)) {
            query.containsIgnoreCase("namespace", namespace);
        }

        if (!StringUtils.isNullOrEmpty(image)) {
            query.containsIgnoreCase("image", image);
        }

        if (!StringUtils.isNullOrEmpty(clusterName)) {
            query.containsIgnoreCase("cluster.name", clusterName);
        }

        List<Application> applications = Application.list(query.build(), query.getParameters());
        return Response.ok(Templates.Applications.table(applications, applications.size(), query.getFilterAsMap()))
                .build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/name/{name}")
    public Application findByName(@PathParam("name") String name) {
        Application application = Application.findByName(name);
        if (application == null) {
            throw new NotFoundException("Application with name " + name + " does not exist.");
        }

        return application;
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    @Path("/claim/{id}")
    public TemplateInstance claimApplicationModal(@PathParam("id") long applicationId) {
        return Templates.Applications.bind(Application.findById(applicationId), Claim.listAvailable());
    }

    @Transactional
    @POST
    @Produces(MediaType.TEXT_HTML)
    @Path("/claim/{id}")
    public Response doClaimApplication(@PathParam("id") long applicationId, @FormParam("claimId") long claimId) {
        Application application = Application.findById(applicationId);
        if (application == null) {
            throw new NotFoundException(String.format("Application %s not found", applicationId));
        }

        Claim claim = Claim.findById(claimId);
        if (claim == null) {
            throw new NotFoundException(String.format("Claim %s not found", claimId));
        }
        if (claim.service == null) {
            throw new NotAcceptableException(String.format("Claim %s has no services available", claimId));
        }
        if (claim.service.credentials == null || claim.service.credentials.isEmpty()) {
            throw new NotAcceptableException(String.format("Service %s has no credentials", claim.service.name));
        }
        claim.application = application;
        try {
            bindService.bindApplication(claim);
            claim.persist();
            return Response.ok().build();
        } catch (ClusterConnectException ex) {
            throw new InternalServerErrorException(
                    "Can't bind the application " + application.name + " because can't connect " + "with the cluster "
                            + ex.getCluster() + ". Cause: " + ex.getMessage());
        }

    }
}
