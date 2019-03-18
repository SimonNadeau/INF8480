#!/bin/bash

pushd $(dirname $0) > /dev/null
basepath=$(pwd)
popd > /dev/null

cat << EndOfMessage
HELP: 
./repartiteur.sh ip_address
	- remote_server_ip: (OPTIONAL) l'addresse ip du serveur distant

EndOfMessage

java -cp "$basepath"/repartiteur.jar:"$basepath"/partage.jar -Djava.security.policy="$basepath"/policy tp2.repartiteur.Repartiteur $*
