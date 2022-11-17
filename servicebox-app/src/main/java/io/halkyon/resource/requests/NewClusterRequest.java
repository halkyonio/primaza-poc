package io.halkyon.resource.requests;

import java.io.InputStream;

import javax.validation.constraints.NotBlank;
import javax.ws.rs.core.MediaType;

import org.jboss.resteasy.annotations.jaxrs.FormParam;
import org.jboss.resteasy.annotations.providers.multipart.PartType;

public class NewClusterRequest {
    @NotBlank
    @FormParam
    public String name;

    @NotBlank
    @FormParam
    public String url;

    @NotBlank
    @FormParam
    public String environment;

    @FormParam
    @PartType(MediaType.APPLICATION_OCTET_STREAM)
    public InputStream kubeConfig;
}
