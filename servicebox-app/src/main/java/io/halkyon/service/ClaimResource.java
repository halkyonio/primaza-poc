package io.halkyon.service;

import io.halkyon.model.Claim;
import io.quarkus.hibernate.orm.rest.data.panache.PanacheEntityResource;
import io.quarkus.rest.data.panache.ResourceProperties;


/***
 * The "@ResourceProperties" annotation of the Quarkus JAX-RS panache extension generates the JAX-RS CRUD endpoints for the ClaimResource interface.
 * More information is available within the quarkus documentation: <a href="https://quarkus.io/guides/rest-data-panache">https://quarkus.io/guides/rest-data-panache</a>
 * The endpoints generated support as HTTP request/response, the `application/json` content type.
 * The endpoints can be overridden, extended using the @see io.halkyon.template.ClaimPage class.
 */
@ResourceProperties(path = "claims")
public interface ClaimResource extends PanacheEntityResource<Claim, Long> { }
