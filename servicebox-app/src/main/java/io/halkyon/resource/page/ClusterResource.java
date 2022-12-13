package io.halkyon.resource.page;

import io.halkyon.Templates;
import io.halkyon.model.Cluster;
import io.halkyon.resource.requests.ClusterRequest;
import io.halkyon.services.ApplicationDiscoveryJob;
import io.halkyon.services.ServiceDiscoveryJob;
import io.halkyon.utils.AcceptedResponseBuilder;
import io.quarkus.qute.TemplateInstance;
import org.jboss.resteasy.annotations.providers.multipart.MultipartForm;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.sql.Date;
import java.util.Set;

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
        return Templates.Clusters.form(new Cluster())
                .data("title","Cluster form");
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance list() {
        return Templates.Clusters.list(Cluster.listAll())
                .data("title","Cluster form")
                .data("items", Cluster.count())
                .data("all", true);
    }

    @GET
    @Path("/cluster/{id}")
    @Consumes(MediaType.TEXT_HTML)
    @Produces(MediaType.TEXT_HTML)
    public Object edit(@PathParam("id") Long id) {
        Cluster cluster = Cluster.findById(id);
        if (cluster == null) {
            throw new NotFoundException(String.format("Cluster not found for id: %d%n", id));
        }
        return Templates.Clusters.form(cluster);
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
            Cluster cluster;
            if (clusterRequest.getLongId() != null && clusterRequest.getLongId() != 0) {
                cluster = Cluster.findById(clusterRequest.getLongId());
                if (cluster != null) {
                    cluster.name = clusterRequest.name;
                    cluster.url = clusterRequest.url;
                    cluster.namespaces = clusterRequest.namespaces;
                    cluster.environment = clusterRequest.environment;
                    cluster.kubeConfig = clusterRequest.getKubeConfig();
                    cluster.created = new Date(System.currentTimeMillis());
                } else {
                    throw new NotFoundException(String.format("Cluster not found for id: %d%n", clusterRequest.id));
                }
                response.withUpdateSuccessMessage(cluster.id);
            } else {
                cluster = new Cluster();
                cluster.name = clusterRequest.name;
                cluster.url = clusterRequest.url;
                cluster.namespaces = clusterRequest.namespaces;
                cluster.environment = clusterRequest.environment;
                cluster.created = new Date(System.currentTimeMillis());
                if (clusterRequest.kubeConfig != null) {
                    try {
                        cluster.kubeConfig = new String(clusterRequest.kubeConfig.readAllBytes());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                response.withSuccessMessage(cluster.id);
            }
            cluster.persist();
            serviceDiscoveryJob.checkCluster(cluster);
            applicationDiscoveryJob.syncApplicationsInCluster(cluster);
        }

        // Return as HTML the template rendering the item for HTMX
        return response.build();
    }

    @DELETE
    @Path("/{id}")
    @Transactional
    public TemplateInstance delete(@PathParam("id") Long id) {
        Cluster.deleteById(id);
        return Templates.Clusters.list(Cluster.listAll())
                .data("items", Cluster.count());
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
}
