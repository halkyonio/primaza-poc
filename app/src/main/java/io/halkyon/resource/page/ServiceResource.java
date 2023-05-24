package io.halkyon.resource.page;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.jboss.resteasy.annotations.Form;

import io.halkyon.Templates;
import io.halkyon.exceptions.ClusterConnectException;
import io.halkyon.model.Service;
import io.halkyon.model.ServiceDiscovered;
import io.halkyon.resource.requests.ServiceRequest;
import io.halkyon.services.KubernetesClientService;
import io.halkyon.services.ServiceDiscoveryJob;
import io.halkyon.utils.AcceptedResponseBuilder;
import io.halkyon.utils.FilterableQueryBuilder;
import io.halkyon.utils.StringUtils;
import io.quarkus.qute.TemplateInstance;

@Path("/services")
public class ServiceResource {

    @Inject
    Validator validator;

    @Inject
    ServiceDiscoveryJob serviceDiscoveryJob;

    @Inject
    KubernetesClientService kubernetesClientService;

    @GET
    @Path("/new")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance service() {
        return Templates.Services.form("Service form", new Service());
    }

    @POST
    @Transactional
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_HTML)
    public Response add(@Form ServiceRequest request, @HeaderParam("HX-Request") boolean hxRequest) {
        Set<ConstraintViolation<ServiceRequest>> errors = validator.validate(request);
        AcceptedResponseBuilder response = AcceptedResponseBuilder.withLocation("/services");

        if (errors.size() > 0) {
            response.withErrors(errors);
        } else {
            if (Service.count("name=?1 AND version=?2", request.name, request.version) > 0) {
                throw new ClientErrorException(
                        "Service name" + request.name + " and version " + request.version + " already exists",
                        Response.Status.CONFLICT);
            }
            Service service = new Service();
            doUpdateService(service, request);
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
        return Templates.Services.form("Service " + id + " form", service);
    }

    @PUT
    @Path("/{id:[0-9]+}")
    @Transactional
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_HTML)
    public Response edit(@PathParam("id") Long id, @Form ServiceRequest request) throws IOException {
        Service service = Service.findById(id);
        if (service == null) {
            throw new NotFoundException(String.format("Service not found for id: %d%n", id));
        }

        Set<ConstraintViolation<ServiceRequest>> errors = validator.validate(request);
        AcceptedResponseBuilder response = AcceptedResponseBuilder.withLocation("/services/" + id);

        if (errors.size() > 0) {
            response.withErrors(errors);
        } else {
            if (Service.count("id != ?1 AND name=?2 AND version=?3", id, request.name, request.version) > 0) {
                throw new ClientErrorException("Service name and version already exists", Response.Status.CONFLICT);
            }

            doUpdateService(service, request);

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
    public TemplateInstance list() {
        return Templates.Services.list("Services", Service.listAll(), Service.count(), Collections.emptyMap());
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    @Path("/filter")
    public Response filter(@QueryParam("name") String name, @QueryParam("version") String version,
            @QueryParam("type") String type, @QueryParam("endpoint") String endpoint,
            @QueryParam("available") String available) {
        FilterableQueryBuilder query = new FilterableQueryBuilder();
        if (!StringUtils.isNullOrEmpty(name)) {
            query.containsIgnoreCase("name", name);
        }

        if (!StringUtils.isNullOrEmpty(version)) {
            query.startsWith("version", version);
        }

        if (!StringUtils.isNullOrEmpty(type)) {
            query.startsWith("type", type);
        }

        if (!StringUtils.isNullOrEmpty(endpoint)) {
            query.containsIgnoreCase("endpoint", endpoint);
        }

        if (!StringUtils.isNullOrEmpty(available)) {
            query.equals("available", StringUtils.equalsIgnoreCase("on", available));
        }

        List<Service> services = Service.list(query.build(), query.getParameters());
        return Response.ok(Templates.Services.table(services, services.size(), query.getFilterAsMap())).build();
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
    public TemplateInstance listDiscoveredServices() throws ClusterConnectException {
        List<ServiceDiscovered> servicesDiscovered = kubernetesClientService.discoverServicesInCluster();
        return Templates.Services.listDiscovered("Services available", servicesDiscovered, servicesDiscovered.size());
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/discovered/polling")
    public TemplateInstance pollingDiscoveredServices() throws ClusterConnectException {
        List<ServiceDiscovered> servicesDiscovered = kubernetesClientService.discoverServicesInCluster();
        return Templates.Services.listDiscovered("Services available", servicesDiscovered, servicesDiscovered.size());
    }

    private void doUpdateService(Service service, ServiceRequest request) {
        service.name = request.name;
        service.version = request.version;
        service.type = request.type;
        service.endpoint = request.endpoint;
        service.externalEndpoint = request.externalEndpoint;
        if (request.installable != null && request.installable.equals("on")) {
            service.installable = true;
        } else {
            service.installable = false;
        }
        service.helmRepo = request.helmRepo;
        service.helmChart = request.helmChart;
        service.helmChartVersion = request.helmChartVersion;

        if (StringUtils.isNotEmpty(service.externalEndpoint)) {
            service.available = true;
        } else {
            serviceDiscoveryJob.linkServiceInCluster(service);
        }

        service.persist();
    }
}
