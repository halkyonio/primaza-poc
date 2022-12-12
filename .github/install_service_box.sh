#!/usr/bin/env bash

GITHUB_REPOSITORY=${1:-https://github.com/halkyonio/primaza-poc}
GITHUB_COMMIT_SHA=${2:-Unknown}

KUBERNETES_NAMESPACE=sb
KIND_REGISTRY_GROUP=local
VERSION=latest
kubectl create namespace $KUBERNETES_NAMESPACE

mvn clean install -DskipTests -Ppush-images,kubernetes -Dquarkus.container-image.build=true \
  -Dquarkus.container-image.push=true \
  -Dquarkus.container-image.registry=$KIND_REGISTRY \
  -Dquarkus.container-image.group=$KIND_REGISTRY_GROUP \
  -Dquarkus.container-image.tag=$VERSION \
  -Dquarkus.container-image.insecure=true \
  -Dgithub.repo=$GITHUB_REPOSITORY \
  -Dgit.sha.commit=$GITHUB_COMMIT_SHA

# And install application from the Helm repository
helm install --dependency-update servicebox-app servicebox-app/target/helm/kubernetes/servicebox-app -n $KUBERNETES_NAMESPACE --set app.image=$KIND_REGISTRY/$KIND_REGISTRY_GROUP/servicebox-app:$VERSION
kubectl wait --for=condition=ready --timeout=5m pod -l app.kubernetes.io/name=servicebox-app -n $KUBERNETES_NAMESPACE

POD_NAME=$(kubectl get pod -l app.kubernetes.io/name=servicebox-app -n $KUBERNETES_NAMESPACE -o name)
RESULT=$(kubectl exec -i $POD_NAME --container servicebox-app -n $KUBERNETES_NAMESPACE -- sh -c "curl -s -i localhost:8080/home")
if [[ "$RESULT" = *"500 Internal Server Error"* ]]
then
  echo "Service Box is not working"
  exit 1
fi