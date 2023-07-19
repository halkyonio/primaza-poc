#!/usr/bin/env bash

SCRIPTS_DIR="$(cd $(dirname "${BASH_SOURCE}") && pwd)"
source ${SCRIPTS_DIR}/common.sh

####################
## Global parameters
#####################
VM_IP=${VM_IP:-127.0.0.1}

KV_APP_NAME=${KV_APP_NAME:-primaza}
KV1_PREFIX=${KV1_PREFIX:-kv1}
KV2_PREFIX=${KV2_PREFIX:-secret}

VAULT_NAMESPACE=${VAULT_NAMESPACE:-vault}
VAULT_USER=${VAULT_USER:-bob}
VAULT_PASSWORD=${VAULT_PASSWORD:-sinclair}
VAULT_POLICY_NAME=kv-${KV_APP_NAME}-policy

TMP_DIR=.vault
mkdir -p ${TMP_DIR}

#########################
## Help / Usage
#########################
function usage() {
  fmt ""
  fmt "Usage: $0 [option]"
  fmt ""
  fmt "\tWhere option is:"
  fmt "\t-h           \tPrints help"
  fmt "\tremove       \tUninstall the helm chart and additional kubernetes resources"
  fmt "\tlogin        \tLog in to vault using the root token"
  fmt "\tloginAsUser  \tLog in to vault using parameters: <user> <password>. Default: bob/sinclair"
  fmt "\trootToken    \tDisplay the vault root token"
  fmt "\tregisterUser \tRegister a new vault user and assign a policy using as parameters: <user> <password> <policy_name>"
  fmt "\tvaultExec    \tExecute a vault command within the vault pod"
  fmt ""
}

#########################
## Generic functions
#########################
function vaultExec() {
  COMMAND=${1}
  kubectl exec vault-0 -n ${VAULT_NAMESPACE} -- sh -c "${COMMAND}" 2> /dev/null
}

function install() {
  log BLUE "Installing Vault Helm"
  cat <<EOF > ${TMP_DIR}/my-values.yml
server:
  image:
    tag: 1.13.0
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
  helm install vault hashicorp/vault --create-namespace -n ${VAULT_NAMESPACE} -f ${TMP_DIR}/my-values.yml

  while [[ $(kubectl get pod/vault-0 -n vault -ojson | jq -r 'if .status.phase == "Running" then "true" else "false" end') != "true" ]]; do
     echo "Still waiting for vault pod to be ready"
     sleep 5
  done
}

#########################
## Script functions
#########################
function remove() {
  log BLUE "Removing helm vault & pvc"
  helm uninstall vault -n ${VAULT_NAMESPACE} || true
  kubectl delete -n ${VAULT_NAMESPACE} pvc -lapp.kubernetes.io/name=vault
  kubectl delete -n ${VAULT_NAMESPACE} secret tokens || true
  rm -rf ${TMP_DIR} || true
}

function login() {
  log BLUE "Logging in as Root"
  ROOT_TOKEN=$(jq -r ".root_token" ${TMP_DIR}/cluster-keys.json)
  vaultExec "vault login ${ROOT_TOKEN}"
}

function rootToken() {
  log "YELLOW" "Vault root token: $(kubectl get secret -n ${VAULT_NAMESPACE} tokens -ojson | jq -r '.data.root_token' | base64 -d)"
}

