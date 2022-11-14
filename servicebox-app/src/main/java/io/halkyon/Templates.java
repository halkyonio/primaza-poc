package io.halkyon;

import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;

import java.util.List;

public class Templates {

        @CheckedTemplate(basePath = "app")
        public static class App {
                public static native TemplateInstance home();
        }

        @CheckedTemplate(basePath = "claims", requireTypeSafeExpressions = false)
        public static class Claims {
                public static native TemplateInstance list(List<io.halkyon.model.Claim> claims);
                public static native TemplateInstance table(List<io.halkyon.model.Claim> claims);
                public static native TemplateInstance form();
        }

        @CheckedTemplate(basePath = "services")
        public static class Services {
                public static native TemplateInstance list(List<io.halkyon.model.Service> services);
                public static native TemplateInstance item(io.halkyon.model.Service service);
                public static native TemplateInstance form();
        }

        @CheckedTemplate(basePath = "clusters")
        public static class Clusters {
                public static native TemplateInstance list(List<io.halkyon.model.Cluster> clusters);
                public static native TemplateInstance item(io.halkyon.model.Cluster cluster);
                public static native TemplateInstance form();
        }
}
