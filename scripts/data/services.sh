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

# Default parameter values
DEFAULT_PRIMAZA_URL="localhost:8080"
DEFAULT_SERVICE_INSTALLABLE="off"
DEFAULT_SERVICE_NAME=""
DEFAULT_SERVICE_VERSION=""
DEFAULT_SERVICE_TYPE=""
DEFAULT_SERVICE_ENDPOINT=""
DEFAULT_HELM_REPO=""

# Function to parse named parameters
parse_parameters() {
  for arg in "$@"; do
    case $arg in
      url=*)
        PRIMAZA_URL="${arg#*=}"
        ;;
      installable=*)
        SERVICE_INSTALLABLE="${arg#*=}"
        ;;
      service_name=*)
        SERVICE_NAME="${arg#*=}"
        ;;
      version=*)
        SERVICE_VERSION="${arg#*=}"
        ;;
      type=*)
        SERVICE_TYPE="${arg#*=}"
        ;;
      endpoint=*)
        SERVICE_ENDPOINT="${arg#*=}"
        ;;
      helm_repo=*)
        HELM_REPO="${arg#*=}"
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
SERVICE_INSTALLABLE=${SERVICE_INSTALLABLE:-$DEFAULT_SERVICE_INSTALLABLE}
SERVICE_NAME=${SERVICE_NAME:-$DEFAULT_SERVICE_NAME}
SERVICE_VERSION=${SERVICE_VERSION:-$DEFAULT_SERVICE_VERSION}
SERVICE_TYPE=${SERVICE_TYPE:-$DEFAULT_SERVICE_TYPE}
SERVICE_ENDPOINT=${SERVICE_ENDPOINT:-$DEFAULT_SERVICE_ENDPOINT}
HELM_REPO=${HELM_REPO:-$DEFAULT_HELM_REPO}

BODY="name=$SERVICE_NAME&version=$SERVICE_VERSION&type=$SERVICE_TYPE&endpoint=$SERVICE_ENDPOINT&installable=$SERVICE_INSTALLABLE&helmRepo=$HELM_REPO"
note "Creating the service using as body: $BODY"
note "curl -X POST -s -k -d \"${BODY}\" ${PRIMAZA_URL}/services" >&2

RESPONSE=$(curl -s -k -o response.txt -w '%{http_code}'\
    -X POST \
    -d "${BODY}" \
    -i ${PRIMAZA_URL}/services)
log_http_response "Service failed to be saved in Primaza: %s" "Service installed in Primaza: %s" $RESPONSE

#SERVICE=$(curl -H 'Accept: application/json' -s $PRIMAZA_URL:8080/services/name/$SERVICE_NAME)
#if [ $(echo "$SERVICE" | jq -r '.available') != "true" ]
#then
#  error "Primaza was not able to discover the $SERVICE_NAME: $SERVICE"
#  exit 1
#fi
