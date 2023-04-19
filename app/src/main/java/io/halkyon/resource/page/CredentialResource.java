package io.halkyon.resource.page;

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
import io.halkyon.model.Credential;
import io.halkyon.model.CredentialParameter;
import io.halkyon.model.Service;
import io.halkyon.resource.requests.CredentialRequest;
import io.halkyon.utils.AcceptedResponseBuilder;
import io.halkyon.utils.FilterableQueryBuilder;
import io.halkyon.utils.StringUtils;
import io.quarkus.qute.TemplateInstance;

@Path("/credentials")
public class CredentialResource {

    @Inject
    Validator validator;

    @GET
    @Path("/new")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance newCredential() {
        return Templates.Credentials.form("Credential form", new Credential(), Service.listAll());
    }

    @POST
    @Transactional
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_HTML)
    public Response add(@Form CredentialRequest request, @HeaderParam("HX-Request") boolean hxRequest) {
        Set<ConstraintViolation<CredentialRequest>> errors = validator.validate(request);
        AcceptedResponseBuilder response = AcceptedResponseBuilder.withLocation("/credentials");
        if (!errors.isEmpty()) {
            response.withErrors(errors);
        } else {
            Credential credential = new Credential();
            doUpdateCredential(credential, request);
            response.withSuccessMessage(credential.id);
        }

        // Return as HTML the template rendering the item for HTMX
        return response.build();
    }

    @DELETE
    @Path("/{id}")
    @Produces(MediaType.TEXT_HTML)
    @Transactional
    public TemplateInstance delete(@PathParam("id") Long id) {
        Credential.deleteById(id);
        return list();
    }

    @GET
    @Path("/{id:[0-9]+}")
    @Produces(MediaType.TEXT_HTML)
    public Object edit(@PathParam("id") Long id) {
        Credential credential = Credential.findById(id);
        if (credential == null) {
            throw new NotFoundException(String.format("Credential not found for id: %d%n", id));
        }
        return Templates.Credentials.form("Credential " + id + " form", credential, Service.listAll());
    }

    @PUT
    @Path("/{id:[0-9]+}")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_HTML)
    @Transactional
    public Object edit(@PathParam("id") Long id, @Form CredentialRequest request) {
        Credential credential = Credential.findById(id);
        if (credential == null) {
            throw new NotFoundException(String.format("Credential not found for id: %d%n", id));
        }

        Set<ConstraintViolation<CredentialRequest>> errors = validator.validate(request);
        AcceptedResponseBuilder response = AcceptedResponseBuilder.withLocation("/credentials/" + credential.id);

        if (errors.size() > 0) {
            response.withErrors(errors);
        } else {
            doUpdateCredential(credential, request);
            response.withUpdateSuccessMessage(credential.id);
        }

        // Return as HTML the template rendering the item for HTMX
        return response.build();
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance list() {
        return Templates.Credentials.list("Credentials", Credential.listAll(), Credential.count(),
                Collections.emptyMap());
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    @Path("/filter")
    public Response filter(@QueryParam("name") String name, @QueryParam("service.name") String service,
            @QueryParam("username") String username, @QueryParam("password") String password) {
        FilterableQueryBuilder query = new FilterableQueryBuilder();
        if (!StringUtils.isNullOrEmpty(name)) {
            query.containsIgnoreCase("name", name);
        }

        if (!StringUtils.isNullOrEmpty(service)) {
            query.containsIgnoreCase("service.name", service);
        }

        if (!StringUtils.isNullOrEmpty(username)) {
            query.containsIgnoreCase("username", username);
        }

        if (!StringUtils.isNullOrEmpty(password)) {
            query.containsIgnoreCase("password", password);
        }

        List<Credential> credentials = Credential.list(query.build(), query.getParameters());
        return Response.ok(Templates.Credentials.table(credentials, credentials.size(), query.getFilterAsMap()))
                .build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/name/{name}")
    public Credential findByName(@PathParam("name") String name) {
        Credential credential = Credential.findByName(name);
        if (credential == null) {
            throw new NotFoundException("Credential with name " + name + " does not exist.");
        }
        return credential;
    }

    private void doUpdateCredential(Credential credential, CredentialRequest request) {
        credential.name = request.name;
        credential.username = request.username;
        credential.password = request.password;
        credential.vaultKvPath = request.vaultKvPath;
        credential.service = Service.findById(request.serviceId);
        credential.params.clear();
        if (request.params != null) {
            for (String param : request.params) {
                String[] nameValue = param.split(":");
                if (nameValue.length == 2) {
                    CredentialParameter paramEntity = new CredentialParameter();
                    paramEntity.credential = credential;
                    paramEntity.paramName = nameValue[0];
                    paramEntity.paramValue = nameValue[1];
                    credential.params.add(paramEntity);
                }
            }
        }

        credential.persist();
    }
}
