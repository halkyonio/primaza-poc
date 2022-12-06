#!/usr/bin/env bash

SCRIPTS_DIR="$(cd $(dirname "${BASH_SOURCE}") && pwd)"

source ${SCRIPTS_DIR}/common.sh
source ${SCRIPTS_DIR}/play-demo.sh

####################################
## Variables
####################################
VM_IP=${VM_IP:=127.0.0.1}
NAMESPACE=app
REGISTRY=localhost:5000
IMAGE_VERSION=latest
INGRESS_HOST=atomic-fruits.${VM_IP}.nip.io
QUARKUS_APP_PATH="$HOME/atomic-fruits-service"
GITHUB_REPO=https://github.com/aureamunoz/atomic-fruits-service

# Database credential
DB_NAMESPACE=db
USERNAME=healthy
PASSWORD=healthy
TYPE=postgresql
DATABASE_NAME=fruits-database

# Parameters to play the demo
TYPE_SPEED=${TYPE_SPEED:=200}
NO_WAIT=true

if [[ ! -d "${QUARKUS_APP_PATH}" ]]; then
  git clone ${GITHUB_REPO} ${QUARKUS_APP_PATH}
fi

if [[ "$1" == "clean" ]]; then
  pe "helm uninstall postgresql -n ${DB_NAMESPACE} || true"
  pe "k delete -f ${QUARKUS_APP_PATH}/target/kubernetes/kubernetes.yml -n ${NAMESPACE} || true"
  pe "k delete ns ${NAMESPACE} || true"
  pe "k delete ns ${DB_NAMESPACE} || true"
  exit 0
fi

pushd ${QUARKUS_APP_PATH}

pe "k create ns $NAMESPACE --dry-run=client -o yaml | k apply -f -"
pe "k config set-context --current --namespace=${NAMESPACE}"

pe "helm repo add bitnami https://charts.bitnami.com/bitnami"
pe "helm install postgresql bitnami/postgresql \
    --create-namespace -n ${DB_NAMESPACE} \
    --version 11.9.1 \
    --set auth.username=${USERNAME} \
    --set auth.password=${PASSWORD}"

p "Package the application and build the image"
pe "mvn clean package -DskipTests \
  -Dquarkus.container-image.push=true \
  -Dquarkus.container-image.registry=${REGISTRY} \
  -Dquarkus.container-image.insecure=true \
  -Dquarkus.kubernetes.namespace=${NAMESPACE} \
  -Dquarkus.kubernetes.deploy=true \
  -Dquarkus.kubernetes.ingress.host=${INGRESS_HOST}"