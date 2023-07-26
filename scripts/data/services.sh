#!/usr/bin/env bash

#
# Usage:
# ./scripts/data/services.sh
#
# To create the records on a different Primaza server which is not localhost:8080
# PRIMAZA_URL=myprimaza:8080 ./scripts/data/services.sh
#

SCRIPTS_DIR="$(cd $(dirname "${BASH_SOURCE}") && pwd)"
source ${SCRIPTS_DIR}/../common.sh

# Parameters to play the script
export TYPE_SPEED=400
NO_WAIT=true

# Script parameters
PRIMAZA_URL=${PRIMAZA_URL:-localhost:8080}
INSTALLABLE=${INSTALLABLE-off}

declare -a arr=(
  "name=postgresql&version=14.5&type=postgresql&endpoint=tcp:5432&installable=$INSTALLABLE&helmRepo=https://charts.bitnami.com/bitnami&helmChart=postgresql&helmChartVersion=11.9.13"
  "name=mysql&version=8.0&type=mysql&endpoint=tcp:3306"
  "name=activemq-artemis&version=2.26&type=activemq&endpoint=tcp:8161"
  "name=mariadb&version=10.9&type=mariadb&endpoint=tcp:3306"
)

for i in "${arr[@]}"
do
  note "curl -X POST ${PRIMAZA_URL}/services -s -k -d \"${i}\"" >&2
  RESPONSE=$(curl -s -k -o response.txt -w '%{http_code}'\
      -X POST \
      -d "${i}" \
      -i ${PRIMAZA_URL}/services)
  log_http_response "Service failed to be saved in Primaza: %s" "Service installed in Primaza: %s" $RESPONSE

  SERVICE=$(curl -H 'Accept: application/json' -s $PRIMAZA_URL:8080/services/name/$SERVICE_NAME)
  if [ $(echo "$SERVICE" | jq -r '.available') != "true" ]
  then
    error "Primaza was not able to discover the $SERVICE_NAME: $SERVICE"
    exit 1
  fi
done