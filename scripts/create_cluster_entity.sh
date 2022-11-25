#!/usr/bin/env bash

SCRIPTS_DIR="$(cd $(dirname "${BASH_SOURCE}") && pwd)"

source ${SCRIPTS_DIR}/common.sh
source ${SCRIPTS_DIR}/play-demo.sh

# Parameters to play the script
TYPE_SPEED=200
NO_WAIT=true

# Script parameters
PRIMAZA_URL=${PRIMAZA_URL:-localhost:8080}
p "Primaza server: ${PRIMAZA_URL}"

CONTEXT_TO_USE=${CONTEXT_TO_USE:-kind-kind}

KIND_URL=$(kubectl config view -o json | jq -r --arg ctx ${CONTEXT_TO_USE} '.clusters[] | select(.name == $ctx) | .cluster.server')
p "Kind server: ${KIND_URL}"

CFG=$(kubectl config view --flatten --minify --context=${CONTEXT_TO_USE})
p "Creating a Primaza DEV cluster for local kind usage ..."
curl -X POST -H 'Content-Type: multipart/form-data' -F name=${CONTEXT_TO_USE} -F environment=DEV -F url=${KIND_URL} -F kubeConfig="${CFG}" -s -i ${PRIMAZA_URL}/clusters