#!/bin/bash

info() {
    echo -e "INFO: $1"
}

warn() {
    echo -e "\033[1;33mWARN:\033[0m $1"
}

error() {
    echo -e "\033[1;31mERROR:\033[0m $1"
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
      exit;
    fi
  fi
}

addSecretToEnvFile() {
  userHasJq

  info "Fetching secrets from kubernetes"

  kubectl -n etterlatte get secret azuread-ey-sak-lokal -o json \
      | jq -r '.data | map_values(@base64d)
        | .["WONDERWALL_OPENID_CLIENT_ID"] = .AZURE_APP_CLIENT_ID
        | .["WONDERWALL_OPENID_CLIENT_JWK"] = .AZURE_APP_JWK
        | .["WONDERWALL_OPENID_WELL_KNOWN_URL"] = .AZURE_APP_WELL_KNOWN_URL
        | to_entries[] | (.key | ascii_upcase) +"=" + .value' \
      | sed -r "s/^(WONDERWALL_OPENID_CLIENT_JWK)=(\{.*\})$/\1='\2'/g" \
      > .env.dev-gcp

  if [ $? -eq 0 ]; then
    info "Secrets saved to .env.dev-gcp"
  else
    error "Unhandled error on 'kubectl get secret' ..."
  fi
}


CAN_READ_SECRET=$(kubectl auth can-i get secret)

if [ "$CAN_READ_SECRET" == "yes" ]; then
  addSecretToEnvFile
else
  error "Not logged in or lacking rights. Ensure you're connected to naisdevice and gcp."
fi
