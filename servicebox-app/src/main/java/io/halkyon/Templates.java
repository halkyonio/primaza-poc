package io.halkyon;

import io.halkyon.model.Application;
import io.halkyon.model.Claim;
import io.halkyon.model.Cluster;
import io.halkyon.model.Credential;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;

import java.util.List;

public class Templates {

        @CheckedTemplate(basePath = "index")
        public static class Index {
                public static native TemplateInstance home();
        }

        @CheckedTemplate(basePath = "claims", requireTypeSafeExpressions = false)
        public static class Claims {
                public static native TemplateInstance list(List<io.halkyon.model.Claim> claims);
                public static native TemplateInstance table(List<io.halkyon.model.Claim> claims);
                public static native TemplateInstance form(Claim claim);
        }

        @CheckedTemplate(basePath = "services", requireTypeSafeExpressions = false)
        public static class Services {
                public static native TemplateInstance list(List<io.halkyon.model.Service> services);
                public static native TemplateInstance item(io.halkyon.model.Service service);
                public static native TemplateInstance form();
                public static native TemplateInstance listDiscovered(List<io.halkyon.model.Service> services);
                public static native TemplateInstance listDiscoveredTable(List<io.halkyon.model.Service> services);
        }

        @CheckedTemplate(basePath = "credentials", requireTypeSafeExpressions = false)
        public static class Credentials {
                public static native TemplateInstance list(List<io.halkyon.model.Credential> credentials);
                public static native TemplateInstance item(io.halkyon.model.Credential credential);
                public static native TemplateInstance form(Credential credential);
        }

        @CheckedTemplate(basePath = "clusters")
        public static class Clusters {
                public static native TemplateInstance list(List<io.halkyon.model.Cluster> clusters);
                public static native TemplateInstance item(io.halkyon.model.Cluster cluster);
                public static native TemplateInstance form(Cluster cluster);
        }

        @CheckedTemplate(basePath = "applications", requireTypeSafeExpressions = false)
        public static class Applications {
                public static native TemplateInstance list(List<io.halkyon.model.Application> applications);
                public static native TemplateInstance listTable(List<io.halkyon.model.Application> applications);
                public static native TemplateInstance bind(Application application, List<Claim> claims);
        }
}
