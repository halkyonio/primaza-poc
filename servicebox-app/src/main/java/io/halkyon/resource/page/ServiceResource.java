package io.halkyon.resource.page;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
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
        return Templates.Services.form(new Service()).data("title", "Service form");
    }

    @POST
    @Transactional
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_HTML)
    public Response add(@Form Service service, @HeaderParam("HX-Request") boolean hxRequest) {
        Set<ConstraintViolation<Service>> errors = validator.validate(service);
        AcceptedResponseBuilder response = AcceptedResponseBuilder.withLocation("/services");

        if (errors.size() > 0) {
            response.withErrors(errors);
        } else {
            if (Service.count("name=?1 AND version=?2", service.name, service.version) > 0) {
                throw new ClientErrorException("Service name and version already exists", Response.Status.CONFLICT);
            }

            serviceDiscoveryJob.linkServiceInCluster(service);
            service.persist();
            response.withSuccessMessage(service.id);
        }

        // Return as HTML the template rendering the item for HTMX
        return response.build();
    }

    @GET
    @Path("/{id:[0-9]+}")
    @Consumes(MediaType.TEXT_HTML)
    @Produces(MediaType.TEXT_HTML)
    public Object edit(@PathParam("id") Long id) {
        Service service = Service.findById(id);
        if (service == null) {
            throw new NotFoundException(String.format("Service not found for id: %d%n", id));
        }
        return Templates.Services.form(service);
    }

    @PUT
    @Path("/{id:[0-9]+}")
    @Transactional
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_HTML)
    public Response edit(@PathParam("id") Long id, @Form Service serviceRequest) throws IOException {
        Service service = Service.findById(id);
        if (service == null) {
            throw new NotFoundException(String.format("Service not found for id: %d%n", id));
        }

        Set<ConstraintViolation<Service>> errors = validator.validate(serviceRequest);
        AcceptedResponseBuilder response = AcceptedResponseBuilder.withLocation("/services/" + id);

        if (errors.size() > 0) {
            response.withErrors(errors);
        } else {
            if (Service.count("id != ?1 AND name=?2 AND version=?3", id, serviceRequest.name,
                    serviceRequest.version) > 0) {
                throw new ClientErrorException("Service name and version already exists", Response.Status.CONFLICT);
            }

            service.name = serviceRequest.name;
            service.version = serviceRequest.version;
            service.type = serviceRequest.type;
            service.database = serviceRequest.database;
            service.endpoint = serviceRequest.endpoint;
            service.externalEndpoint = serviceRequest.externalEndpoint;

            serviceDiscoveryJob.linkServiceInCluster(service);
            service.persist();

            response.withUpdateSuccessMessage(service.id);
        }

        // Return as HTML the template rendering the item for HTMX
        return response.build();
    }

    @DELETE
    @Path("/{id}")
    @Produces(MediaType.TEXT_HTML)
    @Transactional
    public TemplateInstance delete(@PathParam("id") Long id) {
        Service.deleteById(id);
        return list();
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
    @Path("/name/{name}")
    public io.halkyon.model.Service findByName(@PathParam("name") String name) {
        Service service = Service.findByName(name);
        if (service == null) {
            throw new NotFoundException("Service with name " + name + " does not exist.");
        }
        return service;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/name/{name}/version/{version}")
    public io.halkyon.model.Service findByNameAndVersion(@PathParam("name") String name,
            @PathParam("version") String version) {
        Service service = Service.findByNameAndVersion(name, version);
        if (service == null) {
            throw new NotFoundException(
                    "Service with name '" + name + "' and version '" + version + "' does not exist.");
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
