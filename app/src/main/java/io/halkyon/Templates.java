package io.halkyon;

import java.util.List;
import java.util.Map;

import io.halkyon.model.Application;
import io.halkyon.model.Claim;
import io.halkyon.model.Cluster;
import io.halkyon.model.Credential;
import io.halkyon.model.Service;
import io.halkyon.model.ServiceDiscovered;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;

public class Templates {

    @CheckedTemplate(basePath = "index")
    public static class Index {
        public static native TemplateInstance home();
    }

    @CheckedTemplate(basePath = "claims", requireTypeSafeExpressions = false)
    public static class Claims {
        public static native TemplateInstance list(String title, List<Claim> claims, List<Service> services, long items,
                Map<String, Object> filter);

        public static native TemplateInstance table(List<Claim> claims, long items, Map<String, Object> filter);

        public static native TemplateInstance form(String title, Claim claim, List<Service> services,
                Map<String, Object> optional);
    }

    @CheckedTemplate(basePath = "services", requireTypeSafeExpressions = false)
    public static class Services {
        public static native TemplateInstance list(String title, List<Service> services, long items,
                Map<String, Object> filter);

        public static native TemplateInstance table(List<Service> services, long items, Map<String, Object> filter);

        public static native TemplateInstance item(String title, Service service);

        public static native TemplateInstance form(String title, Service service);

        public static native TemplateInstance listDiscovered(String title, List<ServiceDiscovered> services,
                long items);

        public static native TemplateInstance listDiscoveredTable(List<ServiceDiscovered> services, long items);
    }

    @CheckedTemplate(basePath = "credentials", requireTypeSafeExpressions = false)
    public static class Credentials {
        public static native TemplateInstance list(String title, List<Credential> credentials, long items,
                Map<String, Object> filter);

        public static native TemplateInstance table(List<Credential> credentials, long items,
                Map<String, Object> filter);

        public static native TemplateInstance item(String title, Credential credential);

        public static native TemplateInstance form(String title, Credential credential, List<Service> services);
    }

    @CheckedTemplate(basePath = "clusters")
    public static class Clusters {
        public static native TemplateInstance list(String title, List<Cluster> clusters, long items,
                Map<String, Object> filter);

        public static native TemplateInstance table(List<Cluster> clusters, long items, Map<String, Object> filter);

        public static native TemplateInstance item(String title, Cluster cluster);

        public static native TemplateInstance form(String title, Cluster cluster);
    }

    @CheckedTemplate(basePath = "applications", requireTypeSafeExpressions = false)
    public static class Applications {
        public static native TemplateInstance list(String title, List<Application> applications, long items,
                Map<String, Object> filter);

        public static native TemplateInstance table(List<Application> applications, long items,
                Map<String, Object> filter);

        public static native TemplateInstance bind(Application application, List<Claim> claims);
    }
}
