#!/usr/bin/env bash

SCRIPTS_DIR="$(cd $(dirname "${BASH_SOURCE}") && pwd)"

source ${SCRIPTS_DIR}/common.sh

####################################
## Variables
####################################
VM_IP=${VM_IP:=127.0.0.1}

FRUITS_CHART_NAME=fruits-app
HELM_APP_NAME=atomic-fruits

IMAGE=quay.io/halkyonio/atomic-fruits:latest
INGRESS_HOST=atomic-fruits.${VM_IP}.nip.io

ATOMIC_FRUITS_NAMESPACE=${ATOMIC_FRUITS_NAMESPACE:-app}

DB_USERNAME=${DB_USERNAME:-healthy}
DB_PASSWORD=${DB_PASSWORD:-healthy}
DB_DATABASE=${DB_DATABASE:-fruits_database}
DB_RELEASE_NAME=postgresql
DB_VERSION=11.9.13

# Parameters to play the demo
export TYPE_SPEED=${TYPE_SPEED:=400}
NO_WAIT=true

function remove() {
  cmdExec "helm uninstall ${HELM_APP_NAME} -n ${ATOMIC_FRUITS_NAMESPACE}"
}

function deploy() {
  cmdExec "k create ns $ATOMIC_FRUITS_NAMESPACE --dry-run=client -o yaml | k apply -f -"
  cmdExec "k config set-context --current --namespace=${ATOMIC_FRUITS_NAMESPACE}"
  
  cmdExec "helm upgrade -i ${HELM_APP_NAME} \
      ${FRUITS_CHART_NAME} \
      --repo http://halkyonio.github.io/helm-charts \
      -n ${ATOMIC_FRUITS_NAMESPACE} \
      --set app.image=${IMAGE} \
      --set app.host=${INGRESS_HOST} \
      --set db.auth.database=${DB_DATABASE} \
      --set db.auth.username=${DB_USERNAME} \
      --set db.auth.password=${DB_PASSWORD} \
      --set db.enabled=false \
      --set app.serviceBinding.enabled=false"
}

function installdb() {
  cmdExec "helm repo add bitnami https://charts.bitnami.com/bitnami"
  
  cmdExec "helm install $DB_RELEASE_NAME bitnami/postgresql \
    --version $DB_VERSION \
    --set auth.username=$DB_USERNAME \
    --set auth.password=$DB_PASSWORD \
    --set auth.database=$DB_DATABASE \
    --create-namespace \
    -n ${ATOMIC_FRUITS_NAMESPACE}"
}

function removedb() {
  cmdExec "helm uninstall postgresql -n ${ATOMIC_FRUITS_NAMESPACE}"
  cmdExec "kubectl delete pvc -lapp.kubernetes.io/name=$DB_RELEASE_NAME -n ${ATOMIC_FRUITS_NAMESPACE}"
}

case $1 in
    -h)           usage; exit;;
    deploy)       "$@"; exit;;
    installdb)    "$@"; exit;;
    removedb)     removedb; exit;;
    remove)       "$@"; exit;;
    *)
      installdb
      deploy
      exit;;
esac