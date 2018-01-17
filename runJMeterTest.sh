#!/bin/bash

SERVER="$1"
PORT="$2"
THREADS="$3"
LOOPS="$4"


sed -e "s/%SERVERPORT%/${PORT}/g" -e "s/%SERVER%/${SERVER}/g" -e "s/%THREADS%/${THREADS}/g" -e "s/%LOOPS%/${LOOPS}/g" template.jmx > temp.jmx
~/apache-jmeter-3.3/bin/jmeter -n -t temp.jmx
