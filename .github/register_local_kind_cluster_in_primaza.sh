#!/usr/bin/env bash

# List of namespaces to be excluded during service and application and service discovery separated by commas
EXCLUDED_NAMESPACES=$1
PRIMAZA_KUBERNETES_NAMESPACE=sb
KIND_URL=https://kubernetes.default.svc
POD_NAME=$(kubectl get pod -l app.kubernetes.io/name=primaza-app -n $PRIMAZA_KUBERNETES_NAMESPACE -o name)
# To connect to Kind from outside a pod:
# KIND_URL=$(kubectl config view -o jsonpath='{"Cluster name\tServer\n"}{range .clusters[*]}{.name}{"\t"}{.cluster.server}{"\n"}{end}' | grep kind-kind | sed "s/kind-kind//" | xargs)
kind get kubeconfig > /tmp/local-kind-kubeconfig
kubectl cp /tmp/local-kind-kubeconfig sb/${POD_NAME:4}:/tmp/local-kind-kubeconfig -c primaza-app
RESULT=$(kubectl exec -i $POD_NAME --container primaza-app -n $PRIMAZA_KUBERNETES_NAMESPACE -- sh -c "curl -X POST -H 'Content-Type: multipart/form-data' -H 'HX-Request: true' -F name=local-kind -F excludedNamespaces=$EXCLUDED_NAMESPACES -F environment=DEV -F url=$KIND_URL -F kubeConfig=@/tmp/local-kind-kubeconfig -s -i localhost:8080/clusters")
if [[ "$RESULT" = *"500 Internal Server Error"* ]]
then
  echo "Cluster failed to be saved in Primaza: $RESULT"
  exit 1
fi
if [[ "$RESULT" = *"alert-danger"* ]]
then
  echo "Cluster failed to be saved in Primaza: $RESULT"
  exit 1
fi
echo "Cluster installed in Primaza: $RESULT"