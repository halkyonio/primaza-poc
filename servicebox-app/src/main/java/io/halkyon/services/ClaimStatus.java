package io.halkyon.services;

public enum ClaimStatus {
    // When claim is created
    NEW,
    // When claim is waiting for the service requested to be linked and available.
    PENDING,
    // When claim has a matching service that has been found in a cluster and also has at least one credential
    // installed.
    BIND,
    // When the claim exceeded the attempts to find an available service.
    ERROR;

    @Override
    public String toString() {
        return name().toLowerCase();
    }
}
