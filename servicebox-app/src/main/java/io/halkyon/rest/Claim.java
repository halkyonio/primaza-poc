package rest;

import io.quarkiverse.renarde.Controller;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;

import javax.ws.rs.Path;

public class Claim extends Controller {

    @CheckedTemplate
    static class Templates {
        public static native TemplateInstance index();
    }

    @Path("/")
    public TemplateInstance index() {
        return Claim.Templates.index();
    }
}
