#!/usr/bin/env bash

SCRIPTS_DIR="$(cd $(dirname "${BASH_SOURCE}") && pwd)"

source ${SCRIPTS_DIR}/common.sh
source ${SCRIPTS_DIR}/play-demo.sh

####################################
## Variables
####################################
useConfirm=true
VM_IP=${VM_IP:=127.0.0.1}
NAMESPACE=primaza
PROJECT_DIR=app

CONTEXT_TO_USE=${CONTEXT_TO_USE:-kind}
GIT_SHA_COMMIT=${GIT_SHA_COMMIT:-$(git rev-parse --short HEAD)}

# Parameters to be used when we build and push to a local container registry
REGISTRY_GROUP=local
REGISTRY=kind-registry:5000
IMAGE_VERSION=latest
INGRESS_HOST=primaza.${VM_IP}.nip.io

# Parameters used when using the image from an external container registry: quay.io/halkyonio/primaza-app
# and helm chart published on: http://halkyonio.github.io/primaza-helm
PRIMAZA_GITHUB_REPO=https://github.com/halkyonio/primaza-poc
HALKYONIO_HELM_REPO=https://halkyonio.github.io/helm-charts/
PRIMAZA_IMAGE_NAME=${PRIMAZA_IMAGE_NAME:-quay.io/halkyonio/primaza-app:${GIT_SHA_COMMIT}}

# Parameters to play the demo
export TYPE_SPEED=400
NO_WAIT=true

p "SCRIPTS_DIR dir: ${SCRIPTS_DIR}"
p "Ingress host is: ${INGRESS_HOST}"

#########################
## Help / Usage
#########################
function usage() {
  fmt ""
  fmt "Usage: $0 [option]"
  fmt ""
  fmt "\tWhere option is:"
  fmt "\t-h            \tPrints help"
  fmt "\tremove        \tUninstall Primaza helm chart and additional kubernetes resources"
  fmt "\tbuild         \tBuild the Primaza quarkus application"
  fmt "\tdeploy        \tDeploy the primaza helm chart using Halkyon Helm repo"
  fmt "\tlocaldeploy   \tDeploy the primaza helm chart using local build application"
  fmt ""
}

function build() {
  pushd ${PROJECT_DIR}
  pe "mvn clean install -DskipTests -Dquarkus.container-image.build=true \
     -Dquarkus.container-image.push=true \
     -Dquarkus.container-image.registry=${REGISTRY} \
     -Dquarkus.container-image.group=${REGISTRY_GROUP} \
     -Dquarkus.container-image.tag=${IMAGE_VERSION} \
     -Dquarkus.container-image.insecure=true \
     -Dquarkus.kubernetes.ingress.host=${INGRESS_HOST} \
     -Dlog.level=INFO \
     -Dgit.sha.commit=${GIT_SHA_COMMIT} \
     -Dgithub.repo=${PRIMAZA_GITHUB_REPO}"

  pe "kind load docker-image ${REGISTRY}/${REGISTRY_GROUP}/primaza-app -n ${CONTEXT_TO_USE}"
  popd
}

