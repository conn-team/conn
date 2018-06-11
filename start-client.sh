#!/bin/bash
ROOT=`pwd`
java -jar "$ROOT/client-app/target/client-app-1.0-SNAPSHOT.jar" \
     -jks conn_keystore -jks-password password -host localhost $@
