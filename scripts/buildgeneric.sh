#!/bin/bash

./buildcommon.sh

rm -rf output

jpackage --name DuoCBZReader \
         --input ../target/scala-3.7.0 \
         --main-jar duoCBZReader.jar \
         --main-class be.afront.reader.DuoCBZReader \
         --type app-image \
         --runtime-image custom-jre \
         --dest output \
         --copyright "Copyright 2025 Paul Janssens - All rights reserved" \
         --app-version "1.0.3" \
         --file-associations zip.properties \
         --file-associations cbz.properties \
         --file-associations epub.properties \
         --icon MyIcon.icns \
         --mac-package-identifier be.afront.reader
