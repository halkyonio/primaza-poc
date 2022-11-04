package io.halkyon.resource.page;

import static io.halkyon.services.ClusterValidator.validateCluster;

import java.sql.Date;
import java.util.List;

import javax.transaction.Transactional;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.jboss.resteasy.annotations.Form;

import io.halkyon.Templates;
import io.halkyon.model.Cluster;
import io.quarkus.qute.TemplateInstance;

@Path("/clusters")
public class ClusterResource {

    @GET
    @Path("/new")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance newClusterForm() {
        return Templates.clusterForm().data("title","Cluster form");
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance list() {
        return Templates.clusterList(Cluster.listAll())
                .data("title","Cluster form")
                .data("items", Cluster.count())
                .data("all", true);
    }

    @POST
    @Transactional
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_HTML)
    public Response add(@Form Cluster cluster, @HeaderParam("HX-Request") boolean hxRequest) {
        List<String> errors = validateCluster(cluster);
        StringBuffer response = new StringBuffer();

        if (cluster.created == null) {
            cluster.created = new Date(System.currentTimeMillis());
        }

        cluster.persist();
        if (errors.size() > 0) {
            for(String error : errors) {
                response.append("<div class=\"alert alert-danger\"><strong>Error! </strong>" + error + "</div>");
            };
        } else {
            response.append("<div class=\"alert alert-success\">Cluster created successfully for id: " + cluster.id + "</div>");
        }

        // Return as HTML the template rendering the item for HTMX
        return Response.accepted(response.toString()).status(Response.Status.CREATED).header("Location", "/clusters").build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/name/{name}")
    public Cluster findByName(@PathParam("name") String name) {
        return Cluster.findByName(name);
    }
}
