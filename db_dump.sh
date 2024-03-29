#!/bin/bash

NAME=`basename $0`

error() {
    echo -e "$1" >&2
    exit 1
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
      echo "Invalid option: -$OPTARG" >&2
      exit 1
      ;;
    *)
      echo -e "No arguments provided -- \n  Try \"$NAME --help\" for more information" >&2
      exit 1
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


CURRENT_CONTEXT=$(kubectl config current-context)

if [ "$CURRENT_CONTEXT" != "dev-gcp" ]; then
    error "Current context is $CURRENT_CONTEXT, but should be 'dev-gcp'"
fi

NOW=$(date +%s)
DUMP_FILE="${PROXY_DB}_$(date +%s).sql"

echo "Starting pg_dump (this can take a minute)"
pg_dump -x -a -h localhost -p 5432 -U $PROXY_USER -d $PROXY_DB -f $DUMP_FILE &
DUMP_PID=$!

while ps -p $DUMP_PID &>/dev/null; do
  echo -ne "."
  sleep 1
done

if [ $? -eq 0 ]; then
  echo -e "\n\npg_dump was successful. Restoring to local database.\n"
  psql -h host.docker.internal -p $LOCAL_PORT -U postgres -d postgres -f $DUMP_FILE

  echo -e "\n\nDump and restore completed!"
else
  error "\nUnexpected error occurred while running pg_dump"
fi

rm $DUMP_FILE 2> /dev/null
