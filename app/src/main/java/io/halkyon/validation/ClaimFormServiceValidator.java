package io.halkyon.validation;

import static io.halkyon.utils.StringUtils.isNotEmpty;
import static io.halkyon.utils.StringUtils.isNullOrEmpty;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import io.halkyon.resource.requests.ClaimRequest;

public class ClaimFormServiceValidator implements ConstraintValidator<ValidService, ClaimRequest> {
    @Override
    public boolean isValid(ClaimRequest claim, ConstraintValidatorContext constraintValidatorContext) {
        if (isNullOrEmpty(claim.serviceRequested) && isNullOrEmpty(claim.serviceId)) {
            return false;
        } else if (isNotEmpty(claim.serviceRequested) && isNotEmpty(claim.serviceId)) {
            return false;
        }

        return true;
    }
}
