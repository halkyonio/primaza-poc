#!/usr/bin/env bash

SCRIPTS_DIR="$(cd $(dirname "${BASH_SOURCE}") && pwd)"

source ${SCRIPTS_DIR}/common.sh
source ${SCRIPTS_DIR}/play-demo.sh

# Parameters to play the demo
export TYPE_SPEED=400
NO_WAIT=true

function help() {
  fmt ""
  fmt "Usage: $0 [option]"
  fmt ""
  fmt "Utility script to install Crossplane"
  fmt ""
  fmt "\tWhere option is:"
  fmt "\t-h                         \tPrints help"
  fmt "\tdeploy                     \tInstall the crossplane helm chart and RBAC"
  fmt "\tremove                     \tRemove the crossplane helm chart"
  fmt "\thelm-provider              \tDeploy the crossplane Helm provider and configure it"
}

function deploy() {
  helm upgrade -i crossplane \
    crossplane \
    -n crossplane-system \
    --create-namespace \
    --repo https://charts.crossplane.io/stable
  kubectl rollout status deployment/crossplane -n crossplane-system
}

function helmProvider() {
  p "Installing the Helm provider ..."
  cat <<EOF | kubectl apply -f -
apiVersion: pkg.crossplane.io/v1
kind: Provider
metadata:
  name: helm-provider
spec:
  package: crossplanecontrib/helm-provider:master
EOF

  p "Configure the Crossplane Helm Provider"
  cat <<EOF | kubectl apply -f -
apiVersion: helm.crossplane.io/v1beta1
kind: ProviderConfig
metadata:
  name: helm-provider
spec:
  credentials:
    source: InjectedIdentity
EOF

  p "Wait till the helm provider is healthy"
  kubectl wait providerrevision -lpkg.crossplane.io/package=provider-helm --for condition=Healthy=true

  pe "kubectl rollout status deployment/crossplane -n crossplane-system"
  p "Give more RBAC rights to the crossplane service account"
  SA=$(kubectl -n crossplane-system get sa -o name | grep provider-helm | sed -e 's|serviceaccount\/|crossplane-system:|g')
  echo ${SA}

  kubectl create clusterrolebinding helm-provider-admin-binding --clusterrole cluster-admin --serviceaccount=${SA}
}

function remove() {
  kubectl delete provider/helm-provider || true
  kubectl delete providerconfig/helm-provider || true
  kubectl delete clusterrolebinding/helm-provider-admin-binding || true
  helm uninstall crossplane -n crossplane-system
}

case $1 in
    deploy)       "$@"; exit;;
    helm-provider) helmProvider; exit;;
    remove)       "$@"; exit;;
    *) help; exit;;
esac

remove
deploy