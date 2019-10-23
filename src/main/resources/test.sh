#!/bin/bash

if [ "$1" == "red" ] || [ "$1" == "white" ]; then
    team=$1
    user=$2
fi
if [ "$2" == "red" ] || [ "$2" == "white" ]; then
    team=$2
    user=$1
fi

if [ -z "$user" ]; then
    read -p "username? " user
fi
if [ -z "$team" ]; then
    read -p "team? " team
fi

count=1
RANGE=4
directionArray=("up" "down" "left" "right")

while true; do
    number=$RANDOM
    let "number %= $RANGE"
    
    echo ${directionArray[$number]}
    curl -sX POST "https://directive-producer-demojam-zombie.apps.akrohg-openshift.redhatgov.io/camel/rest/produce/${team}" --data "{\"username\":\"${user}\",\"direction\":\"${directionArray[$number]}\"}" -H "Content-Type: application/json" > /dev/null
    sleep .2
done

echo "DONE!"
