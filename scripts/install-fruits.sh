#!/usr/bin/env bash

SCRIPTS_DIR="$(cd $(dirname "${BASH_SOURCE}") && pwd)"

source ${SCRIPTS_DIR}/common.sh

####################################
## Variables
####################################
VM_IP=${VM_IP:=127.0.0.1}

FRUITS_CHART_NAME=fruits-app
HELM_APP_NAME=atomic-fruits

IMAGE=quay.io/halkyonio/atomic-fruits:latest
INGRESS_HOST=atomic-fruits.${VM_IP}.nip.io

NAMESPACE=${NAMESPACE:-app}

DB_USERNAME=${DB_USERNAME:-healthy}
DB_PASSWORD=${DB_PASSWORD:-healthy}
DB_DATABASE_NAME=${DB_DATABASE_NAME:-fruits_database}

# Parameters to play the demo
TYPE_SPEED=${TYPE_SPEED:=200}
NO_WAIT=true

if [[ "$1" == "clean" ]]; then
  pe "helm uninstall ${HELM_APP_NAME} -n ${NAMESPACE} || true"
  exit 0
fi

pe "k create ns $NAMESPACE --dry-run=client -o yaml | k apply -f -"
pe "k config set-context --current --namespace=${NAMESPACE}"

pe "helm upgrade -i ${HELM_APP_NAME} \
    ${FRUITS_CHART_NAME} \
    --repo http://halkyonio.github.io/helm-charts \
    -n ${NAMESPACE} \
    --set app.image=${IMAGE} \
    --set app.host=${INGRESS_HOST} \
    --set db.auth.database=${DB_DATABASE_NAME} \
    --set db.auth.username=${DB_USERNAME} \
    --set db.auth.password=${DB_PASSWORD} \
    --set app.serviceBinding.enabled=false"
