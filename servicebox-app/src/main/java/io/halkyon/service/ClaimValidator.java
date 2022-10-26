package io.halkyon.service;

import io.halkyon.model.Claim;

import javax.inject.Singleton;
import javax.ws.rs.BadRequestException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Singleton
public class ClaimValidator {
    public List<String> validateForm(Claim claim) {
        List<String> errors = new ArrayList<>();

        if (claim.name.isEmpty()) {
            errors.add("Claim name must not be null");
        }

        if (claim.description.isEmpty()) {
            errors.add("Description must not be null");
        }

        if (claim.serviceRequested.isEmpty()) {
            errors.add("The service to be used must not be null");
        }

        return errors;
    }
}
