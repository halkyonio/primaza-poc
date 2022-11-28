#!/usr/bin/env bash

#
# Usage:
# ./scripts/data/services.sh
#
# To use a different context
# CONTEXT_TO_USE=my-ctx ./scripts/data/services.sh
#
# To create the cluster record on a Primaza server which is not localhost:8080
# PRIMAZA_URL=myprimaza:8080 ./scripts/data/services.sh
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
  "name=mysql&version=8.0.&endpoint=tcp:3306"
  "name=postgresql&version=11.5&endpoint=tcp:5432"
  "name=postgresql&version=13.5&endpoint=tcp:5432"
  "name=activemq-artemis&version=2.26&endpoint=tcp:8161"
  "name=mariadb&version=10.9&endpoint=tcp:3306"
)

for i in "${arr[@]}"
do
  #pe "wget --no-check-certificate -q ${PRIMAZA_URL}/services --post-data=\"${i}\" "
  pe "curl -X POST ${PRIMAZA_URL}/services -s -k -d \"${i}\" -o /dev/null"
done