package io.halkyon;

import io.halkyon.model.Claim;
import io.quarkus.hibernate.orm.rest.data.panache.PanacheEntityResource;
import io.quarkus.rest.data.panache.ResourceProperties;

@ResourceProperties(path = "claims")
public interface ClaimResource extends PanacheEntityResource<Claim, Long> {


}
