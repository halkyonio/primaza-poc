package io.halkyon;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.Reconciler;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;

public class ClaimReconciler implements Reconciler<Claim> {
    private final KubernetesClient client;

    public ClaimReconciler(KubernetesClient client) {
        this.client = client;
    }

    // TODO Fill in the rest of the reconciler

    @Override
    public UpdateControl<Claim> reconcile(Claim resource, Context context) {
        // TODO: fill in logic

        return UpdateControl.noUpdate();
    }
}
