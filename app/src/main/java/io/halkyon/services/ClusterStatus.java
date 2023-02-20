package io.halkyon.services;

public enum ClusterStatus {
    // When the connection with the cluster worked.
    OK,
    // When the connection with the cluster failed. See error message for further information.
    ERROR;
}
