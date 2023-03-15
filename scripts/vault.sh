#!/usr/bin/env bash

SCRIPTS_DIR="$(cd $(dirname "${BASH_SOURCE}") && pwd)"
source ${SCRIPTS_DIR}/common.sh

####################
## Global parameters
#####################
VM_IP=${VM_IP:-127.0.0.1}

VAULT_USER=${VAULT_USER:-bob}
VAULT_PASSWORD=${VAULT_PASSWORD:-sinclair}
APP_POLICY=${APP_POLICY:-primaza}
KV_PREFIX=${KV_PREFIX:-kv}

TMP_DIR=$(mktemp -d 2>/dev/null || mktemp -d -t 'mytmpdir')

#########################
## Generic functions
#########################
function vaultExec() {
  COMMAND=${1}
  kubectl exec vault-0 -n vault -- sh -c "${COMMAND}" 2> /dev/null
}

function install() {
  log BLUE "Installing Vault Helm"
  cat <<EOF > ${TMP_DIR}/my-values.yml
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
  helm install vault hashicorp/vault --create-namespace -n vault -f ${TMP_DIR}/my-values.yml
}

#########################
## Script functions
#########################
function remove() {
  log BLUE "Removing helm vault & pvc"
  helm uninstall vault -n vault
  kubectl delete pvc -n vault -lapp.kubernetes.io/name=vault
  rm -rf ${TMP_DIR} || true
}

function login() {
  log BLUE "Logging in as Root"
  ROOT_TOKEN=$(jq -r ".root_token" ${TMP_DIR}/cluster-keys.json)
  vaultExec "vault login ${ROOT_TOKEN}"
}

function loginAsUser() {
  log BLUE "Login as user: ${VAULT_USER}"
  vaultExec "vault login -method=userpass username=bob password=sinclair"
}

function unseal() {
    log BLUE "Init vault and unseal"
    vaultExec "vault operator init \
        -key-shares=1 \
        -key-threshold=1 \
        -format=json" > ${TMP_DIR}/cluster-keys.json

    VAULT_UNSEAL_KEY=$(jq -r ".unseal_keys_b64[]" ${TMP_DIR}/cluster-keys.json)
    vaultExec "vault operator unseal $VAULT_UNSEAL_KEY"
}

function enableKVSecretEngine() {
  log BLUE "Enable KV secret engine"
  vaultExec "vault secrets enable kv"
}

function enableK8sSecretEngine() {
  log BLUE "Enable Kubernetes secret engine"
  vaultExec "vault secrets enable kubernetes"
}

function enableUserPasswordAuth() {
  log BLUE "Enable User/password auth"
  vaultExec "vault auth enable userpass"
}

function createUserPolicy() {
  POLICY_NAME=${KV_PREFIX}-${APP_POLICY}-policy
  ROLES="\"read\", \"create\", \"list\", \"delete\", \"update\""
  log BLUE "Creating policy ${POLICY_NAME} for path: ${KV_PREFIX}/${APP_POLICY}/* having as roles: ${ROLES}"

  POLICY_FILE=/tmp/spi_policy.hcl
  vaultExec "echo 'path \"${KV_PREFIX}/${APP_POLICY}/*\" { capabilities = [${ROLES}] }' > ${POLICY_FILE}"
  vaultExec "vault policy write $POLICY_NAME $POLICY_FILE"
}

function registerUser() {
  POLICY_NAME=${KV_PREFIX}-${APP_POLICY}-policy
  note "User: ${VAULT_USER}"
  note "Password: ${VAULT_PASSWORD}"
  note "Policy name: ${POLICY_NAME}"
  vaultExec "vault write auth/userpass/users/${VAULT_USER} password=${VAULT_PASSWORD} policies=${POLICY_NAME}"
}

case $1 in
    install) "$@"; exit;;
    remove) "$@"; exit;;
    unseal) "$@"; exit;;
    enableKVSecretEngine) "$@"; exit;;
    enableK8sSecretEngine) "$@"; exit;;
    login) "$@"; exit;;
    enableUserPasswordAuth) "$@"; exit;;
    createUserPolicy) "$@"; exit;;
    registerUser) "$@"; exit;;
    loginAsUser) "$@"; exit;;
esac

install
# DO NOT WORK -> kubectl rollout status statefulset/vault -n vault
sleep 20
unseal
login
enableKVSecretEngine
enableK8sSecretEngine
enableUserPasswordAuth
createUserPolicy
registerUser
loginAsUser
vaultExec "vault kv put kv/primaza/hello target=world"

log YELLOW "Temporary folder containing created files: ${TMP_DIR}"
log YELLOW "Vault Root Token: $(jq -r ".root_token" ${TMP_DIR}/cluster-keys.json)"