#!/usr/bin/env bash

CREDENTIAL_NAME=$1
SERVICE_NAME=$2
VAULT_KV_PATH=$3


PRIMAZA_KUBERNETES_NAMESPACE=sb
POD_NAME=$(kubectl get pod -l app.kubernetes.io/name=primaza-app -n $PRIMAZA_KUBERNETES_NAMESPACE -o name)

SERVICE=$(kubectl exec -i $POD_NAME --container primaza-app -n $PRIMAZA_KUBERNETES_NAMESPACE -- sh -c "curl -H 'Accept: application/json' -s localhost:8080/services/name/$SERVICE_NAME")
SERVICE_ID=$(echo "$SERVICE" | jq -r '.id')

BODY="name=$CREDENTIAL_NAME&serviceId=$SERVICE_ID&vaultKvPath=$VAULT_KV_PATH"
echo "Sending service credential with body: $BODY"
RESULT=$(kubectl exec -i $POD_NAME --container primaza-app -n $PRIMAZA_KUBERNETES_NAMESPACE -- sh -c "curl -X POST -H 'Content-Type: application/x-www-form-urlencoded' -d '$BODY' -s -i localhost:8080/credentials")
if [[ "$RESULT" = *"500 Internal Server Error"* ]]
then
  echo "Credential failed to be saved in Primaza: $RESULT"
  exit 1
fi
if [[ "$RESULT" = *"alert-danger"* ]]
then
  echo "Credential failed to be saved in Primaza: $RESULT"
  exit 1
fi
echo "Credential installed in Primaza: $RESULT"