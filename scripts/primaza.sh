#!/usr/bin/env bash

source ./common.sh
source ./play-demo.sh

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
PROJECT_DIR=servicebox-app

p "Ingress host is: ${INGRESS_HOST}"

cd {PROJECT_DIR}

curl -s -L "https://raw.githubusercontent.com/snowdrop/k8s-infra/main/kind/kind-reg-ingress.sh" | bash -s y latest 0
k wait -n ingress \
  --for=condition=ready pod \
  --selector=app.kubernetes.io/component=controller \
  --timeout=120s

k create namespace $NAMESPACE
k config set-context --current --namespace=$NAMESPACE

mvn clean install -DskipTests -Ppush-images,kubernetes -Dquarkus.container-image.build=true \
   -Dquarkus.container-image.push=true \
   -Dquarkus.container-image.registry=${REGISTRY} \
   -Dquarkus.container-image.group=${REGISTRY_GROUP} \
   -Dquarkus.container-image.tag=${IMAGE_VERSION} \
   -Dquarkus.container-image.insecure=true \
   -Dquarkus.kubernetes.ingress.host=${INGRESS_HOST}

kind load docker-image ${REGISTRY}/${REGISTRY_GROUP}/servicebox-app

helm install --devel servicebox-app \
  --dependency-update \
  ./target/helm/kubernetes/servicebox-app \
  -n $NAMESPACE \
  --set app.image=localhost:5000/${REGISTRY_GROUP}/servicebox-app:$VERSION 2>&1 1>/dev/null

k wait -n $NAMESPACE \
  --for=condition=ready pod \
  -l app.kubernetes.io/name=servicebox-app \
  --timeout=7m

POD_NAME=$(k get pod -l app.kubernetes.io/name=servicebox-app -n $NAMESPACE -o name)
while [[ $(kubectl exec -i $POD_NAME -c servicebox-app -n $NAMESPACE -- bash -c "curl -s -o /dev/null -w ''%{http_code}'' localhost:8080/home") != "200" ]];
  do sleep 1
  echo "waiting till Primaza is running"
done

KIND_URL=https://kubernetes.default.svc
kind get kubeconfig > local-kind-kubeconfig
kubectl cp local-kind-kubeconfig sb/${POD_NAME:4}:/tmp/local-kind-kubeconfig -c servicebox-app

echo "END OF THE SCRIPT"

RESULT=$(kubectl exec -i $POD_NAME --container servicebox-app -n $NAMESPACE -- sh -c "curl -X POST -H 'Content-Type: multipart/form-data' -H 'HX-Request: true' -F name=local-kind -F environment=DEV -F url=$KIND_URL -F kubeConfig=@/tmp/local-kind-kubeconfig -s -i localhost:8080/clusters")
if [ "$RESULT" = *"500 Internal Server Error"* ]
then
    echo "Cluster failed to be saved in Service Box: $RESULT"
    kubectl describe $POD_NAME -n $NAMESPACE
    kubectl logs $POD_NAME -n $NAMESPACE
    exit 1
fi
echo "Local k8s cluster registered: $RESULT"