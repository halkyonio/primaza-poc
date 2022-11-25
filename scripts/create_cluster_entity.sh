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
CFG=$(kind get kubeconfig)
KIND_URL=$(kubectl config view -o json | jq -r '.clusters[0].cluster.server')
p "Kind server: ${KIND_URL}"

p "Creating a Primaza DEV cluster for local kind usage ..."
curl -X POST -H 'Content-Type: multipart/form-data' -F name=local-kind -F environment=DEV -F url=${KIND_URL} -F kubeConfig="${CFG}" -s -i ${PRIMAZA_URL}/clusters