function deploy() {
    ENVARGS=""
    if [[ -n "${VAULT_URL}" ]]; then ENVARGS+="--set app.envs.vault.url=${VAULT_URL}"; fi
    if [[ -n "${VAULT_USER}" ]]; then ENVARGS+="--set app.envs.vault.user=${VAULT_USER}"; fi
    if [[ -n "${VAULT_PASSWORD}" ]]; then ENVARGS+="--set app.envs.vault.password=${VAULT_PASSWORD}"; fi

    pe "k create namespace ${NAMESPACE} --dry-run=client -o yaml | kubectl apply -f -"
    pe "k config set-context --current --namespace=${NAMESPACE}"
    pe "helm install \
      --devel \
      --repo ${HALKYONIO_HELM_REPO} \
      primaza-app \
      primaza-app \
      -n ${NAMESPACE} \
      --set app.image=${PRIMAZA_IMAGE_NAME} \
      --set app.host=${INGRESS_HOST} \
      --set app.envs.git.sha.commit=${GIT_SHA_COMMIT} \
      --set app.envs.github.repo=${PRIMAZA_GITHUB_REPO} \
      ${ENVARGS} \
      2>&1 1>/dev/null"

    pe "k wait -n ${NAMESPACE} \
      --for=condition=ready pod \
      -l app.kubernetes.io/name=primaza-app \
      --timeout=7m"

    p "waiting till Primaza Application is running"
    POD_NAME=$(k get pod -l app.kubernetes.io/name=primaza-app -n ${NAMESPACE} -o name)
    while [[ $(k exec -i $POD_NAME -c primaza-app -n ${NAMESPACE} -- bash -c "curl -s -o /dev/null -w ''%{http_code}'' localhost:8080/home") != "200" ]];
      do sleep 1
    done

    p "Get the kubeconf and creating a cluster"
    KIND_URL=https://kubernetes.default.svc
    pe "kind get kubeconfig -n ${CONTEXT_TO_USE} > local-kind-kubeconfig"
    pe "k cp local-kind-kubeconfig ${NAMESPACE}/${POD_NAME:4}:/tmp/local-kind-kubeconfig -c primaza-app"

    NS_TO_BE_EXCLUDED=${NS_TO_BE_EXCLUDED:-default,kube-system,ingress,primaza,pipelines-as-code,tekton-pipelines,tekton-pipelines-resolvers,vault,local-path-storage,kube-node-lease}
    RESULT=$(k exec -i $POD_NAME -c primaza-app -n ${NAMESPACE} -- sh -c "curl -X POST -H 'Content-Type: multipart/form-data' -H 'HX-Request: true' -F name=local-kind -F excludedNamespaces=$NS_TO_BE_EXCLUDED -F environment=DEV -F url=$KIND_URL -F kubeConfig=@/tmp/local-kind-kubeconfig -s -i localhost:8080/clusters")
    if [ "$RESULT" = *"500 Internal Server Error"* ]
    then
        p "Cluster failed to be saved in Primaza: $RESULT"
        k describe $POD_NAME -n ${NAMESPACE}
        k logs $POD_NAME -n ${NAMESPACE}
        exit 1
    fi
    note "Local k8s cluster registered: $RESULT"
}

function localDeploy() {
    pe "k create namespace ${NAMESPACE} --dry-run=client -o yaml | kubectl apply -f -"
    pe "k config set-context --current --namespace=${NAMESPACE}"
    pe "helm install --devel primaza-app \
      --dependency-update \
      ${PROJECT_DIR}/target/helm/kubernetes/primaza-app \
      -n ${NAMESPACE} \
      --set app.image=${PRIMAZA_IMAGE_NAME}/primaza-app:${IMAGE_VERSION} 2>&1 1>/dev/null"

    pe "k wait -n ${NAMESPACE} \
      --for=condition=ready pod \
      -l app.kubernetes.io/name=primaza-app \
      --timeout=7m"

    p "waiting till Primaza Application is running"
    POD_NAME=$(k get pod -l app.kubernetes.io/name=primaza-app -n ${NAMESPACE} -o name)
    while [[ $(k exec -i $POD_NAME -c primaza-app -n ${NAMESPACE} -- bash -c "curl -s -o /dev/null -w ''%{http_code}'' localhost:8080/home") != "200" ]];
      do sleep 1
    done

    p "Get the kubeconf and creating a cluster"
    KIND_URL=https://kubernetes.default.svc
    pe "kind get kubeconfig -n ${CONTEXT_TO_USE} > local-kind-kubeconfig"
    pe "k cp local-kind-kubeconfig ${NAMESPACE}/${POD_NAME:4}:/tmp/local-kind-kubeconfig -c primaza-app"

    RESULT=$(k exec -i $POD_NAME -c primaza-app -n ${NAMESPACE} -- sh -c "curl -X POST -H 'Content-Type: multipart/form-data' -H 'HX-Request: true' -F name=local-kind -F excludedNamespaces=default,kube-system,ingress,pipelines-as-code,tekton-pipelines,tekton-pipelines-resolvers,vault -F environment=DEV -F url=$KIND_URL -F kubeConfig=@/tmp/local-kind-kubeconfig -s -i localhost:8080/clusters")
    if [ "$RESULT" = *"500 Internal Server Error"* ]
    then
        p "Cluster failed to be saved in Primaza: $RESULT"
        k describe $POD_NAME -n ${NAMESPACE}
        k logs $POD_NAME -n ${NAMESPACE}
        exit 1
    fi
    note "Local k8s cluster registered: $RESULT"
}

function remove() {
  pe "helm uninstall primaza-app -n ${NAMESPACE}" || true
}

case $1 in
    -h)           usage; exit;;
    build)        "$@"; exit;;
    deploy)       "$@"; exit;;
    localdeploy)  localDeploy; "$@"; exit;;
    remove)       "$@"; exit;;
    *)            usage; exit;;
esac

remove
deploy