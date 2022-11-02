package io.halkyon;

import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;

import java.util.List;

@CheckedTemplate
public class Templates {
        public static native TemplateInstance claimList(List<io.halkyon.model.Claim> claims);
        public static native TemplateInstance claimForm();
        public static native TemplateInstance home();

        public static native TemplateInstance serviceList(List<io.halkyon.model.Service> services);
        public static native TemplateInstance serviceItem(io.halkyon.model.Service service);
        public static native TemplateInstance serviceForm();

        public static native TemplateInstance clusterList(List<io.halkyon.model.Cluster> clusters);
        public static native TemplateInstance clusterItem(io.halkyon.model.Cluster cluster);
        public static native TemplateInstance clusterForm();
}
