package io.halkyon.resource.requests;

import java.util.List;

import org.jboss.resteasy.annotations.jaxrs.FormParam;

public class NewCredentialRequest {
    @FormParam
    public String name;

    @FormParam
    public Long serviceId;

    @FormParam
    public String username;

    @FormParam
    public String password;

    @FormParam
    public List<String> params;
}
