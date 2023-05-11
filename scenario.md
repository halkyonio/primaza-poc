## Installation

Steps to execute to create a kind cluster + vault + crossplane locally

```bash
export VM_IP=127.0.0.1
export VAULT_URL=http://vault-internal.vault:8200
export PRIMAZA_URL=primaza.$VM_IP.nip.io
export PRIMAZA_IMAGE_NAME=kind-registry:5000/local/primaza-app

curl -s -L "https://raw.githubusercontent.com/snowdrop/k8s-infra/main/kind/kind.sh" | bash -s install --delete-kind-cluster
kubectl rollout status deployment/ingress-nginx-controller -n ingress

$(pwd)/scripts/vault.sh
$(pwd)/scripts/crossplane.sh

export VAULT_ADDR=http://vault.$VM_IP.nip.io
vault login -method=userpass username=bob password=sinclair
vault kv put -mount=secret primaza/fruits username=healthy password=healthy database=fruits_database
vault kv get -mount=secret primaza/fruits
```

## Running Primaza on k8s using local build
```bash
k create ns primaza --dry-run=client -o yaml | k apply -f -
$(pwd)/scripts/primaza.sh build
$(pwd)/scripts/primaza.sh localdeploy

export KIND_URL=https://kubernetes.default.svc
$(pwd)/scripts/data/services.sh
$(pwd)/scripts/data/credentials.sh
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

## Rollout
```bash
$(pwd)/scripts/primaza.sh build
k scale -n primaza --replicas=0 deployment/primaza-app; k scale -n primaza --replicas=1 deployment/primaza-app
kubectl rollout status deployment/primaza-app -n primaza

export KIND_URL=https://kubernetes.default.svc
$(pwd)/scripts/data/cluster.sh
$(pwd)/scripts/data/services.sh
$(pwd)/scripts/data/credentials.sh
```
## Test 1

DB installed using Crossplane Helm Release (= auto)

- Check the box `installable` for postgresql DB within the Service catalog
```bash
helm uninstall fruits-app -n app
helm install fruits-app halkyonio/fruits-app \
  -n app --create-namespace \
  --set app.image=quay.io/halkyonio/atomic-fruits:latest \
  --set app.host=atomic-fruits.$VM_IP.nip.io \
  --set app.serviceBinding.enabled=false \
  --set db.enabled=false
```

## TEST 2

!! DB manually installed using Bitnami Helm chart

```bash
helm uninstall fruits-app -n app
helm install fruits-app halkyonio/fruits-app \
  -n app --create-namespace \
  --set app.image=quay.io/halkyonio/atomic-fruits:latest \
  --set app.host=atomic-fruits.$VM_IP.nip.io \
  --set app.serviceBinding.enabled=false \
  --set db.enabled=false

DB_PASSWORD=healthy
DB_DATABASE=fruits_database
RELEASE_NAME=postgresql
VERSION=11.9.13

helm uninstall postgresql -n db
kubectl delete pvc -lapp.kubernetes.io/name=$RELEASE_NAME -n db

helm install $RELEASE_NAME bitnami/postgresql \
  --version $VERSION \
  --set auth.username=$DB_USERNAME \
  --set auth.password=$DB_PASSWORD \
  --set auth.database=$DB_DATABASE \
  --create-namespace \
  -n db
```