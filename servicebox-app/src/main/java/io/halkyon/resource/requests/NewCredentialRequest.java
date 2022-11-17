package io.halkyon.resource.requests;

import java.util.List;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import org.jboss.resteasy.annotations.jaxrs.FormParam;

public class NewCredentialRequest {
    @NotBlank
    @FormParam
    public String name;

    @NotNull
    @FormParam
    public Long serviceId;

    @NotBlank
    @FormParam
    public String username;

    @NotBlank
    @FormParam
    public String password;

    @FormParam
    public List<String> params;
}