function loginAsUser() {
    if [ -v 1 ]; then
      VAULT_USER=$1
    fi
    if [ -v 2 ]; then
      VAULT_PASSWORD=$2
    fi
  log BLUE "Login as user: ${VAULT_USER}"
  vaultExec "vault login -method=userpass username=${VAULT_USER} password=${VAULT_PASSWORD}"
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

function enableKV1SecretEngine() {
  log BLUE "Enable the KV version 1 secret engine. The secrets engine will be mounted to the path: ${KV1_PREFIX}"
  vaultExec "vault secrets enable -path=${KV1_PREFIX} -version=1 kv"
}

function enableKV2SecretEngine() {
  log BLUE "Enable KV version 2 secret engine. The secrets engine will be mounted to the path: ${KV2_PREFIX}"
  vaultExec "vault secrets enable -path=${KV2_PREFIX} -version=2 kv"
}

function enableK8sSecretEngine() {
  log BLUE "Enable Kubernetes secret engine"
  vaultExec "vault secrets enable kubernetes"
}

function enableUserPasswordAuth() {
  log BLUE "Enable User/password auth"
  vaultExec "vault auth enable userpass"
}

function createTokensKubernetesSecret() {
  log BLUE "Creating a kubernetes secret storing the Vault Root Token"
  kubectl create secret generic -n ${VAULT_NAMESPACE} tokens --from-literal=root_token=$(jq -r '.root_token' ${TMP_DIR}/cluster-keys.json)
}

function createUserPolicy() {
  ROLES="\"read\",\"create\",\"list\",\"delete\",\"update\""
  log BLUE "Creating policy ${VAULT_POLICY_NAME} for path: ${KV1_PREFIX}/${KV_APP_NAME}/* having as roles: ${ROLES}"

  POLICY_FILE=${TMP_DIR}/spi_policy.hcl

  cat <<EOF > $POLICY_FILE
#
# Rule to access kv keys (read, list) - version 1 or 2
# Example: vault kv list or vault kv read default
#
path "${KV1_PREFIX}/*" {
  "capabilities"=["read","list"]
}
path "${KV2_PREFIX}/*" {
  "capabilities"=["read","list"]
}
#
# Rule to access ${KV1_PREFIX}/${KV_APP_NAME} keys (CRUD) for version 1 or 2
# Example: vault kv read ${KV1_PREFIX}/${KV_APP_NAME}/hello
#
path "${KV1_PREFIX}/${KV_APP_NAME}/*" {
    "capabilities"=[${ROLES}]
}
path "${KV2_PREFIX}/data/${KV_APP_NAME}/*" {
    "capabilities"=[${ROLES}]
}
#
# Rule to access the secret engines (list)
# Example: vault secrets list
#
path "sys/secrets/*" {
  "capabilities"=["read","list"]
}
#
# Rule to access secrets engine mounted (read, list)
#
path "sys/mounts" {
  "capabilities"=["read","list"]
}
#
# Rule to access the ACL policies (read, list)
# Example: vault policy list or vault policy read default
#
path "sys/policies/acl/*" {
  "capabilities"=["read","list"]
}
EOF
  kubectl -n ${VAULT_NAMESPACE} cp ${POLICY_FILE} vault-0:/tmp/spi_policy.hcl
  vaultExec "vault policy write $VAULT_POLICY_NAME /tmp/spi_policy.hcl"
}

function registerUser() {
  if [ -v 1 ]; then
    VAULT_USER=$1
  fi
  if [ -v 2 ]; then
    VAULT_PASSWORD=$2
  fi
  if [ -v 3 ]; then
    VAULT_POLICY_NAME=$3
  fi

  log BLUE "Creating a new vault user: ${VAULT_USER}, password: ${VAULT_PASSWORD} having as policy: ${VAULT_POLICY_NAME}"
  vaultExec "vault write auth/userpass/users/${VAULT_USER} password=${VAULT_PASSWORD} policies=${VAULT_POLICY_NAME}"
}

function putHelloKey() {
  loginAsUser
  log BLUE "Executing: vault kv put -mount=${KV2_PREFIX} ${KV_APP_NAME}/hello target=world"
  vaultExec "vault kv put -mount=${KV2_PREFIX} ${KV_APP_NAME}/hello target=world"
}

function logRootToken() {
  log YELLOW "Vault temp folder containing the generated files: ${SCRIPTS_DIR}/../${TMP_DIR}"
  log YELLOW "Vault Root Token: $(jq -r ".root_token" ${TMP_DIR}/cluster-keys.json)"
  log YELLOW "Vault Root Token can be found from the kubernetes secret: \"kubectl get secret -n ${VAULT_NAMESPACE} tokens -ojson | jq -r '.data.root_token' | base64 -d\""
}

case $1 in
    -h) usage; exit;;
    install) "$@"; exit;;
    remove) "$@"; exit;;
    unseal) "$@"; exit;;
    enableKV1SecretEngine) "$@"; exit;;
    enableKV2SecretEngine) "$@"; exit;;
    enableK8sSecretEngine) "$@"; exit;;
    login) "$@"; exit;;
    rootToken) "$@"; exit;;
    enableUserPasswordAuth) "$@"; exit;;
    createUserPolicy) "$@"; exit;;
    registerUser) "$@"; exit;;
    loginAsUser) "$@"; exit;;
    vaultExec) "$@"; exit;;
    *)
      install
      unseal
      login
      enableKV2SecretEngine
      enableK8sSecretEngine
      enableUserPasswordAuth
      createTokensKubernetesSecret
      createUserPolicy
      registerUser
      logRootToken
      exit;;
esac
