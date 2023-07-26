#!/usr/bin/env bash

#
# Usage:
# ./scripts/data/claims.sh
#
# To create the records on a different Primaza server which is not localhost:8080
# PRIMAZA_URL=myprimaza:8080 ./scripts/data/claims.sh
#

SCRIPTS_DIR="$(cd $(dirname "${BASH_SOURCE}") && pwd)"
source ${SCRIPTS_DIR}/../common.sh

# Parameters to play the script
export TYPE_SPEED=400
NO_WAIT=true

# Script parameters
PRIMAZA_URL=${PRIMAZA_URL:-localhost:8080}
SERVICE_ID=${SERVICE_ID:-1}

CLAIM_NAME=${CLAIM_NAME:-fruits-claim}
CLAIM_DESCRIPTION=${CLAIM_DESCRIPTION:-postgresql-fruits-db}
CLAIM_REQUESTED_SERVICE=${CLAIM_REQUESTED_SERVICE:-postgresql-14.5}
PARAMS="name=$CLAIM_NAME&description=$CLAIM_DESCRIPTION&serviceRequested=$CLAIM_REQUESTED_SERVICE"

note "curl -X POST ${PRIMAZA_URL}/claims -s -i -k -d \"${PARAMS}\""
RESULT=$(curl -X POST ${PRIMAZA_URL}/claims -s -i -k -d "${PARAMS}")
log_http_response "Claim failed to be saved in Primaza: %s" "Claim installed in Primaza: %s" "$RESULT"