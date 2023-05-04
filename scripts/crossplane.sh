#!/usr/bin/env bash

SCRIPTS_DIR="$(cd $(dirname "${BASH_SOURCE}") && pwd)"

source ${SCRIPTS_DIR}/common.sh
source ${SCRIPTS_DIR}/play-demo.sh

# Parameters to play the demo
export TYPE_SPEED=400
NO_WAIT=true

function usage() {
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
    p "Set the debug arg"
    cat <<EOF | kubectl apply -f -
apiVersion: pkg.crossplane.io/v1alpha1
kind: ControllerConfig
metadata:
  name: debug-config
spec:
  args:
    - --debug
EOF

  p "Installing the Helm provider ..."
  cat <<EOF | kubectl apply -f -
apiVersion: pkg.crossplane.io/v1
kind: Provider
metadata:
  name: helm-provider
spec:
  package: crossplanecontrib/provider-helm:v0.14.0
  controllerConfigRef:
      name: debug-config
EOF

  pe "kubectl wait provider.pkg.crossplane.io/helm-provider --for condition=Healthy=true --timeout=300s"

  pe "kubectl rollout status deployment/crossplane -n crossplane-system"
  p "Give more RBAC rights to the crossplane service account"
  SA=$(kubectl -n crossplane-system get sa -o name | grep helm-provider | sed -e 's|serviceaccount\/|crossplane-system:|g')
  echo ${SA}

  kubectl create clusterrolebinding helm-provider-admin-binding --clusterrole cluster-admin --serviceaccount=${SA}

  pe "kubectl wait providerrevision -lpkg.crossplane.io/package=helm-provider --for condition=Healthy=true --timeout=300s"

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
}

function remove() {
  kubectl delete statefulset/postgresql -n db || true
  kubectl delete providerconfig/helm-provider || true
  kubectl delete provider/helm-provider || true
  kubectl delete clusterrolebinding/helm-provider-admin-binding || true
  helm uninstall crossplane -n crossplane-system
}

case $1 in
    -h)           usage; exit;;
    deploy)       "$@"; exit;;
    helm-provider) helmProvider; exit;;
    remove)       "$@"; exit;;
esac

deploy
helmProvider