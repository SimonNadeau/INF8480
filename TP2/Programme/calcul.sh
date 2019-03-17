#!/bin/bash

pushd $(dirname $0) > /dev/null
basepath=$(pwd)
popd > /dev/null

cat << EndOfMessage
HELP: 
./calcul.sh ip_address
	- ip_address: (OPTIONAL) L'addresse ip du serveur.
	  Si l'arguement est non fourni, on conisdère que le serveur est local (ip_address = 127.0.0.1)

EndOfMessage

IPADDR=$1
NUM=$2
MAL=$3
NAME=$4
if [ -z "$1" ]
  then
    IPADDR="127.0.0.1"
fi

if [ -z "$2" ]
  then
    NUM=5
fi

if [ -z "$3" ]
  then
    MAL=0
fi

if [ -z "$4" ]
  then
    NAME="calcul"
fi

java -cp "$basepath"/calcul.jar:"$basepath"/partage.jar \
  -Djava.rmi.server.codebase=file:"$basepath"/partage.jar \
  -Djava.security.policy="$basepath"/policy \
  -Djava.rmi.server.hostname="$IPADDR" \
  tp2.calcul.Calcul $NUM $MAL $NAME
