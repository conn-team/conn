#!/bin/bash
ROOT=`pwd`
java -jar "$ROOT/server/target/server-1.0-SNAPSHOT.jar" -jks conn_keystore -jks-password password $@
