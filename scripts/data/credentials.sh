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
source ${SCRIPTS_DIR}/../play-demo.sh

# Parameters to play the script
TYPE_SPEED=200
NO_WAIT=true

# Script parameters
PRIMAZA_URL=${PRIMAZA_URL:-localhost:8080}
p "Primaza server: ${PRIMAZA_URL}"

declare -a arr=(
  "serviceId=1&name=user&username=healthy&password=healthy&params="
  "serviceId=3&name=admin&username=supermario&password=supermario&params="
)

for i in "${arr[@]}"
do
  pe "curl -X POST ${PRIMAZA_URL}/credentials -s -k -d \"${i}\" -o /dev/null"
done