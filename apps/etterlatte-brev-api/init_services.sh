#!/bin/bash

info() {
  echo -e "\n\033[0;36m\t--------\n$1\n\t--------\033[0m\n"
}
info "Setting up network"

docker network create kafka

info "Setting up postgres"

echo "Stopping and removing current container"
docker stop etterlatte-brev; docker rm etterlatte-brev

echo "Starting postgres docker image"
docker pull postgres
docker run \
    --name etterlatte-brev \
    -e POSTGRES_PASSWORD=postgres \
    -p 5432:5432 \
    -d \
    --rm \
    postgres

info "Setting up Zookeeper"
echo "Stopping and removing current container"
docker stop kafka; docker rm kafka
docker stop zookeeper; docker rm zookeeper

echo "Starting zookeeper docker image"
docker pull confluentinc/cp-zookeeper:latest
docker run \
    --name zookeeper \
    --net kafka \
    --env-file zookeeper.env \
    -d \
    --rm \
    confluentinc/cp-zookeeper:latest


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

#Starter kafka til slutt så zookeeper får tid til å starte her så
info "Setting up Kafka"

echo "Starting kafka docker image"
docker pull confluentinc/cp-kafka:latest
docker run \
    --name kafka \
    --net kafka \
    --env-file kafka.env \
    -p 9092:9092 \
    -d \
    --rm \
    confluentinc/cp-kafka:latest

echo "Run Kotlin application with the following env variables:"
echo -e "\033[0;36mDB_HOST=localhost;DB_PORT=5432;DB_DATABASE=postgres;DB_USERNAME=postgres;DB_PASSWORD=postgres\033[0m"
