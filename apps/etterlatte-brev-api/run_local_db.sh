#!/bin/bash

usage() {
	error "Invalid or missing flags."
	echo "Usage: run_local_db [options]"
	echo -e "\nwhere options include: \n"
	echo -e "\t-n <name> \n\t\tThe name of the docker container and database name"
	echo -e "\t-p <port> \n\t\tThe port which the docker image will run"
}

# Initialize provided options
while getopts ":n:p:" opt; do
	case "$opt" in
		n)
			name=${OPTARG}
			;;
		p)
			port=${OPTARG}
			;;
		*)
			usage
			;;
	esac
done
shift $((OPTIND-1))

# Ensure required options are supplied
if [ -z "${name}" ] || [ -z "${port}" ]; then
    usage
    exit 1;
fi

echo "Stopping and removing current container"
docker stop $name; docker rm $name

echo "Starting postgres docker image."
docker run \
    --name $name \
    -e POSTGRES_PASSWORD=test \
    -p $port:$port \
    -d \
    postgres

sleep 2

docker exec $name psql -U postgres -c "CREATE DATABASE \"$name\";"

echo "Run Kotlin application with the following env variables:"
echo -e "\033[0;36mDB_HOST=localhost;DB_PORT=$port;DB_DATABASE=$name;DB_USERNAME=postgres;DB_PASSWORD=test\033[0m"
