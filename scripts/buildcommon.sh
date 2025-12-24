#!/bin/bash

rm -rf custom-jre

VERSION=1.0.11

JDK_MAJOR=$(jlink --version | cut -d '.' -f 1)

# Avoid warnings but still support older JDKs

if [ "$JDK_MAJOR" -ge 21 ]; then
    COMPRESS="--compress=zip-6"
else
    COMPRESS="--compress=2"
fi

jlink --module-path $JAVA_HOME/jmods \
      --add-modules java.base,java.desktop,jdk.charsets,java.logging,jdk.unsupported  \
      --output custom-jre $COMPRESS
