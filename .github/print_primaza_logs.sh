#!/usr/bin/env bash

PRIMAZA_KUBERNETES_NAMESPACE=sb
POD_NAME=$(kubectl get pod -l app.kubernetes.io/name=primaza-app -n $PRIMAZA_KUBERNETES_NAMESPACE -o name)
kubectl logs $POD_NAME -n $PRIMAZA_KUBERNETES_NAMESPACE
kubectl describe $POD_NAME -n $PRIMAZA_KUBERNETES_NAMESPACE