#!/bin/bash

NAME=`basename $0`

info() {
    echo -e "\033[1;32mINFO:\033[0m\t$1"
}

warn() {
    echo -e "\033[1;33mWARN:\033[0m\t$1"
}

error() {
    echo -e "\033[1;31mERROR:\033[0m\t$1" >&2
    exit;
}

ps_loader() {
  PID=$!

  while ps -p $PID &>/dev/null; do
    echo -ne "."
    sleep 1
  done

  echo "" # newline
}

help() {
    echo -e "\033[1;33m$NAME\033[0m is a script to simplify database dumping from dev-gcp to localhost \n"
    echo -e "Usage: \n  $NAME [OPTION] [ARGS...] \n"
    echo "Options:"
    echo -e "  -d \t\t proxied database name you want to dump data from"
    echo -e "  -p \t\t port to local database where data should be restored (cannot be 5432)"
    echo -e "  -u \t\t username for the remote proxy"
    exit 1
}

if [[ $* == --help ]]; then
    help
fi

if [ $# -eq 0 ]; then
    error "No arguments provided -- \n  Try \"$NAME --help\" for more information"
fi

while getopts ":d:p:u:" opt; do
  case $opt in
    d)
      PROXY_DB="$OPTARG"
      ;;
    p)
      if [[ "$OPTARG" == 5432 ]]; then
        error "Illegal port 5432 – this port is reserved for the proxy!"
      else
        LOCAL_PORT="$OPTARG"
      fi
      ;;
    u)
      PROXY_USER="$OPTARG"
      ;;
    \?)
      error "Invalid option: -$OPTARG" >&2
      ;;
    *)
      error "No arguments provided -- \n  Try \"$NAME --help\" for more information" >&2
      ;;
  esac
done


if [[ -z "$PROXY_USER" ]]; then
    error "Proxy user (-u) is required -- \n  Try \"$NAME --help\" for more information"
elif [[ -z "$PROXY_DB" ]]; then
    error "Proxy database (-d) is required -- \n  Try \"$NAME --help\" for more information"
elif [[ -z "$LOCAL_PORT" ]]; then
    error "Local db port (-p) is required -- \n  Try \"$NAME --help\" for more information"
fi

NOW=$(date +%s)
DUMP_FILE="${PROXY_DB}_$(date +%s).dump"
DOCKER_HOST="host.docker.internal"
CURRENT_CONTEXT=$(kubectl config current-context)

# Ensure the dump file is deleted on exit
trap "rm $DUMP_FILE 2> /dev/null" EXIT

nais device status | grep -q -i "naisdevice status: Disconnected" &> /dev/null
if [ $? -eq 0 ]; then # Grep returns 0 if there is a match
  error "Naisdevice is not connected. Please run:\n\n\t$ nais device connect \n\nOr use the naisdevice.app"
fi

gcloud auth print-identity-token &> /dev/null
if [ $? -gt 0 ]; then
  error "Not logged into gcloud... Please run:\n\n\t$ gcloud auth login"
fi

# Denne sjekken må IKKE kommenteres ut!
if [ "$CURRENT_CONTEXT" != "dev-gcp" ]; then
    error "Current context is $CURRENT_CONTEXT, but should be 'dev-gcp'"
fi

# Check if connected to dev proxy
if psql -h localhost -p 5432 -U $PROXY_USER -l  | grep -q 'cloudsqladmin'; then
  info "Connected to proxy on [localhost:5432]"
else
  error "Unable to connect to [localhost:5432]. Are you sure the proxy is up and running?"
fi

info "Database cloning from dev-gcp started!"
info "Running pg_dump on [$PROXY_DB] with username [$PROXY_USER]"

pg_dump \
  --format=custom \
  --no-privileges \
  --host=localhost \
  --port=5432 \
  --username=$PROXY_USER \
  --dbname=$PROXY_DB \
   > $DUMP_FILE &

# Wait for the background process to finish and capture its exit status
wait $!
PG_DUMP_EXIT_STATUS=$?

if [ $PG_DUMP_EXIT_STATUS -eq 0 ]; then
  info "pg_dump was successful!"
else
  rm $DUMP_FILE 2> /dev/null
  error "Unexpected error occurred while running pg_dump"
fi


info "Dropping database 'postgres' on [$DOCKER_HOST:$LOCAL_PORT]"
dropdb --force -h $DOCKER_HOST -p $LOCAL_PORT -U postgres postgres
info "Creating database 'postgres' on [$DOCKER_HOST:$LOCAL_PORT]"
createdb -h $DOCKER_HOST -p $LOCAL_PORT -U postgres postgres

info "Running pg_restore"
pg_restore \
  --no-owner \
  --no-privileges \
  --host=$DOCKER_HOST \
  --port=$LOCAL_PORT \
  --username=postgres \
  --dbname=postgres \
  $DUMP_FILE &

# Wait for the background process to finish and capture its exit status
wait $!
PG_RESTORE_EXIT_STATUS=$?

if [ $PG_RESTORE_EXIT_STATUS -eq 0 ]; then
  info "pg_restore was successful!"
  info "Database cloning from dev-gcp completed!"
else
  error "Unexpected error occurred while running pg_restore"
fi
