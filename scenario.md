Table of Contents
=================

* [Prerequisites](#prerequisites)
* [Installation](#installation)
* [Running Primaza on k8s using local maven build](#running-primaza-on-k8s-using-local-maven-build)
* [Access the cluster using mvn quarkus:dev](#access-the-cluster-using-mvn-quarkusdev)
* [Watch primaza log](#watch-primaza-log)
* [Build and rollout](#build-and-rollout)
* [Test 1 :: DB installed using Crossplane Helm Release (= auto)](#test-1--db-installed-using-crossplane-helm-release--auto)
* [Test 2 :: DB manually installed using Bitnami Helm chart](#test-2--db-manually-installed-using-bitnami-helm-chart)

## Prerequisites

Add first the following helm repositories
```bash
helm repo add hashicorp https://helm.releases.hashicorp.com
helm repo add bitnami https://charts.bitnami.com/bitnami
helm repo add halkyonio http://halkyonio.github.io/helm-charts
```
Install the vault client using the [following](https://developer.hashicorp.com/vault/docs/install) instructions

## Installation

Steps to execute to create a kind cluster + vault + crossplane locally

```bash
export VM_IP=127.0.0.1 or export VM_IP=$(ifconfig eth0 | grep 'inet ' | cut -d: -f2 | awk '{ print $2}')
export VAULT_URL=http://vault-internal.vault:8200
export PRIMAZA_URL=primaza.$VM_IP.nip.io
export PRIMAZA_IMAGE_NAME=kind-registry:5000/local/primaza-app
export PRIMAZA_NAMESPACE=primaza

curl -s -L "https://raw.githubusercontent.com/snowdrop/k8s-infra/main/kind/kind.sh" | bash -s install --delete-kind-cluster
curl -s -L "https://raw.githubusercontent.com/snowdrop/k8s-infra/main/kind/registry.sh" | bash -s install --registry-name kind-registry
kubectl rollout status deployment/ingress-nginx-controller -n ingress

$(pwd)/scripts/vault.sh
$(pwd)/scripts/crossplane.sh

export VAULT_ADDR=http://vault.$VM_IP.nip.io
vault login -method=userpass username=bob password=sinclair
vault kv put -mount=secret primaza/fruits username=healthy password=healthy database=fruits_database
vault kv get -mount=secret primaza/fruits
```

## Running Primaza on k8s using local maven build
```bash
k create ns primaza --dry-run=client -o yaml | k apply -f -
$(pwd)/scripts/primaza.sh build
$(pwd)/scripts/primaza.sh localdeploy

export KIND_URL=https://kubernetes.default.svc

$(pwd)/scripts/data/cluster.sh url=$PRIMAZA_URL kube_context=kind kind_url="https://kubernetes.default.svc" environment=dev ns_to_exclude="default,kube-system,ingress,pipelines-as-code,local-path-storage,crossplane-system,primaza,tekton-pipelines,tekton-pipelines-resolvers,vault"

$(pwd)/scripts/data/services.sh url=$PRIMAZA_URL service_name=postgresql version=14.5 installable=on type=postgresql endpoint=tcp:5432 helm_repo="https://charts.bitnami.com/bitnami&helmChart=postgresql&helmChartVersion=11.9.13"
$(pwd)/scripts/data/services.sh url=$PRIMAZA_URL service_name=mysql version=8.0 type=mysql endpoint=tcp:3306
$(pwd)/scripts/data/services.sh url=$PRIMAZA_URL service_name=activemq-artemis version=2.26 type=activemq endpoint=tcp:8161
$(pwd)/scripts/data/services.sh url=$PRIMAZA_URL service_name=mariadb version=10.9 type=mariadb endpoint=tcp:3306

$(pwd)/scripts/data/credentials.sh url=$PRIMAZA_URL credential_type=vault credential_name=fruits_database-vault-creds service_name=postgresql vault_kv=primaza/fruits

# Install atomic fruits
$(pwd)/scripts/atomic-fruits.sh deploy

# Create the claim
$(pwd)/scripts/data/claims.sh url=$PRIMAZA_URL claim_name=fruits-claim description=postgresql-fruits-db requested_service=postgresql-14.5 application_id=1

# Do the binding
$(pwd)/scripts/data/bind_application.sh application_name=atomic-fruits claim_name=fruits-claim
```

## Access the cluster using mvn quarkus:dev
```bash
export PRIMAZA_URL=localhost:8080
export KIND_URL=$(kubectl config view -o json | jq -r --arg ctx kind-kind '.clusters[] | select(.name == $ctx) | .cluster.server')
$(pwd)/scripts/data/cluster.sh
$(pwd)/scripts/data/services.sh
$(pwd)/scripts/data/credentials.sh
```

## Watch primaza log 
```bash
POD=$(k get pods -n primaza -lapp.kubernetes.io/name=primaza-app -oname)
k -n primaza logs $POD -f
```

## Build and rollout
```bash
$(pwd)/scripts/primaza.sh build
k scale -n primaza --replicas=0 deployment/primaza-app; k scale -n primaza --replicas=1 deployment/primaza-app
kubectl rollout status deployment/primaza-app -n primaza

export KIND_URL=https://kubernetes.default.svc
$(pwd)/scripts/data/cluster.sh
$(pwd)/scripts/data/services.sh
$(pwd)/scripts/data/credentials.sh
```
## Test 1 :: DB installed using Crossplane Helm Release (= auto)

- Check the box `installable` for postgresql DB within the Service catalog
```bash
$(pwd)/scripts/atomic-fruits.sh remove
$(pwd)/scripts/atomic-fruits.sh deploy
```

## Test 2 :: DB manually installed using Bitnami Helm chart

```bash
$(pwd)/scripts/atomic-fruits.sh remove
$(pwd)/scripts/atomic-fruits.sh deploy

$(pwd)/scripts/atomic-fruits.sh removedb
$(pwd)/scripts/atomic-fruits.sh installdb
```