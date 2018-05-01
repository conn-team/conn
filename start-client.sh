#!/bin/bash
ROOT=`pwd`
java -Djavax.net.ssl.trustStore=conn_keystore -Djavax.net.ssl.trustStorePassword=password \
     -jar "$ROOT/client-app/target/client-app-1.0-SNAPSHOT.jar"
