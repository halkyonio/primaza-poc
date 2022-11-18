package io.halkyon.services;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class RepositoryService {
    @ConfigProperty(name = "github.repo", defaultValue = "http://github.com/halkyonio/primaza-poc")
    String githubRepo;
    @ConfigProperty(name = "git.sha.commit", defaultValue = "666")
    String gitShaCommit;
}