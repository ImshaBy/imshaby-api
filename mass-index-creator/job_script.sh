#!/bin/sh
exec java -XX:MaxRAMPercentage=85 -XX:MinRAMPercentage=85 -jar /opt/app/app.jar
