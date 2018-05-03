#!/bin/bash
# Protobufs are generated on demand, because it confuses VS code intellisense

set -e
PROTO_DIR=core/src/main/java/com/github/connteam/conn/core/net/proto/

rm -fr "$PROTO_DIR"
mvn -f "core/pom.xml" com.github.os72:protoc-jar-maven-plugin:run
find "$PROTO_DIR" -name '*.java' | xargs sed -i -r 's/public final class /@SuppressWarnings("all")\n\0/g'
