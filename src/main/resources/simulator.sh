#!/bin/bash

for i in {1..500}; do
     ~/directive-consumer/src/main/resources/test.sh redbot$i red &
done

for i in {1..500}; do
     ~/directive-consumer/src/main/resources/test.sh whitebot$i white &
done

# TO DELETE ALL RUNNING THREADS
sleep 50
ps -ax | grep "bin/bash " | awk '{ system("kill -9 "$1) }'
