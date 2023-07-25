#!/usr/bin/env bash

SCRIPTS_DIR="$(cd $(dirname "${BASH_SOURCE}") && pwd)"
source ${SCRIPTS_DIR}/common.sh

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
  fmt "\tkube-provider              \tDeploy the crossplane Kubernetes provider and configure it"
}

function deploy() {
  helm repo add crossplane-stable https://charts.crossplane.io/stable
  helm repo update crossplane-stable
  helm install crossplane \
    -n crossplane-system \
    --create-namespace \
    crossplane-stable/crossplane
  kubectl rollout status deployment/crossplane -n crossplane-system

  note "Configure the ControllerConfig resource to set the debug arg"
  cat <<EOF | kubectl apply -f -
apiVersion: pkg.crossplane.io/v1alpha1
kind: ControllerConfig
metadata:
  name: debug-config
spec:
  args:
    - --debug
EOF
}

function kubernetesProvider(){
    note "Installing the kubernetes provider"
    cat <<EOF | kubectl apply -f -
apiVersion: pkg.crossplane.io/v1
kind: Provider
metadata:
  name: kubernetes-provider
spec:
  package: "crossplane/provider-kubernetes:main"
  controllerConfigRef:
      name: debug-config
EOF

    pe "kubectl wait provider.pkg.crossplane.io/kubernetes-provider --for condition=Healthy=true --timeout=300s"

    note "Give more RBAC rights to the crossplane service account"
    SA=$(kubectl -n crossplane-system get sa -o name | grep kubernetes-provider | sed -e 's|serviceaccount\/|crossplane-system:|g')
    kubectl create clusterrolebinding kubernetes-provider-admin-binding --clusterrole cluster-admin --serviceaccount=${SA}

  note "Deploy the Crossplane Kubernetes ProviderConfig"
  cat <<EOF | kubectl apply -f -
apiVersion: kubernetes.crossplane.io/v1alpha1
kind: ProviderConfig
metadata:
  name: kubernetes-provider
spec:
  credentials:
    source: InjectedIdentity
EOF
}

function helmProvider() {
  note "Installing the Helm provider ..."
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
  note "Give more RBAC rights to the crossplane service account"
  SA=$(kubectl -n crossplane-system get sa -o name | grep helm-provider | sed -e 's|serviceaccount\/|crossplane-system:|g')
  echo ${SA}

  kubectl create clusterrolebinding helm-provider-admin-binding --clusterrole cluster-admin --serviceaccount=${SA}

  pe "kubectl wait providerrevision -lpkg.crossplane.io/package=helm-provider --for condition=Healthy=true --timeout=300s"

  note "Configure the Crossplane Helm Provider"
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
  kubectl delete controllerconfig --all || true
  kubectl delete providerconfig --all || true
  kubectl delete providers --all || true
  kubectl delete clusterrolebinding/helm-provider-admin-binding || true
  kubectl delete clusterrolebinding/kubernetes-provider-admin-binding || true
  helm uninstall crossplane -n crossplane-system
}

case $1 in
    -h)           usage; exit;;
    deploy)       "$@"; exit;;
    helm-provider) helmProvider; exit;;
    kube-provider) kubernetesProvider; exit;;
    remove)       "$@"; exit;;
    *)
      deploy
      helmProvider
      kubernetesProvider
      exit;;
esac

