package io.halkyon;

import io.quarkus.qute.TemplateGlobal;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
@TemplateGlobal
public class ApplicationProperties {

    @ConfigProperty(name = "github.repo", defaultValue = "http://github.com/halkyonio/primaza-poc")
    static String gGitHubRepo;
    @ConfigProperty(name = "git.sha.commit", defaultValue = "666")
    static String gGitShaCommit;
}