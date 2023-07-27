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

# Default parameter values
DEFAULT_PRIMAZA_URL="localhost:8080"
DEFAULT_NS_TO_EXCLUDE="default,kube-system,ingress,pipelines-as-code,local-path-storage,crossplane-system,primaza,tekton-pipelines,tekton-pipelines-resolvers,vault"
DEFAULT_KUBE_CONTEXT="kind"
DEFAULT_KIND_URL="https://kubernetes.default.svc"
DEFAULT_ENVIRONMENT="DEV"

# Function to parse named parameters
parse_parameters() {
  for arg in "$@"; do
    case $arg in
      url=*)
        PRIMAZA_URL="${arg#*=}"
        ;;
      ns_to_exclude=*)
        NS_TO_EXCLUDE="${arg#*=}"
        ;;
      kube_context=*)
        KUBE_CONTEXT="${arg#*=}"
        ;;
      kind_url=*)
        KIND_URL="${arg#*=}"
        ;;
      environment=*)
        ENVIRONMENT="${arg#*=}"
        ;;
      *)
        # Handle any other unrecognized parameters
        echo "Unrecognized parameter: $arg"
        exit 1
        ;;
    esac
  done
}

# Parse the named parameters with defaults
parse_parameters "$@"

PRIMAZA_URL=${PRIMAZA_URL:-$DEFAULT_PRIMAZA_URL}
NS_TO_EXCLUDE=${NS_TO_EXCLUDE:-$DEFAULT_NS_TO_EXCLUDE}
KUBE_CONTEXT=${CONTEXT_TO_USE:-$DEFAULT_KUBE_CONTEXT}
KIND_URL=${KIND_URL:-$DEFAULT_KIND_URL}
ENVIRONMENT=${ENVIRONMENT:-$DEFAULT_ENVIRONMENT}

note "Getting the kube config using context name: $KUBE_CONTEXT"
CFG=$(kind get kubeconfig --name ${KUBE_CONTEXT})

note "curl -sS -o /dev/null -w '%{http_code}'\
        -X POST -H 'Content-Type: multipart/form-data' \
        -F excludedNamespaces=${NS_TO_EXCLUDE}\
        -F name=${KUBE_CONTEXT}\
        -F environment=DEV\
        -F url=${KIND_URL}\
        -F kubeConfig=${CFG}\
        -i ${PRIMAZA_URL}/clusters" >&2

RESPONSE=$(curl -s -k -o response.txt -w '%{http_code}'\
  -X POST -H 'Content-Type: multipart/form-data' \
  -F excludedNamespaces=${NS_TO_EXCLUDE}\
  -F name=${CONTEXT_TO_USE}\
  -F environment=DEV\
  -F url=${KIND_URL}\
  -F kubeConfig="${CFG}"\
  -i ${PRIMAZA_URL}/clusters)

log_http_response "Cluster failed to be saved in Primaza: %s" "Cluster installed in Primaza: %s" "$RESPONSE"

#POD_NAME=$(k get pod -l app.kubernetes.io/name=primaza-app -n ${PRIMAZA_NAMESPACE} -o name)
#k describe $POD_NAME -n ${PRIMAZA_NAMESPACE}
#k logs $POD_NAME -n ${PRIMAZA_NAMESPACE}