package io.halkyon.services;

public enum ClaimStatus {
    // When claim is just created, not processed yet
    NEW,
    // When claim is waiting for the service requested to be linked and available.
    PENDING,
    // When claim that has linked a service with a credential
    BINDABLE,
    // When claim that is used by one application
    BOUND,
    // When the claim exceeded the attempts to find an available service.
    ERROR;

    @Override
    public String toString() {
        return name().toLowerCase();
    }
}
