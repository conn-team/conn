#!/bin/bash
keytool -genkey -keyalg EC -keysize 256 -noprompt                     \
        -keystore conn_keystore -storepass password -keypass password \
        -dname "CN=localhost, OU=conn, O=conn, L=conn, ST=conn, C=PL"
