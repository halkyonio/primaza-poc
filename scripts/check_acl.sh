#!/usr/bin/env bash

SCRIPTS_DIR="$(cd $(dirname "${BASH_SOURCE}") && pwd)"
source ${SCRIPTS_DIR}/common.sh

VM_IP=${VM_IP:=127.0.0.1}
ROOT_TOKEN=${ROOT_TOKEN:=TOTO}
POLICY_NAME=primaza-policy

#path "*" {
#  "capabilities"=["create", "read", "update", "delete", "list"]
#}

cat <<EOF > spi_policy.hcl
path "kv/primaza/*" {
  capabilities = ["create", "read", "update", "delete", "list"]
}
path "sys/policies/acl/*" {
  capabilities = ["read","list"]
}
EOF

export VAULT_ADDR=http://vault.${VM_IP}.nip.io
vault login $(jq -r ."root_token" ${ROOT_TOKEN}
vault policy delete ${POLICY_NAME} ; vault policy write ${POLICY_NAME} spi_policy.hcl
vault write auth/userpass/users/quark password=admin policies=${POLICY_NAME}

vault login -method=userpass username=quark password=admin
vault policy list
vault secrets list
vault policy read ${POLICY_NAME}