#!/bin/bash
ROOT=`pwd`
java -jar "$ROOT/server/target/server-1.0-SNAPSHOT.jar" \
     -Djavax.net.ssl.keyStore=conn_keystore -Djavax.net.ssl.keyStorePassword=password
