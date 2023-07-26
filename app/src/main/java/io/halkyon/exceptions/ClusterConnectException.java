package io.halkyon.exceptions;

public class ClusterConnectException extends Exception {
    private final String clusterName;

    public ClusterConnectException(String clusterName, Exception cause) {
        super(cause);

        this.clusterName = clusterName;
    }

    public String getClusterName() {
        return clusterName;
    }
}
