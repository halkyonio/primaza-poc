#!/usr/bin/env bash

# TODO: To be improved using: https://github.com/redhat-appstudio/service-provider-integration-operator/blob/main/hack/vault-init.sh

SCRIPTS_DIR="$(cd $(dirname "${BASH_SOURCE}") && pwd)"

source ${SCRIPTS_DIR}/common.sh

VM_IP=${VM_IP:-127.0.0.1}
USER=${0:-bob}
PASSWORD=${1:-sinclair}
PATH_PREFIX=${2:-kv/*}
APP=${2:-primaza}

echo "USER 1: ${USER}"
echo "PASSWORD 2: ${PASSWORD}"
echo "PATH_PREFIX 3: ${PATH_PREFIX}"
echo "APP 4: ${APP}"


function install() {
  echo "Installing Vault Helm"
  cat <<EOF > ./my-values.yml
server:
  updateStrategyType: RollingUpdate
  ha:
    enabled: false
  ingress:
    enabled: true
    ingressClassName: nginx
    hosts:
    - host: vault.${VM_IP}.nip.io
      paths: []
ui:
  enabled: true
  serviceType: "ClusterIP"
EOF
  helm install vault hashicorp/vault --create-namespace -n vault -f ./my-values.yml
}

function remove() {
  echo "Removing helm vault & pvc"
  helm uninstall vault -n vault
  kubectl delete pvc -n vault -lapp.kubernetes.io/name=vault
}

function vaultExec() {
  COMMAND=${1}
  kubectl exec vault-0 -n vault -- sh -c "${COMMAND}" 2> /dev/null
}

function login() {
  echo "Logging in as Root"
  ROOT_TOKEN=$(jq -r ".root_token" ./cluster-keys.json)
  echo "Root Token: ${ROOT_TOKEN}"
  vaultExec "vault login ${ROOT_TOKEN}"
}

function loginAsUser() {
  vaultExec "vault login -method=userpass username=bob password=sinclair"
}

function unseal() {
    vaultExec "vault operator init \
        -key-shares=1 \
        -key-threshold=1 \
        -format=json" > ./cluster-keys.json

    VAULT_UNSEAL_KEY=$(jq -r ".unseal_keys_b64[]" ./cluster-keys.json)
    vaultExec "vault operator unseal $VAULT_UNSEAL_KEY"
    echo "##############################################################"
    echo "Vault Root Token: $(jq -r ".root_token" ./cluster-keys.json)"
    echo "##############################################################"
}

function enableKvSecretEngine() {
  vaultExec "vault secrets enable kv"
}

function enableK8sSecretEngine() {
  vaultExec "vault secrets enable kubernetes"
}

function enableUserPasswordAuth() {
  vaultExec "vault auth enable userpass"
}

function registerUser() {
  vaultExec "vault write auth/userpass/users/${1} password=${2} policies=kv-${3}-policy"
}

function createUserPolicy() {
  echo "Creating User Policy"
  PATH_PREFIX=${1}
  APP=${2}
  echo "Creating kv-${APP}-policy User Policy for '${PATH_PREFIX}' path"
  cat <<EOF | vault policy write kv-$APP-policy -
path "${PATH_PREFIX}" {
  capabilities = ["read", "create"]
}
EOF

}

case $1 in
    install) "$@"; exit;;
    remove) "$@"; exit;;
    unseal) "$@"; exit;;
    kv) "$@"; exit;;
    login) "$@"; exit;;
    createUserPolicy) "$@"; exit;;
    enableUserPasswordAuth) "$@"; exit;;
    registerUser) "$@"; exit;;
    loginAsUser) "$@"; exit;;
esac

install
# DO NOT WORK -> kubectl rollout status statefulset/vault -n vault
sleep 20
unseal
login
enableKvSecretEngine
enableK8sSecretEngine
createUserPolicy ${PATH_PREFIX} ${APP}
enableUserPasswordAuth
registerUser ${USER} ${PASSWORD} kv-${APP}-policy
loginAsUser