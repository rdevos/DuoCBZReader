#!/bin/bash

APP_PATH="/Applications/DuoCBZReader.app"

/usr/bin/xattr -cr "$APP_PATH"
/usr/bin/codesign --force --deep --sign - "$APP_PATH"

echo "Post-install: Quarantine removed and ad-hoc signing applied to $APP_PATH"

exit 0