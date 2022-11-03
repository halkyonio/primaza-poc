package io.halkyon.services;

public enum ClaimStatus {
    // When claim is created
    NEW,
    // When claim is waiting for a service to be available
    PENDING,
    // When claim has a linked service
    CLAIMED,
    // When the claim exceeded the attempts to find an available service.
    ERROR,
    REJECTED,
    BIND;

    @Override
    public String toString() {
        return name().toLowerCase();
    }
}
