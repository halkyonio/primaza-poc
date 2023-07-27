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

# Default parameter values
DEFAULT_PRIMAZA_URL="localhost:8080"
DEFAULT_CLAIM_NAME="fruits-claim"
DEFAULT_CLAIM_DESCRIPTION="postgresql-fruits-db"
DEFAULT_CLAIM_REQUESTED_SERVICE="postgresql-14.5"

# Function to parse named parameters
parse_parameters() {
  for arg in "$@"; do
    case $arg in
      url=*)
        PRIMAZA_URL="${arg#*=}"
        ;;
      claim_name=*)
        CLAIM_NAME="${arg#*=}"
        ;;
      description=*)
        CLAIM_DESCRIPTION="${arg#*=}"
        ;;
      requested_service=*)
        CLAIM_REQUESTED_SERVICE="${arg#*=}"
        ;;
      *)
        # Handle any other unrecognized parameters
        echo "Unrecognized parameter: $arg"
        exit 1
        ;;
    esac
  done
}

# Parse the named parameters with defaults
parse_parameters "$@"

# Set defaults if parameters are not provided
PRIMAZA_URL=${PRIMAZA_URL:-DEFAULT_PRIMAZA_URL}
CLAIM_NAME=${CLAIM_NAME:-DEFAULT_CLAIM_NAME}
CLAIM_DESCRIPTION=${CLAIM_DESCRIPTION:-DEFAULT_CLAIM_DESCRIPTION}
CLAIM_REQUESTED_SERVICE=${CLAIM_REQUESTED_SERVICE:-DEFAULT_CLAIM_REQUESTED_SERVICE}

BODY="name=$CLAIM_NAME&description=$CLAIM_DESCRIPTION&serviceRequested=$CLAIM_REQUESTED_SERVICE"
note "Creating the claim using as body: $BODY"
note "curl -X POST -s -i -k -d \"${BODY}\" ${PRIMAZA_URL}/claims" >&2

RESPONSE=$(curl -s -k -o response.txt -w '%{http_code}'\
  -X POST \
  -d "${BODY}"\
  -i ${PRIMAZA_URL}/claims)
log_http_response "Claim failed to be saved in Primaza: %s" "Claim installed in Primaza: %s" "$RESPONSE"