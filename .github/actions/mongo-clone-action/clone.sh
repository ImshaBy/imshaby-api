#!/bin/bash

MONGO_URI_SOURCE=$1
MONGO_URI_TARGET=$2
#remove additional parameters from URI
DB_NAME_SOURCE_WITHOUT_PARAMS=(${MONGO_URI_SOURCE//\?/ })
DB_NAME_TARGET_WITHOUT_PARAMS=(${MONGO_URI_TARGET//\?/ })
DB_NAME_SOURCE="${DB_NAME_SOURCE_WITHOUT_PARAMS##*/}"
DB_NAME_TARGET="${DB_NAME_TARGET_WITHOUT_PARAMS##*/}"

mongodump --uri="${MONGO_URI_SOURCE}" --forceTableScan --archive | mongorestore --uri="${MONGO_URI_TARGET}" --archive --nsInclude="${DB_NAME_SOURCE}.*" --nsFrom="${DB_NAME_SOURCE}.*" --nsTo="${DB_NAME_TARGET}.*" --drop