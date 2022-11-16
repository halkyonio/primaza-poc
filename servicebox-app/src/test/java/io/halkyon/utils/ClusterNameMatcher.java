package io.halkyon.utils;

import java.util.Objects;

import org.mockito.ArgumentMatcher;

import io.halkyon.model.Cluster;

public class ClusterNameMatcher implements ArgumentMatcher<Cluster> {

    private final String clusterName;

    public ClusterNameMatcher(String clusterName) {
        this.clusterName = clusterName;
    }

    @Override
    public boolean matches(Cluster cluster) {
        if (cluster == null) {
            return false;
        }

        return Objects.equals(clusterName, cluster.name);
    }
}
