#!/bin/bash
ROOT=`pwd`
java -Djavax.net.ssl.keyStore=conn_keystore -Djavax.net.ssl.keyStorePassword=password \
     -Djava.util.logging.config.file=logging.properties                               \
     -jar "$ROOT/server/target/server-1.0-SNAPSHOT.jar" $@
