package io.halkyon;

import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Version;

@Version("v1alpha1")
@Group("halkyon.io")
public class Primaza extends CustomResource<PrimazaSpec, PrimazaStatus> implements Namespaced {}

