package io.halkyon.resource.page;

import io.halkyon.Templates;
import io.quarkus.qute.TemplateInstance;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("/home")
public class HomeResource {

    @ConfigProperty(name = "github.repo")
    String githubRepo;
    @ConfigProperty(name = "git.sha.commit")
    String gitShaCommit;
    @GET
    public TemplateInstance home() {
        return Templates.App.home()
                .data("title","Home page")
                .data("git-sha-commit",gitShaCommit)
                .data("github-repo",githubRepo);
    }
}
