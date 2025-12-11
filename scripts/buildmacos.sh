#!/bin/bash

./buildcommon.sh

rm -rf ../target/mac

jpackage --name DuoCBZReader \
         --input ../target/scala-3.7.0 \
         --main-jar duoCBZReader.jar \
         --main-class be.afront.reader.DuoCBZReader \
         --type app-image \
         --runtime-image custom-jre \
         --dest ../target/mac \
         --copyright "Copyright 2025 Paul Janssens - All rights reserved" \
         --app-version "1.0.6" \
         --file-associations cbz.properties \
         --icon MyIcon.icns \
         --mac-package-identifier be.afront.reader

plutil -insert CFBundleDocumentTypes.0 -xml '<dict>
<key>CFBundleTypeName</key><string>ZIP Archive</string>
<key>CFBundleTypeRole</key><string>Viewer</string>
<key>LSHandlerRank</key><string>Alternate</string>
<key>LSItemContentTypes</key><array><string>public.zip-archive</string></array>
</dict>' ../target/mac/DuoCBZReader.app/Contents/Info.plist

plutil -insert CFBundleDocumentTypes.1 -xml '<dict>
<key>CFBundleTypeName</key><string>EPUB Document</string>
<key>CFBundleTypeRole</key><string>Viewer</string>
<key>LSHandlerRank</key><string>Alternate</string>
<key>LSItemContentTypes</key><array><string>org.idpf.epub-container</string></array>
</dict>' ../target/mac/DuoCBZReader.app/Contents/Info.plist
