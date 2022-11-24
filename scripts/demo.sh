#!/usr/bin/env bash

SCRIPTS_DIR="$(cd $(dirname "${BASH_SOURCE}") && pwd)"

source ${SCRIPTS_DIR}/common.sh
source ${SCRIPTS_DIR}/play-demo.sh

####################################
## Variables
####################################
VM_IP=${VM_IP:=127.0.0.1}
NAMESPACE=app
REGISTRY_GROUP=local
REGISTRY=kind-registry:5000
IMAGE_VERSION=latest
INGRESS_HOST=superheroes.${VM_IP}.nip.io
QUARKUS_APP_PATH="$HOME/quarkus-super-heroes"
APP_DIR=rest-heroes
GITHUB_REPO=https://github.com/quarkusio/quarkus-super-heroes

# Parameters to play the demo
TYPE_SPEED=${TYPE_SPEED:=200}
NO_WAIT=true

if [[ ! -d "${QUARKUS_APP_PATH}" ]]; then
  git clone ${GITHUB_REPO} ${QUARKUS_APP_PATH}
fi

pushd ${QUARKUS_APP_PATH}/${APP_DIR}

pe "kubectl create ns $NAMESPACE --dry-run=client -o yaml | kubectl apply -f -"

pe "mvn quarkus:add-extension -Dextensions=\"quarkus-kubernetes-service-binding\""
p "Remove the third party installations via templates (we'll install these services using Service Box :) )"
pe "rm -rf src/main/kubernetes"
p "Remove the default application.yml as we'll provide a different one with our Helm properties"
pe "rm -rf src/main/resources/application.yml"
p "Copy the import.sql file"
pe "cp deploy/db-init/initialize-tables.sql src/main/resources/"

pe "cat > src/main/resources/application.properties << EOF
quarkus.application.name=rest-heroes
quarkus.http.port=8080
quarkus.hibernate-orm.sql-load-script=initialize-tables.sql
quarkus.hibernate-orm.database.generation=drop-and-create
quarkus.container-image.build=true
quarkus.container-image.builder=docker
quarkus.container-image.group=superhero
quarkus.container-image.tag=1.0
quarkus.kubernetes.deployment-target=kubernetes
EOF"

p "Package the application and build the image"
pe "mvn clean package -DskipTests \
  -Dquarkus.container-image.push=true \
  -Dquarkus.container-image.registry=$KIND_REGISTRY \
  -Dquarkus.kubernetes.namespace=$NAMESPACE \
  -Dquarkus.kubernetes.deploy=true"