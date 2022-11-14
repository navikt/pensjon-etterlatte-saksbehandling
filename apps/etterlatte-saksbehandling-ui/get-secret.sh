#!/bin/bash

addSecretToEnvFile() {
  kubectl -n etterlatte get secret azuread-ey-sak-lokal -o json \
      | jq -r '.data | map_values(@base64d)
        | .["WONDERWALL_OPENID_CLIENT_ID"] = .AZURE_APP_CLIENT_ID
        | .["WONDERWALL_OPENID_CLIENT_JWK"] = .AZURE_APP_JWK
        | .["WONDERWALL_OPENID_WELL_KNOWN_URL"] = .AZURE_APP_WELL_KNOWN_URL
        | to_entries[] | (.key | ascii_upcase) +"=" + .value' \
      > .env.dev-gcp
}

echo "Henter secrets og oppretter .env.dev-gcp"

CAN_READ_SECRET=$(kubectl auth can-i get secret)

if [ $CAN_READ_SECRET == 'yes' ]; then
  addSecretToEnvFile
  echo "Secret saved to .env.dev-gcp"
else
  echo "Not logged in or lacking rights. Ensure you're connected to naisdevice and gcp."
fi
