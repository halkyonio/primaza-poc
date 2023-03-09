#!/usr/bin/env bash

SCRIPTS_DIR="$(cd $(dirname "${BASH_SOURCE}") && pwd)"

source ${SCRIPTS_DIR}/common.sh

VM_IP=${VM_IP:-127.0.0.1}

function install() {
  echo "Installing Vault Helm"
  cat <<EOF > ./my-values.yml
server:
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
  ROOT_TOKEN=$(jq -r ".root_token" ./cluster-keys.json)
  vaultExec "vault login ${ROOT_TOKEN} > /dev/null"
}

function unseal() {
    kubectl -n vault exec vault-0 -- vault operator init \
        -key-shares=1 \
        -key-threshold=1 \
        -format=json > ./cluster-keys.json

    VAULT_UNSEAL_KEY=$(jq -r ".unseal_keys_b64[]" ./cluster-keys.json)
    kubectl -n vault exec vault-0 -- vault operator unseal $VAULT_UNSEAL_KEY
    echo "##############################################################"
    echo "Vault Root Token: $(jq -r ".root_token" ./cluster-keys.json)"
    echo "##############################################################"
}

function kv() {
  kubectl -n vault exec vault-0 -- vault secrets enable kv
}

case $1 in
    install) "$@"; exit;;
    remove) "$@"; exit;;
    unseal) "$@"; exit;;
    kv) "$@"; exit;;
    login) "$@"; exit;;
esac

install
sleep 30
unseal
login
kv