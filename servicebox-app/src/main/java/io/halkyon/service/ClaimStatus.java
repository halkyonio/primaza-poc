package io.halkyon.service;

public enum ClaimStatus {
    NEW,
    PENDING,
    REJECTED,
    BIND;

    @Override
    public String toString() {
        return name().toLowerCase();
    }
}
