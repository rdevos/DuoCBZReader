#!/bin/bash

rm -rf custom-jre

JDK_MAJOR=$(jlink --version | cut -d '.' -f 1)

# Avoid warnings but still support older JDKs

if [ "$JDK_MAJOR" -ge 21 ]; then
    COMPRESS="--compress=zip-6"
else
    COMPRESS="--compress=2"
fi

jlink --module-path $JAVA_HOME/jmods \
      --add-modules java.base,java.desktop,java.logging,jdk.unsupported  \
      --output custom-jre $COMPRESS



rm -rf output

jpackage --name DuoCBZReader \
         --input ../target/scala-3.7.0 \
         --main-jar duoCBZReader.jar \
         --main-class be.afront.reader.DuoCBZReader \
         --type app-image \
         --runtime-image custom-jre \
         --dest output \
         --copyright "Copyright 2025 Paul Janssens - All rights reserved" \
         --app-version "1.0.2" \
         --file-associations zip.properties \
         --file-associations cbz.properties \
         --icon MyIcon.icns \
         --mac-package-identifier be.afront.reader

plutil -replace CFBundleDocumentTypes.0 -xml '<dict><key>CFBundleTypeName</key><string>ZIP Archive</string><key>CFBundleTypeRole</key><string>Viewer</string><key>LSHandlerRank</key><string>Alternate</string><key>LSItemContentTypes</key><array><string>public.zip-archive</string></array></dict>' output/DuoCBZReader.app/Contents/Info.plist