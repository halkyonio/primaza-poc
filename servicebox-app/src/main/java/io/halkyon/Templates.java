package io.halkyon;

import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;

import java.util.List;

@CheckedTemplate
public class Templates {
        public static native TemplateInstance claimList(List<io.halkyon.model.Claim> claims);
        public static native TemplateInstance claimItem(io.halkyon.model.Claim claim);
        public static native TemplateInstance claimForm();
        public static native TemplateInstance home();
}