#!/bin/sh
while true; do
  echo "Indexing started"
  /opt/app/script.sh
  result=$?
  echo "Indexing finished with code $result; next run in 600 seconds"
  sleep 600
done
