#!/usr/bin/env bash

SCRIPTS_DIR="$(cd $(dirname "${BASH_SOURCE}") && pwd)"

source ${SCRIPTS_DIR}/common.sh
source ${SCRIPTS_DIR}/play-demo.sh

####################################
## Variables
####################################
VM_IP=${VM_IP:=127.0.0.1}
NAMESPACE=app
FRUITS_CHART_NAME=fruits-app
IMAGE=quay.io/halkyonio/atomic-fruits:latest
INGRESS_HOST=atomic-fruits.${VM_IP}.nip.io

# Parameters to play the demo
TYPE_SPEED=${TYPE_SPEED:=200}
NO_WAIT=true

if [[ "$1" == "clean" ]]; then
  pe "helm uninstall ${FRUITS_CHART_NAME} -n ${NAMESPACE} || true"
  exit 0
fi

pe "k create ns $NAMESPACE --dry-run=client -o yaml | k apply -f -"
pe "k config set-context --current --namespace=${NAMESPACE}"

pe "helm repo add halkyonio http://halkyonio.github.io/helm-charts"
pe "helm install ${FRUITS_CHART_NAME} halkyonio/fruits-app \
    -n ${NAMESPACE} \
    --set app.image=${IMAGE} \
    --set app.host=${INGRESS_HOST}"
