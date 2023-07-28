#!/usr/bin/env bash

SCRIPTS_DIR="$(cd $(dirname "${BASH_SOURCE}") && pwd)"

source ${SCRIPTS_DIR}/common.sh

####################################
## Variables
####################################
VM_IP=${VM_IP:=127.0.0.1}
PROJECT_DIR=app

CONTEXT_TO_USE=${CONTEXT_TO_USE:-kind}
GITHUB_SHA_COMMIT=${GITHUB_SHA_COMMIT:-$(git rev-parse --short HEAD)}

# Parameters to be used when we build and push to a local container registry
REGISTRY_GROUP=${REGISTRY_GROUP:-local}
REGISTRY=${KIND_REGISTRY:-kind-registry:5000}
IMAGE_TAG=${IMAGE_TAG:-latest}
#INGRESS_HOST=primaza.${VM_IP}.nip.io
PRIMAZA_URL=${PRIMAZA_URL:-primaza.$VM_IP.nip.io}

# Parameters used when using the image from an external container registry: quay.io/halkyonio/primaza-app
# and helm chart published on: http://halkyonio.github.io/primaza-helm
PRIMAZA_GITHUB_REPO=https://github.com/halkyonio/primaza-poc
PRIMAZA_NAMESPACE=${PRIMAZA_NAMESPACE:-primaza}
HALKYONIO_HELM_REPO=https://halkyonio.github.io/helm-charts/
PRIMAZA_IMAGE_NAME=${PRIMAZA_IMAGE_NAME:-quay.io/halkyonio/primaza-app:${GITHUB_SHA_COMMIT}}

NS_TO_BE_EXCLUDED=${NS_TO_BE_EXCLUDED:-default,kube-system,ingress,pipelines-as-code,local-path-storage,crossplane-system,primaza,tekton-pipelines,tekton-pipelines-resolvers,vault}

# Parameters to play the demo
export TYPE_SPEED=400
NO_WAIT=true

fmt "SCRIPTS_DIR dir: ${SCRIPTS_DIR}"
fmt "Ingress host is: ${PRIMAZA_URL}"

#########################
## Help / Usage
#########################
function primazaUsage() {
  fmt ""
  fmt "Usage: $0 [option]"
  fmt ""
  fmt "\tWhere option is:"
  fmt "\t-h            \tPrints help"
  fmt "\tremove        \tUninstall Primaza helm chart and additional kubernetes resources"
  fmt "\tbuild         \tBuild the Primaza quarkus application"
  fmt "\tdeploy        \tDeploy the primaza helm chart using Halkyon Helm repo"
  fmt "\tlocaldeploy   \tDeploy the primaza helm chart using local build application"
  fmt "\tlog           \tPrint the log of the primaza quarkus application pod like the information of the pod"
  fmt ""
}

function build() {
  cmdExec "mvn clean install -DskipTests -Dquarkus.container-image.build=true \
     -Dquarkus.container-image.push=true \
     -Dquarkus.container-image.registry=${REGISTRY} \
     -Dquarkus.container-image.group=${REGISTRY_GROUP} \
     -Dquarkus.container-image.tag=${IMAGE_TAG} \
     -Dquarkus.container-image.insecure=true \
     -Dquarkus.kubernetes.ingress.host=${PRIMAZA_URL} \
     -Dlog.level=INFO \
     -Dgit.sha.commit=${GITHUB_SHA_COMMIT} \
     -Dgithub.repo=${PRIMAZA_GITHUB_REPO}"
}

function deploy() {
    ENVARGS=""
    if [[ -n "${VAULT_URL}" ]]; then ENVARGS+="--set app.envs.vault.url=${VAULT_URL}"; fi
    if [[ -n "${VAULT_USER}" ]]; then ENVARGS+="--set app.envs.vault.user=${VAULT_USER}"; fi
    if [[ -n "${VAULT_PASSWORD}" ]]; then ENVARGS+="--set app.envs.vault.password=${VAULT_PASSWORD}"; fi

    cmdExec "k create namespace ${PRIMAZA_NAMESPACE} --dry-run=client -o yaml | kubectl apply -f -"
    cmdExec "k config set-context --current --namespace=${PRIMAZA_NAMESPACE}"
    cmdExec "helm install \
      --devel \
      --repo ${HALKYONIO_HELM_REPO} \
      primaza-app \
      primaza-app \
      -n ${PRIMAZA_NAMESPACE} \
      --set app.image=${PRIMAZA_IMAGE_NAME} \
      --set app.host=${PRIMAZA_URL} \
      --set app.envs.git.sha.commit=${GITHUB_SHA_COMMIT} \
      --set app.envs.github.repo=${PRIMAZA_GITHUB_REPO} \
      ${ENVARGS} \
      2>&1 1>/dev/null"

    cmdExec "k wait -n ${PRIMAZA_NAMESPACE} \
      --for=condition=ready pod \
      -l app.kubernetes.io/name=primaza-app \
      --timeout=7m"

    note "waiting till Primaza Application is running"
    POD_NAME=$(k get pod -l app.kubernetes.io/name=primaza-app -n ${PRIMAZA_NAMESPACE} -o name)
    while [[ $(k exec -i $POD_NAME -c primaza-app -n ${PRIMAZA_NAMESPACE} -- bash -c "curl -s -o /dev/null -w ''%{http_code}'' localhost:8080/home") != "200" ]];
      do sleep 1
    done

    note "Get the kubeconf and creating a cluster"
    KIND_URL=https://kubernetes.default.svc
    cmdExec "kind get kubeconfig --name ${CONTEXT_TO_USE} > local-kind-kubeconfig"
    cmdExec "k cp local-kind-kubeconfig ${PRIMAZA_NAMESPACE}/${POD_NAME:4}:/tmp/local-kind-kubeconfig -c primaza-app"

    RESULT=$(k exec -i $POD_NAME -c primaza-app -n ${PRIMAZA_NAMESPACE} -- sh -c "curl -X POST -H 'Content-Type: multipart/form-data' -F name=local-kind -F excludedNamespaces=$NS_TO_BE_EXCLUDED -F environment=DEV -F url=$KIND_URL -F kubeConfig=@/tmp/local-kind-kubeconfig -s -i localhost:8080/clusters")

    log_http_response "Cluster failed to be saved in Primaza: %s" "Local k8s cluster registered: %s" "$RESULT"
}

