package io.halkyon.exceptions;

import io.halkyon.model.Cluster;

public class ClusterConnectException extends Exception {
    private final Cluster cluster;

    public ClusterConnectException(Cluster cluster, Exception cause) {
        super(cause);

        this.cluster = cluster;
    }

    public Cluster getCluster() {
        return cluster;
    }
}
