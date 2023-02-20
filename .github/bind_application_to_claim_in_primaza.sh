#!/usr/bin/env bash

APPLICATION_NAME=$1
CLAIM_NAME=$2

PRIMAZA_KUBERNETES_NAMESPACE=sb
POD_NAME=$(kubectl get pod -l app.kubernetes.io/name=primaza-app -n $PRIMAZA_KUBERNETES_NAMESPACE -o name)

APPLICATION=$(kubectl exec -i $POD_NAME --container primaza-app -n $PRIMAZA_KUBERNETES_NAMESPACE -- sh -c "curl -H 'Accept: application/json' -s localhost:8080/applications/name/$APPLICATION_NAME")
APPLICATION_ID=$(echo "$APPLICATION" | jq -r '.id')
echo "Application ID to be bound $APPLICATION_ID"

CLAIM=$(kubectl exec -i $POD_NAME --container primaza-app -n $PRIMAZA_KUBERNETES_NAMESPACE -- sh -c "curl -H 'Accept: application/json' -s localhost:8080/claims/name/$CLAIM_NAME")
CLAIM_ID=$(echo "$CLAIM" | jq -r '.id')
echo "Claim ID to be bound $CLAIM_ID"

RESULT=$(kubectl exec -i $POD_NAME --container primaza-app -n $PRIMAZA_KUBERNETES_NAMESPACE -- sh -c "curl -X POST -H 'Content-Type: application/x-www-form-urlencoded' -H 'HX-Request: true' -d 'claimId=$CLAIM_ID' -s -i localhost:8080/applications/claim/$APPLICATION_ID")
if [[ "$RESULT" = *"500 Internal Server Error"* ]]
then
  echo "Application failed to be bound in Service Box: $RESULT"
  exit 1
fi
if [[ "$RESULT" = *"404 Not Found"* ]]
then
  echo "Application failed to be bound in Service Box: $RESULT"
  exit 1
fi
if [[ "$RESULT" = *"406 Not Acceptable"* ]]
then
  echo "Application failed to be bound in Service Box: $RESULT"
  exit 1
fi
if [[ "$RESULT" = *"alert-danger"* ]]
then
  echo "Application failed to be bound in Service Box: $RESULT"
  exit 1
fi
echo "Application bound in Service Box: $RESULT"