package io.halkyon;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@Named
@ApplicationScoped
public class ApplicationProperties {

    @ConfigProperty(name = "github.repo", defaultValue = "http://github.com/halkyonio/primaza-poc")
    String gGitHubRepo;
    @ConfigProperty(name = "git.sha.commit", defaultValue = "666")
    String gGitShaCommit;

    public ApplicationProperties() {
    }

    public String getGitHubRepo() {
        return gGitHubRepo;
    }

    public String getGitShaCommit() {
        return gGitShaCommit;
    }
}
