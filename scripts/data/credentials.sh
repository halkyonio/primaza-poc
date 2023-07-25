#!/usr/bin/env bash

#
# Usage:
# ./scripts/data/credentials.sh
#
# To create the records on a different Primaza server which is not localhost:8080
# PRIMAZA_URL=myprimaza:8080 ./scripts/data/credentials.sh
#

SCRIPTS_DIR="$(cd $(dirname "${BASH_SOURCE}") && pwd)"
source ${SCRIPTS_DIR}/../common.sh

# Parameters to play the script
export TYPE_SPEED=400
NO_WAIT=true

# Script parameters
PRIMAZA_URL=${PRIMAZA_URL:-localhost:8080}
SERVICE_ID=${SERVICE_ID:-1}

note "Primaza server: ${PRIMAZA_URL}"

declare -a arr=(
  "name=fruits_database-vault-creds&serviceId=$SERVICE_ID&vaultKvPath=primaza/fruits"
)

for i in "${arr[@]}"
do
  note "curl -X POST ${PRIMAZA_URL}/credentials -s -k -d \"${i}\"" >&2
  curl -X POST ${PRIMAZA_URL}/credentials -s -k -d "${i}" -o /dev/null
done