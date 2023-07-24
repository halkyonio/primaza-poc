#!/usr/bin/env bash

#
# Usage:
# ./scripts/data/cluster.sh
#
# To use a different context
# CONTEXT_TO_USE=my-ctx ./scripts/data/cluster.sh
#
# To create the cluster record on a Primaza server which is not localhost:8080
# PRIMAZA_URL=myprimaza:8080 ./scripts/data/cluster.sh
#

SCRIPTS_DIR="$(cd $(dirname "${BASH_SOURCE}") && pwd)"

source ${SCRIPTS_DIR}/../common.sh

# Parameters to play the script
export TYPE_SPEED=400
NO_WAIT=true

# Script parameters
NS_TO_BE_EXCLUDED=${NS_TO_BE_EXCLUDED:-default,kube-system,ingress,pipelines-as-code,local-path-storage,crossplane-system,primaza,tekton-pipelines,tekton-pipelines-resolvers,vault}
PRIMAZA_URL=${PRIMAZA_URL:-localhost:8080}
CONTEXT_TO_USE=${CONTEXT_TO_USE:-kind}
KIND_URL=${KIND_URL:-https://kubernetes.default.svc}

note "Primaza server: ${PRIMAZA_URL}"
note "Kubernetes API server: ${KIND_URL}"

note "Get the kubeconf and creating a primaza's cluster record"

#cmdExec "kind get kubeconfig --name ${CONTEXT_TO_USE} > local-kind-kubeconfig"
#cmdExec "k cp local-kind-kubeconfig ${NAMESPACE}/${POD_NAME:4}:/tmp/local-kind-kubeconfig -c primaza-app"
#CFG=$(kubectl config view --flatten --minify --context=${CONTEXT_TO_USE})
CFG=$(kind get kubeconfig --name ${CONTEXT_TO_USE})
ONE_LINE_CFG=$(echo "$CFG" | tr -d '\n')

note "Creating a Primaza DEV cluster for local kind usage ..."

cmdExec "curl -V -X POST -H 'Content-Type: multipart/form-data' \
  -F excludedNamespaces=${NS_TO_BE_EXCLUDED}\
  -F name=${CONTEXT_TO_USE}\
  -F environment=DEV\
  -F url=${KIND_URL}\
  -F kubeConfig=${ONE_LINE_CFG}\
  -s -i ${PRIMAZA_URL}/clusters"
