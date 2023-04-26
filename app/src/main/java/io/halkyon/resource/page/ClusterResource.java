package io.halkyon.resource.page;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.providers.multipart.MultipartForm;

import io.halkyon.Templates;
import io.halkyon.exceptions.ClusterConnectException;
import io.halkyon.model.Cluster;
import io.halkyon.resource.requests.ClusterRequest;
import io.halkyon.services.ApplicationDiscoveryJob;
import io.halkyon.services.ClusterStatus;
import io.halkyon.services.KubernetesClientService;
import io.halkyon.services.ServiceDiscoveryJob;
import io.halkyon.utils.AcceptedResponseBuilder;
import io.halkyon.utils.FilterableQueryBuilder;
import io.halkyon.utils.StringUtils;
import io.quarkus.qute.TemplateInstance;

@Path("/clusters")
public class ClusterResource {

    private static final Logger LOG = Logger.getLogger(ClusterResource.class);

    @Inject
    Validator validator;
    @Inject
    KubernetesClientService kubernetesClientService;
    @Inject
    ServiceDiscoveryJob serviceDiscoveryJob;
    @Inject
    ApplicationDiscoveryJob applicationDiscoveryJob;

    @GET
    @Path("/new")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance newClusterForm() {
        return Templates.Clusters.form("Cluster form", new Cluster());
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance list() {
        return Templates.Clusters.list("Clusters", Cluster.listAll(), Cluster.count(), Collections.emptyMap());
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    @Path("/filter")
    public Response filter(@QueryParam("name") String name, @QueryParam("environment") String environment,
            @QueryParam("url") String url, @QueryParam("status") String status) {
        FilterableQueryBuilder query = new FilterableQueryBuilder();
        if (!StringUtils.isNullOrEmpty(name)) {
            query.containsIgnoreCase("name", name);
        }

        if (!StringUtils.isNullOrEmpty(environment)) {
            query.startsWith("environment", environment);
        }

        if (!StringUtils.isNullOrEmpty(url)) {
            query.containsIgnoreCase("url", url);
        }

        if (!StringUtils.isNullOrEmpty(status)) {
            query.equals("status", StringUtils.equalsIgnoreCase("on", status) ? ClusterStatus.OK : ClusterStatus.ERROR);
        }

        List<Cluster> clusters = Cluster.list(query.build(), query.getParameters());
        return Response.ok(Templates.Clusters.table(clusters, clusters.size(), query.getFilterAsMap())).build();
    }

    @POST
    @Transactional
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.TEXT_HTML)
    public Response add(@MultipartForm ClusterRequest clusterRequest) throws IOException {
        Set<ConstraintViolation<ClusterRequest>> errors = validator.validate(clusterRequest);
        AcceptedResponseBuilder response = AcceptedResponseBuilder.withLocation("/clusters");

        if (errors.size() > 0) {
            response.withErrors(errors);
        } else {
            Cluster cluster = new Cluster();
            doUpdateCluster(cluster, clusterRequest);
            response.withSuccessMessage(cluster.id);
            serviceDiscoveryJob.checkCluster(cluster);
            applicationDiscoveryJob.syncApplicationsInCluster(cluster);
        }

        // Return as HTML the template rendering the item for HTMX
        return response.build();
    }

    @GET
    @Path("/{id:[0-9]+}")
    @Consumes(MediaType.TEXT_HTML)
    @Produces(MediaType.TEXT_HTML)
    public Object edit(@PathParam("id") Long id) {
        Cluster cluster = Cluster.findById(id);
        if (cluster == null) {
            throw new NotFoundException(String.format("Cluster not found for id: %d%n", id));
        }
        return Templates.Clusters.form("Cluster " + id + " form", cluster);
    }

    @PUT
    @Path("/{id:[0-9]+}")
    @Transactional
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.TEXT_HTML)
    public Response edit(@PathParam("id") Long id, @MultipartForm ClusterRequest clusterRequest) throws IOException {
        Cluster cluster = Cluster.findById(id);
        if (cluster == null) {
            throw new NotFoundException(String.format("Cluster not found for id: %d%n", id));
        }

        Set<ConstraintViolation<ClusterRequest>> errors = validator.validate(clusterRequest);
        AcceptedResponseBuilder response = AcceptedResponseBuilder.withLocation("/clusters/" + id);

        if (errors.size() > 0) {
            response.withErrors(errors);
        } else {
            doUpdateCluster(cluster, clusterRequest);
            serviceDiscoveryJob.checkCluster(cluster);
            applicationDiscoveryJob.syncApplicationsInCluster(cluster);

            response.withUpdateSuccessMessage(cluster.id);
        }

        // Return as HTML the template rendering the item for HTMX
        return response.build();
    }

    @DELETE
    @Path("/{id}")
    @Transactional
    public TemplateInstance delete(@PathParam("id") Long id) {
        Cluster.deleteById(id);
        return list();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/name/{name}")
    public Cluster findByName(@PathParam("name") String name) {
        Cluster cluster = Cluster.findByName(name);
        if (cluster == null) {
            throw new NotFoundException("Cluster with name " + name + " does not exist.");
        }
        return cluster;
    }

    private void doUpdateCluster(Cluster cluster, ClusterRequest clusterRequest) {
        cluster.name = clusterRequest.name;
        cluster.url = clusterRequest.url;
        cluster.namespace = clusterRequest.namespace;
        cluster.excludedNamespaces = clusterRequest.excludedNamespaces;
        cluster.environment = clusterRequest.environment;
        cluster.token = clusterRequest.token;
        if (clusterRequest.kubeConfig != null) {
            try {
                cluster.kubeConfig = new String(clusterRequest.kubeConfig.readAllBytes());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        try {
            kubernetesClientService.getClientForCluster(cluster);
            cluster.status = ClusterStatus.OK;
            cluster.persist();
        } catch (ClusterConnectException e) {
            LOG.error("Could not connect with the cluster '" + e.getCluster().name + "'. Caused by: " + e.getMessage());
        }
    }
}
