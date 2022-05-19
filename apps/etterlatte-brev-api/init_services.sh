#!/bin/bash

info() {
  echo -e "\n\033[0;36m\t--------\n$1\n\t--------\033[0m\n"
}

info "Setting up postgres"

echo "Stopping and removing current container"
docker stop etterlatte-brev; docker rm etterlatte-brev

echo "Starting postgres docker image"
docker pull postgres
docker run \
    --name etterlatte-brev \
    -e POSTGRES_PASSWORD=test \
    -p 5432:5432 \
    -d \
    --rm \
    postgres

info "Setting up redpanda"

echo "Stopping and removing current container"
docker stop brev-distribusjon; docker rm brev-distribusjon

echo "Starting redpanda docker image"
docker pull vectorized/redpanda
docker run \
    --name brev-distribusjon \
    -e NAIS_APP_NAME=etterlatte-brev-distribusjon \
    -p 51336:51336 \
    -d \
    --rm \
    vectorized/redpanda

info "Setting up ey-pdfgen"

echo "Stopping and removing current container"
docker stop ey-pdfgen; docker rm ey-pdfgen

PDFGEN_DIR="$(find ~/ -type d -name ey-pdfgen -print -quit 2>/dev/null)"

docker pull ghcr.io/navikt/pdfgen
docker run \
    --name ey-pdfgen \
    -v $PDFGEN_DIR/templates:/app/templates \
    -v $PDFGEN_DIR/fonts:/app/fonts \
    -v $PDFGEN_DIR/data:/app/data \
    -v $PDFGEN_DIR/resources:/app/resources \
    -p 8081:8080 \
    -e DISABLE_PDF_GET=false \
    -d \
    --rm \
    ghcr.io/navikt/pdfgen:1.5.0


#sleep 2

#docker exec etterlatte-brev psql -U postgres -c "CREATE DATABASE \"postgres\";" 2>/dev/null

echo "Run Kotlin application with the following env variables:"
echo -e "\033[0;36mDB_HOST=localhost;DB_PORT=5432;DB_DATABASE=postgres;DB_USERNAME=postgres;DB_PASSWORD=test\033[0m"
