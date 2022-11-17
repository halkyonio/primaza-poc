#!/usr/bin/env bash

PROJECT_NAME=primaza-poc
rm -rf ${PROJECT_NAME}
wget https://github.com/halkyonio/primaza-poc/archive/refs/heads/main.zip
unzip main.zip
mv ${PROJECT_NAME}-main ${PROJECT_NAME}

cd ${PROJECT_NAME}
VM_IP=$1 ./scripts/primaza.sh