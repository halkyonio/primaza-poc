#!/usr/bin/env bash

SERVICE_NAME=$1
SERVICE_VERSION=$2
SERVICE_ENDPOINT=$3
SERVICE_TYPE=$4

PRIMAZA_KUBERNETES_NAMESPACE=sb
POD_NAME=$(kubectl get pod -l app.kubernetes.io/name=primaza-app -n $PRIMAZA_KUBERNETES_NAMESPACE -o name)

BODY="name=$SERVICE_NAME&version=$SERVICE_VERSION&endpoint=$SERVICE_ENDPOINT&type=$SERVICE_TYPE"
RESULT=$(kubectl exec -i $POD_NAME --container primaza-app -n $PRIMAZA_KUBERNETES_NAMESPACE -- sh -c "curl -X POST -H 'Content-Type: application/x-www-form-urlencoded' --data '$BODY' -s -i localhost:8080/services")
if [[ "$RESULT" = *"500 Internal Server Error"* ]]
then
  echo "Service failed to be saved in Primaza: $RESULT"
  exit 1
fi

echo "Service installed in Primaza: $RESULT"

SERVICE=$(kubectl exec -i $POD_NAME --container primaza-app -n $PRIMAZA_KUBERNETES_NAMESPACE -- sh -c "curl -H 'Accept: application/json' -s localhost:8080/services/name/$SERVICE_NAME")
if [ $(echo "$SERVICE" | jq -r '.available') != "true" ]
then
  echo "Primaza didn't discovery the $SERVICE_NAME service: $SERVICE"
  exit 1
fi