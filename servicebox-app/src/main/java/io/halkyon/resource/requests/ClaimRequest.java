package io.halkyon.resource.requests;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

import org.jboss.resteasy.annotations.jaxrs.FormParam;

import io.halkyon.validation.ValidService;

@ValidService(message = "One service must be selected or need to type the Service Requested")
public class ClaimRequest {

    @NotBlank(message = "Name must not be empty")
    @FormParam
    public String name;
    @FormParam
    public String serviceId;
    @Pattern(regexp = "^$|.+-.+", message = "Service Requested format must be <service name>-<service version>. Example: mysql-8.")
    @FormParam
    public String serviceRequested;
    @FormParam
    public String description;
    @FormParam
    public String owner;
}
