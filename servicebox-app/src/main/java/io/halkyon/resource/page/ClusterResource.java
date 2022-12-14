package io.halkyon.resource.page;

import java.io.IOException;
import java.util.Set;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.jboss.resteasy.annotations.providers.multipart.MultipartForm;

import io.halkyon.Templates;
import io.halkyon.model.Cluster;
import io.halkyon.resource.requests.ClusterRequest;
import io.halkyon.services.ApplicationDiscoveryJob;
import io.halkyon.services.ServiceDiscoveryJob;
import io.halkyon.utils.AcceptedResponseBuilder;
import io.quarkus.qute.TemplateInstance;

@Path("/clusters")
public class ClusterResource {

    @Inject
    Validator validator;

    @Inject
    ServiceDiscoveryJob serviceDiscoveryJob;
    @Inject
    ApplicationDiscoveryJob applicationDiscoveryJob;

    @GET
    @Path("/new")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance newClusterForm() {
        return Templates.Clusters.form(new Cluster()).data("title", "Cluster form");
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance list() {
        return Templates.Clusters.list(Cluster.listAll(), Cluster.count()).data("title", "Cluster form");
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
        return Templates.Clusters.form(cluster);
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
        cluster.excludedNamespaces = clusterRequest.excludedNamespaces;
        cluster.environment = clusterRequest.environment;
        if (clusterRequest.kubeConfig != null) {
            try {
                cluster.kubeConfig = new String(clusterRequest.kubeConfig.readAllBytes());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        cluster.persist();
    }
}
