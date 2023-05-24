package io.halkyon.resource.page;

import java.util.Collections;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.NotAcceptableException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import io.halkyon.Templates;
import io.halkyon.exceptions.ClusterConnectException;
import io.halkyon.model.Application;
import io.halkyon.model.Claim;
import io.halkyon.services.BindApplicationService;
import io.halkyon.services.KubernetesClientService;
import io.halkyon.utils.FilterableQueryBuilder;
import io.halkyon.utils.StringUtils;
import io.quarkus.qute.TemplateInstance;

@Path("/applications")
public class ApplicationResource {

    @Inject
    BindApplicationService bindService;

    @Inject
    KubernetesClientService kubernetesClientService;

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
        if (claim.service.installable) {
            claim.service.cluster = claim.application.cluster;
            claim.service.namespace = claim.application.namespace;
            claim.persist();
            try {
                System.out.println("Service is installable using crossplane. Let's do it :-)");
                kubernetesClientService.createCrossplaneHelmRelease(application.cluster, claim.service);
            } catch (ClusterConnectException ex) {
                throw new InternalServerErrorException(
                        "Can't deploy the service with the cluster " + ex.getCluster() + ". Cause: " + ex.getMessage());
            }
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