function localDeploy() {
    ENVARGS=""
    if [[ -n "${VAULT_URL}" ]]; then ENVARGS+="--set app.envs.vault.url=${VAULT_URL}"; fi
    if [[ -n "${VAULT_USER}" ]]; then ENVARGS+="--set app.envs.vault.user=${VAULT_USER}"; fi
    if [[ -n "${VAULT_PASSWORD}" ]]; then ENVARGS+="--set app.envs.vault.password=${VAULT_PASSWORD}"; fi

    cmdExec "k create namespace ${PRIMAZA_NAMESPACE} --dry-run=client -o yaml | kubectl apply -f -"
    cmdExec "k config set-context --current --namespace=${PRIMAZA_NAMESPACE}"
    cmdExec "helm install --devel primaza-app \
      --dependency-update \
      ${PROJECT_DIR}/target/helm/kubernetes/primaza-app \
      -n ${PRIMAZA_NAMESPACE} \
      --set app.image=${PRIMAZA_IMAGE_NAME} \
      ${ENVARGS} \
      2>&1 1>/dev/null"

    cmdExec "k wait -n ${PRIMAZA_NAMESPACE} \
      --for=condition=ready pod \
      -l app.kubernetes.io/name=primaza-app \
      --timeout=7m"

    note "waiting till Primaza Application is running"
    POD_NAME=$(k get pod -l app.kubernetes.io/name=primaza-app -n ${PRIMAZA_NAMESPACE} -o name)
    note "Primaza pod name: $POD_NAME"
    while [[ $(k exec -i $POD_NAME -c primaza-app -n ${PRIMAZA_NAMESPACE} -- bash -c "curl -s -o /dev/null -w ''%{http_code}'' localhost:8080/home") != "200" ]];
      do sleep 1
    done
    note "Primaza application is alive :-)"


}

function loadData() {
   note "Creating the cluster's record"
   ${SCRIPTS_DIR}/data/cluster.sh

   note "Creating the services's records"
   ${SCRIPTS_DIR}/data/services.sh

   note "Creating the credential record"
   ${SCRIPTS_DIR}/data/credentials.sh

   note "Creating the claim record"
   ${SCRIPTS_DIR}/data/claims.sh
}

function remove() {
  cmdExec "helm uninstall primaza-app -n ${PRIMAZA_NAMESPACE}"
}

function log() {
  POD_NAME=$(kubectl get pod -l app.kubernetes.io/name=primaza-app -n $PRIMAZA_NAMESPACE -o name)

  warn "Primaza application log ..."
  k logs $POD_NAME -n $PRIMAZA_NAMESPACE

  warn "Primaza pod information"
  k describe $POD_NAME -n $PRIMAZA_NAMESPACE
}

function isAlive() {
  max_retries=5
  retry_delay=5
  retry_attempt=1

  function healthStatus() {
    STATUS=$(curl -s $PRIMAZA_URL/q/health/live | jq -r .status)
    if [[ "$STATUS" = "UP" ]]; then
      return 0
    else
      return 1
    fi
  }

  while [ $retry_attempt -le $max_retries ]; do
    note "Attempt $retry_attempt of $max_retries"
    if healthStatus; then
      succeeded "Primaza is alive :-)"
      exit 0
    else
      warn "Primaza is not yet alive."
      sleep $retry_delay
    fi
    ((retry_attempt++))
  done
}

case $1 in
    -h)           primazaUsage; exit;;
    build)        "$@"; exit;;
    deploy)       "$@"; exit;;
    localdeploy)  localDeploy; exit;;
    loaddata)     loadData; exit;;
    isAlive)      isAlive; exit;;
    remove)       "$@"; exit;;
    log)          log; exit;;
    *)
      build
      localDeploy
      #loadData
      exit;;
esac