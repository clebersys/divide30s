#!/usr/bin/env bash
# Gera o AAB e o APK de release assinados e copia para dist/.
#
# Requisitos: ANDROID_HOME apontando para o SDK e keystore.properties presente.
set -euo pipefail

cd "$(dirname "$0")/.."

export ANDROID_HOME="${ANDROID_HOME:-$HOME/Android/Sdk}"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
export LC_ALL=C.utf8 LANG=C.utf8
export JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8"

if [ ! -f keystore.properties ]; then
  echo "ERRO: keystore.properties não encontrado." >&2
  echo "      Copie keystore.properties.example e preencha as senhas." >&2
  exit 1
fi

VERSION=$(grep -oP 'versionName\s*=\s*"\K[^"]+' app/build.gradle.kts)
echo ">> Build da versão $VERSION"

./gradlew clean assembleRelease bundleRelease

mkdir -p dist
cp app/build/outputs/apk/release/app-release.apk "dist/Divide30s-${VERSION}-release.apk"
cp app/build/outputs/bundle/release/app-release.aab "dist/Divide30s-${VERSION}.aab"

echo
echo ">> Artefatos gerados:"
ls -lh dist/ | tail -n +2
echo
echo ">> SHA-256:"
sha256sum dist/*.apk dist/*.aab
