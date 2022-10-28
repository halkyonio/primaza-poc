package io.halkyon.resource;

import io.halkyon.model.Claim;
import io.halkyon.model.Service;
import io.quarkus.hibernate.orm.rest.data.panache.PanacheEntityResource;
import io.quarkus.rest.data.panache.ResourceProperties;


/***
 * REST Data with Panache extension generates JAX-RS resources based on the presence of this interface.
 * The @ResourceProperties annotation is used to customize the path of the resource.
 * More information is available within the quarkus documentation: <a href="https://quarkus.io/guides/rest-data-panache">https://quarkus.io/guides/rest-data-panache</a>
 * The endpoints generated support as HTTP request/response, the `application/json` content type.
 * The endpoints can be overridden, extended using the @see io.halkyon.template.ServicePageResource class.
 */
@ResourceProperties(path="/services")
public interface ServiceResource extends PanacheEntityResource<Service, Long> { }
