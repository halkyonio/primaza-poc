package io.halkyon.resource.requests;

import java.io.InputStream;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.ws.rs.core.MediaType;

import org.jboss.resteasy.annotations.jaxrs.FormParam;
import org.jboss.resteasy.annotations.providers.multipart.PartType;

public class ClusterRequest {

    @FormParam
    public String id;

    @NotBlank(message = "Name must not be empty")
    @FormParam
    public String name;

    @NotBlank(message = "URL must not be empty")
    @Pattern(regexp = "^(https://|http://)?[\\w\\.]+(:[\\d]+)?$", message = "URL is not correct. It must be a valid hostname and port number.")
    @FormParam
    public String url;

    @FormParam
    public String namespace;

    @FormParam
    public String excludedNamespaces;

    @NotBlank(message = "Environment must not be empty")
    @FormParam
    public String environment;

    @FormParam
    @PartType(MediaType.APPLICATION_OCTET_STREAM)
    public InputStream kubeConfig;
    @FormParam
    public String token;
}
