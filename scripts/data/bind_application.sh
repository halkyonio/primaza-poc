#!/usr/bin/env bash

SCRIPTS_DIR="$(cd $(dirname "${BASH_SOURCE}") && pwd)"
source ${SCRIPTS_DIR}/../common.sh

# Parameters to play the script
export TYPE_SPEED=400
NO_WAIT=true

# Default parameter values
DEFAULT_APPLICATION_NAME=""
DEFAULT_CLAIM_NAME=""

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
      application_name=*)
        APPLICATION_NAME="${arg#*=}"
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

APPLICATION_NAME=${APPLICATION_NAME:-DEFAULT_APPLICATION_NAME}
CLAIM_NAME=${CLAIM_NAME:-DEFAULT_CLAIM_NAME}

note "Searching about the application to be bound ..."
note "curl -H 'Accept: application/json' -s $PRIMAZA_URL/applications/name/$APPLICATION_NAME"
APPLICATION=$(curl -H 'Accept: application/json' -s $PRIMAZA_URL/applications/name/$APPLICATION_NAME)
APPLICATION_ID=$(echo "$APPLICATION" | jq -r '.id')
note "Application ID to be bound: $APPLICATION_ID"

note "Searching about the claim ..."
CLAIM=$(curl -H 'Accept: application/json' -s $PRIMAZA_URL/claims/name/$CLAIM_NAME)
CLAIM_ID=$(echo "$CLAIM" | jq -r '.id')
note "Claim ID to be bound: $CLAIM_ID"

note "curl -X POST -H 'Content-Type: application/x-www-form-urlencoded' -d \"claimId=$CLAIM_ID\" -s -i $PRIMAZA_URL/applications/claim/$APPLICATION_ID"
RESULT=$(curl -s -k -o response.txt -w '%{http_code}'\
  -X POST \
  -H 'Content-Type: application/x-www-form-urlencoded'\
  -d "claimId=$CLAIM_ID"\
  -s -i $PRIMAZA_URL/applications/claim/$APPLICATION_ID)

log_http_response "Application failed to be bound in Primaza: %s" "Application bound in Primaza: %s" "$RESULT"