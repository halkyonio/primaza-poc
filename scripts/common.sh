#!/usr/bin/env bash

set -e

shopt -s expand_aliases
alias k='kubectl'

# Defining some colors for output
NC='\033[0m' # No Color
COLOR_RESET="\033[0m" # Reset color
BLACK="\033[0;30m"
BLUE='\033[0;34m'
BROWN="\033[0;33m"
GREEN='\033[0;32m'
GREY="\033[0;90m"
CYAN='\033[0;36m'
MAGENTA='\033[0;35m'
RED='\033[0;31m'
PURPLE="\033[0;35m"
WHITE='\033[0;37m'
YELLOW='\033[0;33m'

shopt -s expand_aliases
alias k='kubectl'

####################################
## Section to declare the functions
####################################
repeat_char(){
  COLOR=${1}
	for i in {1..70}; do echo -ne "${!COLOR}$2${NC}"; done
}

fmt() {
  COLOR="CYAN"
  MSG="${@:1}"
  echo -e "${!COLOR} ${MSG}${NC}"
}

msg() {
  COLOR=${1}
  MSG="${@:2}"
  echo -e "\n${!COLOR}## ${MSG}${NC}"
}

succeeded() {
  echo -e "${GREEN}NOTE:${NC} $1"
}

note() {
  echo -e "${BLUE}NOTE:${NC} $1"
}

warn() {
  echo -e "${YELLOW}WARN:${NC} $1"
}

error() {
  echo -e "${RED}ERROR:${NC} $1"
}

log() {
  MSG="${@:2}"
  echo; repeat_char ${1} '#'; msg ${1} ${MSG}; repeat_char ${1} '#'; echo
}