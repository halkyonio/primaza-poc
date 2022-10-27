package io.halkyon.resource.page;

import io.halkyon.Templates;
import io.quarkus.qute.TemplateInstance;
import org.jboss.resteasy.annotations.Form;

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
import java.util.List;

@Path("/services")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ServicesResource {

    @GET
    public TemplateInstance list() {
        return showList(io.halkyon.model.Service.listAll()).data("all", true);
    }

    private TemplateInstance showList(List<io.halkyon.model.Service> services) {
        return Templates.serviceList(services).data("items", io.halkyon.model.Service.count());
    }


    @GET
    @Path("/{name}")
    public io.halkyon.model.Service findByName(@PathParam("name") String name) {
        return io.halkyon.model.Service.findByName(name);
    }


}
