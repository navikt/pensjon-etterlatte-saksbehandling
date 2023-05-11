#!/bin/bash

info() {
    echo -e "INFO: $1"
}

warn() {
    echo -e "\033[1;33mWARN:\033[0m $1"
}

error() {
    echo -e "\033[1;31mERROR:\033[0m $1"
    exit;
}

validateGitIgnore() {
  ROOT_DIR=$(pwd | sed "s/saksbehandling.*/saksbehandling/")
  CONTAINS_IGNORE_ENV=$(cat $ROOT_DIR/.gitignore | grep '.env')

  if [[ -z "$CONTAINS_IGNORE_ENV" ]]; then
    error ".gitignore does not contain entry for .env files!"
  fi
}

userIsAuthenticated() {
  nais device status | grep -q -i "naisdevice status: Disconnected" &> /dev/null
  if [ $? -eq 0 ]; then # Grep returns 0 if there is a match
    error "Naisdevice is not connected. Please run:\n\n\t$ nais device connect \n\nOr use the naisdevice.app"
  fi

  gcloud auth print-identity-token &> /dev/null
  if [ $? -gt 0 ]; then
    error "Not logged into gcloud... Please run:\n\n\t$ gcloud auth login"
  fi

  CURRENT_CONTEXT=$(kubectl config current-context)

  if [ "$CURRENT_CONTEXT" != "dev-gcp" ]; then
      error "Current context is $CURRENT_CONTEXT, but should be 'dev-gcp'"
  fi

  # Only continue if user is able to read secrets from gcp/kubernetes
  CAN_READ_SECRET=$(kubectl auth can-i get secret)

  if [ "$CAN_READ_SECRET" == "yes" ]; then
    info "User can read secrets in kubernetes. Continuing ..."
  else
    error "Not logged in or lacking rights. Ensure you're connected to naisdevice and gcp and correct default namespace is set."
  fi
}

validateAppDir() {
  if [[ -z "$1" ]]; then
    ALL_APPS_DIR=$(pwd | sed "s/saksbehandling.*/saksbehandling\/apps/")
    AVAILABLE_APPS=$(ls $ALL_APPS_DIR)

    error "You must supply app name. \n\nAvailable apps are: \n$AVAILABLE_APPS"
  fi

  APP_NAME=$1
  APP_DIR=$(pwd | sed "s/saksbehandling.*/saksbehandling\/apps\/$APP_NAME/")

  if [ ! -d "$APP_DIR" ]; then
    ALL_APPS_DIR=$(pwd | sed "s/saksbehandling.*/saksbehandling\/apps/")
    AVAILABLE_APPS=$(ls $ALL_APPS_DIR)

    error "Directory '$APP_DIR' does not exist! \n\nAvailable app dirs are: \n$AVAILABLE_APPS"
  else
    info "Found directory '$APP_DIR'."
  fi
}

userHasJq() {
  if ! command -v jq &> /dev/null
  then
    warn "Command 'jq' is missing. Attempting to run 'brew install jq'..."
    brew install jq

    if [ $? -eq 0 ]; then
      info "jq installed successfully"
    else
      error "Unable to install jq. Try installing manually"
    fi
  fi
}

getLocalSecretName() {
  info "Checking if app contains config for azuread secret"

  AZUREAD_FILE_PATH=$(find $APP_DIR -iname "azuread-etterlatte*")
  if [ -z "$AZUREAD_FILE_PATH" ]; then
    error "No azuread secret file found in dir '$APP_DIR'. Please ensure it follows this format:\n\n\t.nais/azuread-etterlatte-<APP_NAME>-lokal.yaml"
  fi

  AZURE_SECRET_NAME=$(grep "secretName" $AZUREAD_FILE_PATH | awk '{print $2}')

  if [ -z "$AZURE_SECRET_NAME" ]; then
    error "Field 'secretName' not found in file '$AZUREAD_FILE_PATH'"
  fi
}

getRemoteSecretName() {
  info "Checking kubernetes for secret for $APP_NAME"

  AZURE_SECRET_NAME=$(kubectl get secrets | grep "azure-$APP_NAME" -m1 | awk '{print $1}')
  if [ -z "$AZURE_SECRET_NAME" ]; then
    error "No azuread secret found for app name $APP_NAME"
  fi
}

getAzureadSecretName() {
  read -p "Do you want to use 'local' secret (default=remote)? [y/N] " USE_LOKAL_SECRET

  if [ "$USE_LOKAL_SECRET" == "y" ]; then
    getLocalSecretName
  else
    getRemoteSecretName
  fi

  info "Found secret with name '$AZURE_SECRET_NAME'"
}

addSecretToEnvFile() {
  userHasJq

  getAzureadSecretName

  echo "" # newline
  read -p "Is this a frontend app (does it use wonderwall)? [y/N] " USING_WONDERWALL

  info "Fetching secrets from kubernetes"

  if [ "$USING_WONDERWALL" == "y" ]; then
    kubectl -n etterlatte get secret $AZURE_SECRET_NAME -o json \
        | jq -r '.data | map_values(@base64d)
          | .["WONDERWALL_OPENID_CLIENT_ID"] = .AZURE_APP_CLIENT_ID
          | .["WONDERWALL_OPENID_CLIENT_JWK"] = .AZURE_APP_JWK
          | .["WONDERWALL_OPENID_WELL_KNOWN_URL"] = .AZURE_APP_WELL_KNOWN_URL
          | to_entries[] | (.key | ascii_upcase) +"=" + .value' \
        | sed -r "s/^(WONDERWALL_OPENID_CLIENT_JWK)=(\{.*\})$/\1='\2'/g" \
        > $APP_DIR/.env.dev-gcp
  else
    kubectl -n etterlatte get secret $AZURE_SECRET_NAME -o json \
        | jq -r '.data | map_values(@base64d) | to_entries[] | (.key | ascii_upcase) +"=" + .value' \
        > $APP_DIR/.env.dev-gcp
  fi

  info ".env.dev-gcp created with valid secrets"
}

info "==============="
info "Starting script ..."
info "==============="

validateGitIgnore

# Ensure user is authenticated
userIsAuthenticated

# Ensure directory is set and exists
validateAppDir $1

# Create .env-file containing secrets
addSecretToEnvFile
