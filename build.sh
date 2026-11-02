#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
: "${ANDROID_SDK_ROOT:?Set ANDROID_SDK_ROOT to the Android SDK path}"
ANDROID_PLATFORM=${ANDROID_PLATFORM:-android-35}
BUILD_TOOLS_VERSION=${BUILD_TOOLS_VERSION:-36.0.0}
bt="$ANDROID_SDK_ROOT/build-tools/$BUILD_TOOLS_VERSION"
platform="$ANDROID_SDK_ROOT/platforms/$ANDROID_PLATFORM/android.jar"
python3 tools/dependencies.py
rm -rf build/classes build/dex
mkdir -p build/classes build/dex
python3 - <<'PY'
import pathlib,subprocess
root=pathlib.Path('.');deps=sorted((root/'.deps').glob('*.jar'));platform=pathlib.Path(__import__('os').environ['ANDROID_SDK_ROOT'])/'platforms'/__import__('os').environ.get('ANDROID_PLATFORM','android-35')/'android.jar'
cmd=['java','com.sun.tools.javac.Main','-source','8','-target','8','-encoding','UTF-8','-classpath',':'.join(map(str,[platform,*deps])),'-d','build/classes',*map(str,(root/'app/src/main/java').rglob('*.java'))]
subprocess.run(cmd,check=True)
import zipfile
with zipfile.ZipFile('build/classes.jar','w') as z:
 for f in pathlib.Path('build/classes').rglob('*.class'):z.write(f,str(f.relative_to('build/classes')))
PY
"$bt/d8" --min-api 26 --lib "$platform" --output build/dex build/classes.jar .deps/*.jar
"$bt/aapt2" compile --dir app/src/main/res -o build/compiled-res.zip
"$bt/aapt2" link build/compiled-res.zip -o build/resources.apk -I "$platform" --manifest app/src/main/AndroidManifest.xml --rename-manifest-package com.gr4nt.filexplorer --version-code 9 --version-name 0.7.2 -A app/src/main/assets --min-sdk-version 26 --target-sdk-version 35
python3 - <<'PY'
import zipfile,pathlib,shutil
shutil.copyfile('build/resources.apk','build/unsigned.apk')
with zipfile.ZipFile('build/unsigned.apk','a',zipfile.ZIP_DEFLATED) as z:
 for f in pathlib.Path('build/dex').glob('*.dex'):z.write(f,f.name)
PY
"$bt/zipalign" -f 4 build/unsigned.apk build/aligned.apk
if [ ! -f build/development.keystore ]; then
 keytool -genkeypair -keystore build/development.keystore -storepass android -keypass android -alias androiddebugkey -dname 'CN=File-Xplorer Development' -keyalg RSA -keysize 2048 -validity 3650 >/dev/null 2>&1
fi
"$bt/apksigner" sign --ks build/development.keystore --ks-key-alias androiddebugkey --ks-pass pass:android --key-pass pass:android --out build/File-Xplorer-v0.7.2.apk build/aligned.apk
"$bt/apksigner" verify --verbose build/File-Xplorer-v0.7.2.apk
