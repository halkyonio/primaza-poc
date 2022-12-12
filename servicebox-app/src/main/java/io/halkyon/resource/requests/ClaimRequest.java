package io.halkyon.resource.requests;

import javax.validation.constraints.NotBlank;

import org.jboss.resteasy.annotations.jaxrs.FormParam;

public class ClaimRequest {

    @NotBlank(message = "Name must not be empty")
    @FormParam
    public String name;
    @NotBlank(message = "Service Requested must not be empty")
    @FormParam
    public String serviceRequested;
    @FormParam
    public String description;
    @FormParam
    public String owner;
}
