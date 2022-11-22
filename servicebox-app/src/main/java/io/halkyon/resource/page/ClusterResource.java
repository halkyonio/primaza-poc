package io.halkyon.resource.page;

import io.halkyon.Templates;
import io.halkyon.model.Cluster;
import io.halkyon.resource.requests.NewClusterRequest;
import io.halkyon.services.ApplicationDiscoveryJob;
import io.halkyon.services.ServiceDiscoveryJob;
import io.halkyon.utils.AcceptedResponseBuilder;
import io.quarkus.qute.TemplateInstance;
import org.jboss.resteasy.annotations.providers.multipart.MultipartForm;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
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
        return Templates.Clusters.form().data("title","Cluster form");
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance list() {
        return Templates.Clusters.list(Cluster.listAll())
                .data("title","Cluster form")
                .data("items", Cluster.count())
                .data("all", true);
    }

    @POST
    @Transactional
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.TEXT_HTML)
    public Response add(@MultipartForm NewClusterRequest clusterRequest, @HeaderParam("HX-Request") boolean hxRequest)
            throws IOException {
        Set<ConstraintViolation<NewClusterRequest>> errors = validator.validate(clusterRequest);
        AcceptedResponseBuilder response = AcceptedResponseBuilder.withLocation("/clusters");

        if (errors.size() > 0) {
            response.withErrors(errors);
        } else {
            Cluster cluster = new Cluster();
            cluster.name = clusterRequest.name;
            cluster.url = clusterRequest.url;
            cluster.environment = clusterRequest.environment;
            cluster.created = new Date(System.currentTimeMillis());
            if (clusterRequest.kubeConfig != null) {
                cluster.kubeConfig = new String(clusterRequest.kubeConfig.readAllBytes());
            }
            serviceDiscoveryJob.checkCluster(cluster);
            cluster.persist();
            applicationDiscoveryJob.syncApplicationsInCluster(cluster);
            response.withSuccessMessage(cluster.id);
        }

        // Return as HTML the template rendering the item for HTMX
        return response.build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/name/{name}")
    public Cluster findByName(@PathParam("name") String name) {
        Cluster cluster = Cluster.findByName(name);
        if (cluster == null) {
            throw new NotFoundException("Cluster with name " + name + " does not exist.");
        }
        return cluster;
    }
}
