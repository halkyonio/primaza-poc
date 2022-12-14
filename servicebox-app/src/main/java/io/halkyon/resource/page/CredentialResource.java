package io.halkyon.resource.page;

import io.halkyon.Templates;
import io.halkyon.model.Credential;
import io.halkyon.model.CredentialParameter;
import io.halkyon.model.Service;
import io.halkyon.resource.requests.NewCredentialRequest;
import io.halkyon.utils.AcceptedResponseBuilder;
import io.quarkus.qute.TemplateInstance;
import org.jboss.resteasy.annotations.Form;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Path("/credentials")
public class CredentialResource {

    @Inject
    Validator validator;

    @GET
    @Path("/new")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance newCredential() {
        return Templates.Credentials.form(new Credential())
                .data("services", Service.listAll())
                .data("title","Credential form");
    }

    @POST
    @Transactional
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_HTML)
    public Response add(@Form NewCredentialRequest request, @HeaderParam("HX-Request") boolean hxRequest) {
        Set<ConstraintViolation<NewCredentialRequest>> errors = validator.validate(request);
        AcceptedResponseBuilder response = AcceptedResponseBuilder.withLocation("/credentials");
        if (!errors.isEmpty()) {
            response.withErrors(errors);
        } else {
            Credential credential;
            if (request.id != null && request.id != 0) {
                credential = Credential.findById(request.id);
                if (credential != null ) {
                    credential.name = request.name;
                    credential.username = request.username;
                    credential.password = request.password;
                    credential.service = Service.findById(request.serviceId);
                    credential.params = new ArrayList<>();
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
                    response.withUpdateSuccessMessage(credential.id);
                } else {
                    throw new NotFoundException(String.format("Credential not found for id: %d%n", request.id));
                }
            } else {
                credential = new Credential();
                credential.name = request.name;
                credential.username = request.username;
                credential.password = request.password;
                credential.service = Service.findById(request.serviceId);
                credential.created = new Date(System.currentTimeMillis());
                credential.params = new ArrayList<>();
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
                response.withSuccessMessage(credential.id);
            }
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
    @Path("/credential/{id}")
    @Consumes(MediaType.TEXT_HTML)
    @Produces(MediaType.TEXT_HTML)
    public Object edit(@PathParam("id") Long id) {
        Credential credential = Credential.findById(id);
        if (credential == null) {
            throw new NotFoundException(String.format("Credential not found for id: %d%n", id));
        }
        return Templates.Credentials.form(credential)
                .data("services", Service.listAll());
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    @Consumes(MediaType.APPLICATION_JSON)
    public TemplateInstance list() {
        return showList(Credential.listAll()).data("all", true);
    }

    private TemplateInstance showList(List<Credential> credentials) {
        return Templates.Credentials.list(credentials).data("items", Credential.count());
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
}
