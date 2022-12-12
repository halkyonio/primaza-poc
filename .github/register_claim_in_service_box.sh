#!/usr/bin/env bash

CLAIM_NAME=$1
CLAIM_REQUESTED_SERVICE=$2
SERVICE_BOX_KUBERNETES_NAMESPACE=sb
POD_NAME=$(kubectl get pod -l app.kubernetes.io/name=servicebox-app -n $SERVICE_BOX_KUBERNETES_NAMESPACE -o name)

PARAMS="name=$CLAIM_NAME&serviceRequested=$CLAIM_REQUESTED_SERVICE"
RESULT=$(kubectl exec -i $POD_NAME --container servicebox-app -n $SERVICE_BOX_KUBERNETES_NAMESPACE -- sh -c "curl -X POST -H 'Content-Type: application/x-www-form-urlencoded' -H 'HX-Request: true' -d '$PARAMS' -s -i localhost:8080/claims")
if [[ "$RESULT" = *"500 Internal Server Error"* ]]
then
  echo "Claim failed to be saved in Service Box: $RESULT"
  exit 1
fi

if [[ "$RESULT" = *"alert-danger"* ]]
then
  echo "Claim failed to be saved in Service Box: $RESULT"
  exit 1
fi

echo "Claim installed in Service Box: $RESULT"