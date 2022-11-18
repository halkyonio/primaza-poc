package io.halkyon;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;

@ApplicationScoped
public class ApplicationProperties {

    @ConfigProperty(name = "github.repo", defaultValue = "http://github.com/halkyonio/primaza-poc")
    public String gGitHubRepo;
    @ConfigProperty(name = "git.sha.commit", defaultValue = "666")
    public String gGitShaCommit;
}