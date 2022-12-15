package io.halkyon;

import java.util.List;
import java.util.Map;

import io.halkyon.model.*;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;

public class Templates {

    @CheckedTemplate(basePath = "index")
    public static class Index {
        public static native TemplateInstance home();
    }

    @CheckedTemplate(basePath = "claims", requireTypeSafeExpressions = false)
    public static class Claims {
        public static native TemplateInstance list(List<Claim> claims, List<Service> services, long items,
                Map<String, Object> filter);

        public static native TemplateInstance table(List<Claim> claims, long items, Map<String, Object> filter);

        public static native TemplateInstance form(Claim claim, List<Service> services);
    }

    @CheckedTemplate(basePath = "services", requireTypeSafeExpressions = false)
    public static class Services {
        public static native TemplateInstance list(List<Service> services, long items, Map<String, Object> filter);

        public static native TemplateInstance table(List<Service> services, long items, Map<String, Object> filter);

        public static native TemplateInstance item(Service service);

        public static native TemplateInstance form(Service service);

        public static native TemplateInstance listDiscovered(List<Service> services);

        public static native TemplateInstance listDiscoveredTable(List<Service> services);
    }

    @CheckedTemplate(basePath = "credentials", requireTypeSafeExpressions = false)
    public static class Credentials {
        public static native TemplateInstance list(List<Credential> credentials, long items);

        public static native TemplateInstance item(Credential credential);

        public static native TemplateInstance form(Credential credential, List<Service> services);
    }

    @CheckedTemplate(basePath = "clusters")
    public static class Clusters {
        public static native TemplateInstance list(List<Cluster> clusters, long items);

        public static native TemplateInstance item(Cluster cluster);

        public static native TemplateInstance form(Cluster cluster);
    }

    @CheckedTemplate(basePath = "applications", requireTypeSafeExpressions = false)
    public static class Applications {
        public static native TemplateInstance list(List<Application> applications, long items);

        public static native TemplateInstance listTable(List<Application> applications, long items);

        public static native TemplateInstance bind(Application application, List<Claim> claims);
    }
}
