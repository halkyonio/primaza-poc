package io.halkyon;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.Reconciler;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;

public class PrimazaReconciler implements Reconciler<Primaza> { 
  private final KubernetesClient client;

  public PrimazaReconciler(KubernetesClient client) {
    this.client = client;
  }

  // TODO Fill in the rest of the reconciler

  @Override
  public UpdateControl<Primaza> reconcile(Primaza resource, Context context) {
    // TODO: fill in logic

    return UpdateControl.noUpdate();
  }
}

