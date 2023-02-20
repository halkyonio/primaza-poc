package io.halkyon.utils;

import java.util.Objects;

import org.mockito.ArgumentMatcher;

import io.halkyon.model.Claim;

public class ClaimNameMatcher implements ArgumentMatcher<Claim> {

    private final String claimName;

    public ClaimNameMatcher(String claimName) {
        this.claimName = claimName;
    }

    @Override
    public boolean matches(Claim claim) {
        if (claim == null) {
            return false;
        }

        return Objects.equals(claimName, claim.name);
    }
}
