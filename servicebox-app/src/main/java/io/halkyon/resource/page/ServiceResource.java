package io.halkyon.resource.page;

import java.sql.Date;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.jboss.resteasy.annotations.Form;

import io.halkyon.Templates;
import io.halkyon.model.Service;
import io.halkyon.services.ServiceDiscoveryJob;
import io.halkyon.utils.AcceptedResponseBuilder;
import io.quarkus.qute.TemplateInstance;

@Path("/services")
public class ServiceResource {

    @Inject
    Validator validator;

    @Inject
    ServiceDiscoveryJob serviceDiscoveryJob;

    @GET
    @Path("/new")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance service() {
        return Templates.Services.form().data("title","Service form");
    }

    @POST
    @Transactional
    @Consumes("application/x-www-form-urlencoded")
    @Produces(MediaType.TEXT_HTML)
    public Response add(@Form io.halkyon.model.Service service, @HeaderParam("HX-Request") boolean hxRequest) {
        Set<ConstraintViolation<Service>> errors = validator.validate(service);
        AcceptedResponseBuilder response = AcceptedResponseBuilder.withLocation("/services");

        if (errors.size() > 0) {
            response.withErrors(errors);
        } else {
            if (service.created == null) {
                service.created = new Date(System.currentTimeMillis());
            }

            serviceDiscoveryJob.checkService(service);
            service.persist();
            response.withSuccessMessage(service.id);
        }

        // Return as HTML the template rendering the item for HTMX
        return response.build();
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    @Consumes(MediaType.APPLICATION_JSON)
    public TemplateInstance list() {
        return showList(io.halkyon.model.Service.listAll()).data("all", true);
    }

    private TemplateInstance showList(List<Service> services) {
        return Templates.Services.list(services).data("items", io.halkyon.model.Service.count());
    }


    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/name/{name}")
    public io.halkyon.model.Service findByName(@PathParam("name") String name) {
        Service service = Service.findByName(name);
        if (service == null) {
            throw new WebApplicationException("Service with name " + name + " does not exist.", 404);
        }
        return service;
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/discovered")
    public TemplateInstance listDiscoveredServices() {
        List<Service> discoveredServices = Service.findAvailableServices();
        return Templates.Services.listDiscovered(discoveredServices).data("items", discoveredServices.size());
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/discovered/polling")
    public TemplateInstance pollingDiscoveredServices() {
        List<Service> discoveredServices = Service.findAvailableServices();
        return Templates.Services.listDiscoveredTable(discoveredServices).data("items", discoveredServices.size());
    }
}
