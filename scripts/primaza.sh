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
REGISTRY_GROUP=local
REGISTRY=kind-registry:5000
IMAGE_VERSION=latest
INGRESS_HOST=primaza.${VM_IP}.nip.io
PROJECT_NAME=primaza-poc
PROJECT_DIR=servicebox-app
GITHUB_REPO_PRIMAZA=https://github.com/halkyonio/primaza-poc.git

# Parameters to play the demo
TYPE_SPEED=${TYPE_SPEED:=40}
NO_WAIT=true

pushd ${PROJECT_DIR}

p "SCRIPTS_DIR dir: ${SCRIPTS_DIR}"
p "Ingress host is: ${INGRESS_HOST}"

pe "curl -s -L https://raw.githubusercontent.com/snowdrop/k8s-infra/main/kind/kind-reg-ingress.sh | bash -s y latest 0 ${VM_IP}"
pe "k wait -n ingress \
  --for=condition=ready pod \
  --selector=app.kubernetes.io/component=controller \
  --timeout=120s"

pe "k create namespace ${NAMESPACE}"
pe "k config set-context --current --namespace=${NAMESPACE}"

pe "mvn clean install -DskipTests -Ppush-images,kubernetes -Dquarkus.container-image.build=true \
   -Dquarkus.container-image.push=true \
   -Dquarkus.container-image.registry=${REGISTRY} \
   -Dquarkus.container-image.group=${REGISTRY_GROUP} \
   -Dquarkus.container-image.tag=${IMAGE_VERSION} \
   -Dquarkus.container-image.insecure=true \
   -Dquarkus.kubernetes.ingress.host=${INGRESS_HOST} \
   -Dlog.level=INFO \
   -Dgit.sha.commit=\"$(git rev-parse --short HEAD)\" \
   -Dgithub.repo=https://github.com/halkyonio/primaza-poc"

pe "kind load docker-image ${REGISTRY}/${REGISTRY_GROUP}/servicebox-app"

pe "helm install --devel servicebox-app \
  --dependency-update \
  ./target/helm/kubernetes/servicebox-app \
  -n ${NAMESPACE} \
  --set app.image=localhost:5000/${REGISTRY_GROUP}/servicebox-app:${IMAGE_VERSION} 2>&1 1>/dev/null"

pe "k wait -n ${NAMESPACE} \
  --for=condition=ready pod \
  -l app.kubernetes.io/name=servicebox-app \
  --timeout=7m"

p "waiting till Primaza Application is running"
POD_NAME=$(k get pod -l app.kubernetes.io/name=servicebox-app -n ${NAMESPACE} -o name)
while [[ $(k exec -i $POD_NAME -c servicebox-app -n ${NAMESPACE} -- bash -c "curl -s -o /dev/null -w ''%{http_code}'' localhost:8080/home") != "200" ]];
  do sleep 1
done

p "Get the kubeconf and creating a cluster"
KIND_URL=https://kubernetes.default.svc
pe "kind get kubeconfig > local-kind-kubeconfig"
pe "k cp local-kind-kubeconfig ${NAMESPACE}/${POD_NAME:4}:/tmp/local-kind-kubeconfig -c servicebox-app"

RESULT=$(k exec -i $POD_NAME -c servicebox-app -n ${NAMESPACE} -- sh -c "curl -X POST -H 'Content-Type: multipart/form-data' -H 'HX-Request: true' -F name=local-kind -F namespaces=default,ingress,kube-system,local-path-storage,primaza -F environment=DEV -F url=$KIND_URL -F kubeConfig=@/tmp/local-kind-kubeconfig -s -i localhost:8080/clusters")
if [ "$RESULT" = *"500 Internal Server Error"* ]
then
    p "Cluster failed to be saved in Service Box: $RESULT"
    k describe $POD_NAME -n ${NAMESPACE}
    k logs $POD_NAME -n ${NAMESPACE}
    exit 1
fi
note "Local k8s cluster registered: $RESULT"

popd