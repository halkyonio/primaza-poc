package io.halkyon;

import io.halkyon.model.Claim;
import io.quarkus.hibernate.orm.rest.data.panache.PanacheEntityResource;
import io.quarkus.rest.data.panache.ResourceProperties;


/***
 * REST Data with Panache generates JAX-RS resources based on this interface. You don't need to implement it.
 * For the Claim entity this extension will generate its implementation providing support for CRUD operations and `application/json` as response content types.
 * For extending this interface the @see io.halkyon.ClaimExtendedResource is provided.
 */
@ResourceProperties(path = "claims")
public interface ClaimResource extends PanacheEntityResource<Claim, Long> { }
