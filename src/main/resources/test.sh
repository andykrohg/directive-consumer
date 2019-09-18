#!/bin/bash

user=$1
team=$2

# Up 2 times
curl -sX POST "https://directive-producer-demojam-zombie.apps.akrohg-openshift.redhatgov.io/camel/rest/produce/${team}" --data "{\"username\":\"${user}\",\"direction\":\"up\"}" -H "Content-Type: application/json" > /dev/null
echo "UP"
sleep 1
curl -sX POST "https://directive-producer-demojam-zombie.apps.akrohg-openshift.redhatgov.io/camel/rest/produce/${team}" --data "{\"username\":\"${user}\",\"direction\":\"up\"}" -H "Content-Type: application/json" > /dev/null
echo "UP"

# Wait for zombie to pass
sleep 3

# Right 3 times
curl -sX POST "https://directive-producer-demojam-zombie.apps.akrohg-openshift.redhatgov.io/camel/rest/produce/${team}" --data "{\"username\":\"${user}\",\"direction\":\"right\"}" -H "Content-Type: application/json" > /dev/null
echo "RIGHT"
sleep 1
curl -sX POST "https://directive-producer-demojam-zombie.apps.akrohg-openshift.redhatgov.io/camel/rest/produce/${team}" --data "{\"username\":\"${user}\",\"direction\":\"right\"}" -H "Content-Type: application/json" > /dev/null
echo "RIGHT"
sleep 1
curl -sX POST "https://directive-producer-demojam-zombie.apps.akrohg-openshift.redhatgov.io/camel/rest/produce/${team}" --data "{\"username\":\"${user}\",\"direction\":\"right\"}" -H "Content-Type: application/json" > /dev/null
echo "RIGHT"
sleep 1

# Down 1 time
curl -sX POST "https://directive-producer-demojam-zombie.apps.akrohg-openshift.redhatgov.io/camel/rest/produce/${team}" --data "{\"username\":\"${user}\",\"direction\":\"down\"}" -H "Content-Type: application/json" > /dev/null
echo "DOWN"
sleep 1

# Right 1 time
curl -sX POST "https://directive-producer-demojam-zombie.apps.akrohg-openshift.redhatgov.io/camel/rest/produce/${team}" --data "{\"username\":\"${user}\",\"direction\":\"right\"}" -H "Content-Type: application/json" > /dev/null
echo "RIGHT"

# Wait for zombie to pass
sleep 1

# Left 1 time
curl -sX POST "https://directive-producer-demojam-zombie.apps.akrohg-openshift.redhatgov.io/camel/rest/produce/${team}" --data "{\"username\":\"${user}\",\"direction\":\"left\"}" -H "Content-Type: application/json" > /dev/null
echo "LEFT"
sleep 1

# Up 1 time
curl -sX POST "https://directive-producer-demojam-zombie.apps.akrohg-openshift.redhatgov.io/camel/rest/produce/${team}" --data "{\"username\":\"${user}\",\"direction\":\"up\"}" -H "Content-Type: application/json" > /dev/null
echo "UP"
sleep 1

# Left 1 time
curl -sX POST "https://directive-producer-demojam-zombie.apps.akrohg-openshift.redhatgov.io/camel/rest/produce/${team}" --data "{\"username\":\"${user}\",\"direction\":\"left\"}" -H "Content-Type: application/json" > /dev/null
echo "LEFT"
sleep 1

# Up 2 times
curl -sX POST "https://directive-producer-demojam-zombie.apps.akrohg-openshift.redhatgov.io/camel/rest/produce/${team}" --data "{\"username\":\"${user}\",\"direction\":\"up\"}" -H "Content-Type: application/json" > /dev/null
echo "UP"
sleep 1
curl -sX POST "https://directive-producer-demojam-zombie.apps.akrohg-openshift.redhatgov.io/camel/rest/produce/${team}" --data "{\"username\":\"${user}\",\"direction\":\"up\"}" -H "Content-Type: application/json" > /dev/null
echo "UP"
sleep 1

# Right 2 times
curl -sX POST "https://directive-producer-demojam-zombie.apps.akrohg-openshift.redhatgov.io/camel/rest/produce/${team}" --data "{\"username\":\"${user}\",\"direction\":\"right\"}" -H "Content-Type: application/json" > /dev/null
echo "RIGHT"
sleep 1

echo "DONE!"
