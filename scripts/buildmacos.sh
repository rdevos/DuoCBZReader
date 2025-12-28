#!/bin/bash

ARCH=$(uname -m)
if [ "$ARCH" = "x86_64" ]; then
    ARCH_SUFFIX="x64"
elif [ "$ARCH" = "arm64" ]; then
    ARCH_SUFFIX="arm"
else
    echo "Unknown architecture: $ARCH" >&2
    exit 1
fi

source ./buildcommon.sh

echo "Removing old build files"

rm -rf ../target/mac/

echo "Building binary"

jpackage --name DuoCBZReader \
         --input ../target/scala-3.7.0 \
         --main-jar duoCBZReader.jar \
         --main-class be.afront.reader.DuoCBZReader \
         --type app-image \
         --runtime-image custom-jre \
         --dest ../target/mac/ \
         --copyright "Copyright 2025 Paul Janssens - All rights reserved" \
         --app-version "${VERSION}" \
         --file-associations cbz.properties \
         --icon MyIcon.icns \
         --mac-package-identifier be.afront.reader

cd ../target/mac/

echo "Tweaking the .plist file"

plutil -insert CFBundleDocumentTypes.0 -xml '<dict>
<key>CFBundleTypeName</key><string>ZIP Archive</string>
<key>CFBundleTypeRole</key><string>Viewer</string>
<key>LSHandlerRank</key><string>Alternate</string>
<key>LSItemContentTypes</key><array><string>public.zip-archive</string></array>
</dict>' DuoCBZReader.app/Contents/Info.plist

plutil -insert CFBundleDocumentTypes.1 -xml '<dict>
<key>CFBundleTypeName</key><string>EPUB Document</string>
<key>CFBundleTypeRole</key><string>Viewer</string>
<key>LSHandlerRank</key><string>Alternate</string>
<key>LSItemContentTypes</key><array><string>org.idpf.epub-container</string></array>
</dict>' DuoCBZReader.app/Contents/Info.plist

echo "creating the installer"

pkgbuild --component DuoCBZReader.app \
         --install-location /Applications \
         --ownership recommended \
         --identifier be.afront.reader \
         --scripts ../../scripts/macos \
         --version ${VERSION} \
         component.pkg

rm -rf iso/
mkdir iso

productbuild --package component.pkg /Applications iso/DuoCBZReader-${ARCH_SUFFIX}-${VERSION}.pkg

hdiutil create -volname "DuoCBZReader Installer" -srcfolder iso -ov -format UDZO DuoCBZReaderInstaller-${ARCH_SUFFIX}-${VERSION}.dmg

cd -