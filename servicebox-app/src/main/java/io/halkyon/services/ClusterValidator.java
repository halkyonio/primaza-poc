package io.halkyon.services;

import java.util.ArrayList;
import java.util.List;

import io.halkyon.resource.requests.NewClusterRequest;

public final class ClusterValidator {

    private ClusterValidator() {

    }

    public static List<String> validateCluster(NewClusterRequest cluster) {
        List<String> errors = new ArrayList<>();

        if (cluster == null) {
            errors.add("Cluster was not sent");
        }

        if (cluster.name == null || cluster.name.isEmpty()) {
            errors.add("Cluster name must not be null");
        }

        if (cluster.url == null || cluster.url.isEmpty()) {
            errors.add("Cluster URL must not be null");
        }

        if (cluster.environment == null || cluster.environment.isEmpty()) {
            errors.add("Cluster environment must not be null");
        }

        return errors;
    }
}
