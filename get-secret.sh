#!/bin/bash

info() {
    echo -e "\033[1;32mINFO:\033[0m\t$1"
}

warn() {
    echo -e "\033[1;33mWARN:\033[0m\t$1"
}

error() {
    echo -e "\033[1;31mERROR:\033[0m\t$1"
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
  ALL_APPS_DIR=$(pwd | sed "s/saksbehandling.*/saksbehandling\/apps/")
  APP_NAME=$(ls $ALL_APPS_DIR | fzf)

  if [ -n "$APP_NAME" ]; then
    info "You selected '$APP_NAME'"

    APP_DIR=$(pwd | sed "s/saksbehandling.*/saksbehandling\/apps\/$APP_NAME/")
  else
    warn "No app selected. Exiting script ..."
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

userHasFzf() {
  if ! command -v fzf &> /dev/null
  then
    warn "Command 'fzf' is missing. Attempting to run 'brew install fzf'..."
    brew install fzf

    if [ $? -eq 0 ]; then
      info "fzf installed successfully"
    else
      error "Unable to install fzf. Try installing manually"
    fi
  fi
}

getLocalSecretName() {
  AZUREAD_FILE_PATH=$(find $APP_DIR -iname "azuread-etterlatte*")
  if [ -z "$AZUREAD_FILE_PATH" ]; then
    error "No azuread secret file found in dir '$APP_DIR'. Please ensure it follows this format:\n\n\t.nais/azuread-etterlatte-<APP_NAME>-lokal.yaml"
  fi

  info "Using local secret ($AZUREAD_FILE_PATH)"
  AZURE_SECRET_NAME=$(grep "secretName" $AZUREAD_FILE_PATH | awk '{print $2}')

  if [ -z "$AZURE_SECRET_NAME" ]; then
    error "Field 'secretName' not found in file '$AZUREAD_FILE_PATH'"
  fi
}

getRemoteSecretName() {
  info "Using remote (dev-gcp) secret"
  info "Checking kubernetes for secret for $APP_NAME"

  AZURE_SECRET_NAME=$(kubectl get secrets | grep "azure-$APP_NAME" -m1 | awk '{print $1}')
  if [ -z "$AZURE_SECRET_NAME" ]; then
    error "No azuread secret found for app name $APP_NAME"
  fi

  info "Found secret with name '$AZURE_SECRET_NAME'"
}

getAzureadSecretName() {
  LOCAL_SECRET=$(ls $APP_DIR/.nais | grep 'lokal')

  if [ -n "$LOCAL_SECRET" ]; then
    echo ""
    info "Found file \033[4m$LOCAL_SECRET\033[0m"
    read -p "Do you want to use this secret? [y/N] " USE_LOKAL_SECRET
  fi

  if [ "$USE_LOKAL_SECRET" == "y" ]; then
    getLocalSecretName
  else
    getRemoteSecretName
  fi
}

getMaskinportenSecretName() {
  info "Checking kubernetes for Maskinporten secret for $APP_NAME"

  MASKINPORTEN_SECRET_NAME=$(kubectl get secrets | grep "maskinporten-$APP_NAME" -m1 | awk '{print $1}')
  if [ -n "$MASKINPORTEN_SECRET_NAME" ]; then
    info "Found secret with name '$MASKINPORTEN_SECRET_NAME'"
  else
    info "No secret for maskinporten found ... continuing without maskinporten secret"
  fi
}

addSecretToEnvFile() {
  getAzureadSecretName

  getMaskinportenSecretName

  info "Fetching $APP_NAME secrets from kubernetes"

  # Wonderwall / sidecar
  APP_USING_SIDECAR=$(cat $APP_DIR/.nais/dev.yaml | grep 'sidecar')

  if [ -n "$APP_USING_SIDECAR" ]; then
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

    if [ -n "$MASKINPORTEN_SECRET_NAME" ]; then
        kubectl -n etterlatte get secret $MASKINPORTEN_SECRET_NAME -o json \
            | jq -r '.data | map_values(@base64d) | to_entries[] | (.key | ascii_upcase) +"=" + .value' \
            >> $APP_DIR/.env.dev-gcp
    fi
  fi

  APP_USING_UNLEASH=$(cat $APP_DIR/.nais/dev.yaml | grep 'unleash')
  if [ -n "$APP_USING_UNLEASH" ]; then
    kubectl -n etterlatte get secret my-application-unleash-api-token -o json \
        | jq -r '.data | map_values(@base64d) | to_entries[] | (.key | ascii_upcase) +"=" + .value' \
        >> $APP_DIR/.env.dev-gcp
    info "Unleash secrets added"
  fi

  info "\033[4m.env.dev-gcp\033[0m successfully created with valid secrets!"
}

info "Initializing ..."

# Ensure user has necessary tools
userHasJq
userHasFzf

# Ensure .env files cannot be commited by accident
validateGitIgnore

# Ensure user is authenticated
userIsAuthenticated

# Ensure directory is set and exists
validateAppDir $1

# Create .env-file containing secrets
addSecretToEnvFile
