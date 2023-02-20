#!/usr/bin/env bash

SERVICE_BOX_KUBERNETES_NAMESPACE=sb
POD_NAME=$(kubectl get pod -l app.kubernetes.io/name=primaza-app -n $SERVICE_BOX_KUBERNETES_NAMESPACE -o name)
kubectl logs $POD_NAME -n $SERVICE_BOX_KUBERNETES_NAMESPACE
kubectl describe $POD_NAME -n $SERVICE_BOX_KUBERNETES_NAMESPACE