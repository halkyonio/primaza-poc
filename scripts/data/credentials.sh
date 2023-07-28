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

# Default parameter values
DEFAULT_PRIMAZA_URL="localhost:8080"

DEFAULT_CREDENTIAL_NAME=""
DEFAULT_SERVICE_NAME=""
DEFAULT_USERNAME=""
DEFAULT_PASSWORD=""
DEFAULT_DATABASE_NAME=""
DEFAULT_VAULT_KV=""

# Function to parse named parameters
parse_parameters() {
  for arg in "$@"; do
    case $arg in
      url=*)
        PRIMAZA_URL="${arg#*=}"
        ;;
      credential_name=*)
        CREDENTIAL_NAME="${arg#*=}"
        ;;
      service_name=*)
        SERVICE_NAME="${arg#*=}"
        ;;
      username=*)
        USERNAME="${arg#*=}"
        ;;
      password=*)
        PASSWORD="${arg#*=}"
        ;;
      database_name=*)
        DATABASENAME="${arg#*=}"
        ;;
      vault_kv=*)
        VAULT_KV="${arg#*=}"
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
PRIMAZA_URL=${PRIMAZA_URL:-$DEFAULT_PRIMAZA_URL}
CREDENTIAL_NAME=${CREDENTIAL_NAME:-$DEFAULT_CREDENTIAL_NAME}
SERVICE_ID=${SERVICE_ID:-$DEFAULT_SERVICE_ID}
SERVICE_NAME=${SERVICE_NAME:-$DEFAULT_SERVICE_NAME}
USERNAME=${USERNAME:-$DEFAULT_USERNAME}
PASSWORD=${PASSWORD:-$DEFAULT_PASSWORD}
DATABASENAME=${SERVICE_NAME:-$DEFAULT_DATABASE_NAME}
VAULT_KV=${VAULT_KV:-$DEFAULT_VAULT_KV}

note "Trying to find the service from the catalog ..."
note "curl -H 'Accept: application/json' -s $PRIMAZA_URL/services/name/$SERVICE_NAME"
RESPONSE=$(curl -H 'Accept: application/json' -s $PRIMAZA_URL/services/name/$SERVICE_NAME)
SERVICE_ID=$(echo "$RESPONSE" | jq -r '.id')

if [ -z "$SERVICE_ID" ]; then
  error "No service id found for service name: $SERVICE_NAME"
else
  note "Service id found: $SERVICE_ID"
fi

if [ -z "$USERNAME" ] && [ -z "$PASSWORD" ]; then
  BODY="name=$CREDENTIAL_NAME&serviceId=$SERVICE_ID&vaultKvPath=primaza/fruits"
else
  BODY="name=$CREDENTIAL_NAME&serviceId=$SERVICE_ID&username=$USERNAME&password=$PASSWORD&params=database:$DATABASE_NAME"
fi

note "Creating the credential using as body: $BODY"
note "curl -X POST -s -k -d \"${BODY}\" ${PRIMAZA_URL}/credentials" >&2

RESPONSE=$(curl -s -k -o response.txt -w '%{http_code}' \
  -X POST \
  -d "${BODY}" \
  -i ${PRIMAZA_URL}/credentials)
log_http_response "Credential failed to be saved in Primaza: %s" "Credential installed in Primaza: %s" $RESPONSE