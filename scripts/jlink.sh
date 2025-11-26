#!/bin/bash

rm -rf custom-jre

jlink --module-path $JAVA_HOME/jmods \
      --add-modules java.base,java.desktop,java.logging,jdk.unsupported  \
      --output custom-jre \
      --compress=zip-6



rm -rf output

jpackage --name DuoCBZReader \
         --input ../target/scala-3.7.0 \
         --main-jar duoCBZReader.jar \
         --main-class be.afront.reader.DuoCBZReader \
         --type app-image \
         --runtime-image custom-jre \
         --dest output \
         --copyright "Copyright 2025 Paul Janssens - All rights reserved" \
         --app-version "1.0.0" \
         --icon MyIcon.icns \
         --mac-package-identifier be.afront.reader