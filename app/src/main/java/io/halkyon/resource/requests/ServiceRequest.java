package io.halkyon.resource.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import org.jboss.resteasy.annotations.jaxrs.FormParam;

public class ServiceRequest {

    @NotBlank(message = "Name must not be empty")
    @FormParam
    public String name;
    @NotBlank(message = "Version must not be empty")
    @FormParam
    public String version;
    @NotBlank(message = "Type must not be empty")
    @FormParam
    public String type;
    /* in form of tcp:8080 */
    @NotBlank(message = "Service endpoint must not be empty")
    @Pattern(regexp = "\\w+:\\d+", message = "Wrong format in service endpoint. It must be 'protocol:port'")
    @FormParam
    public String endpoint;
    @FormParam
    public String externalEndpoint;
    @FormParam
    public boolean installable;
    @FormParam
    public String helmRepo;
    @FormParam
    public String helmChart;
    @FormParam
    public String helmChartVersion;
}
