#!/bin/bash
# Installe l'Android SDK command-line (sdkmanager, platform-tools,
# platforms;android-36, build-tools;36.0.0) pour que ./gradlew build,
# test et lint fonctionnent dans les sessions Claude Code on the web.
#
# Nécessite que la politique réseau de l'environnement autorise
# dl.google.com (voir CLAUDE.md).
set -euo pipefail

if [ "${CLAUDE_CODE_REMOTE:-}" != "true" ]; then
  exit 0
fi

ANDROID_SDK_DIR="${HOME}/android-sdk"
CMDLINE_TOOLS_BUILD="15859902"
CMDLINE_TOOLS_ZIP_URL="https://dl.google.com/android/repository/commandlinetools-linux-${CMDLINE_TOOLS_BUILD}_latest.zip"

mkdir -p "${ANDROID_SDK_DIR}"

if [ ! -x "${ANDROID_SDK_DIR}/cmdline-tools/latest/bin/sdkmanager" ]; then
  echo "Téléchargement des Android SDK command-line tools..."
  tmp_zip="$(mktemp)"
  tmp_extract="$(mktemp -d)"
  curl -fsSL -o "${tmp_zip}" "${CMDLINE_TOOLS_ZIP_URL}"
  unzip -q "${tmp_zip}" -d "${tmp_extract}"
  mkdir -p "${ANDROID_SDK_DIR}/cmdline-tools"
  rm -rf "${ANDROID_SDK_DIR}/cmdline-tools/latest"
  mv "${tmp_extract}/cmdline-tools" "${ANDROID_SDK_DIR}/cmdline-tools/latest"
  rm -rf "${tmp_zip}" "${tmp_extract}"
fi

export ANDROID_HOME="${ANDROID_SDK_DIR}"
export ANDROID_SDK_ROOT="${ANDROID_SDK_DIR}"
export PATH="${ANDROID_SDK_DIR}/cmdline-tools/latest/bin:${ANDROID_SDK_DIR}/platform-tools:${PATH}"

yes | sdkmanager --licenses >/dev/null 2>&1 || true
sdkmanager --install "platform-tools" "platforms;android-36" "build-tools;36.0.0" >/dev/null

{
  echo "export ANDROID_HOME=\"${ANDROID_SDK_DIR}\""
  echo "export ANDROID_SDK_ROOT=\"${ANDROID_SDK_DIR}\""
  echo "export PATH=\"${ANDROID_SDK_DIR}/cmdline-tools/latest/bin:${ANDROID_SDK_DIR}/platform-tools:\$PATH\""
} >> "${CLAUDE_ENV_FILE}"

echo "Android SDK prêt dans ${ANDROID_SDK_DIR} (platform-tools, platforms;android-36, build-tools;36.0.0)."
