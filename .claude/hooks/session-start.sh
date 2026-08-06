#!/bin/bash
# Prépare une session Claude Code on the web : locale UTF-8, puis Android SDK
# command-line (sdkmanager, platform-tools, platforms;android-37.0,
# build-tools;37.0.0) pour que ./gradlew build, test et lint fonctionnent.
#
# Nécessite que la politique réseau de l'environnement autorise
# dl.google.com (voir CLAUDE.md).
set -euo pipefail

if [ "${CLAUDE_CODE_REMOTE:-}" != "true" ]; then
  exit 0
fi

# --- Locale ------------------------------------------------------------------
# Le conteneur démarre avec LANG vide, donc `sun.jnu.encoding = ANSI_X3.4-1968`.
# Le compilateur Kotlin échoue alors à écrire le fichier `.class` d'une lambda
# déclarée dans un test au nom français accentué — et le démon Kotlin hérite de
# la locale de son premier lancement, si bien qu'une seule commande lancée sans
# la variable empoisonne toutes les suivantes.
#
# Écrit **avant** l'installation du SDK, et non à la fin : une panne réseau sur
# dl.google.com interromprait le script (`set -e`) et laisserait la session sans
# locale, c'est-à-dire avec le défaut exact qu'on cherche à corriger.
{
  echo 'export LANG="C.UTF-8"'
  echo 'export LC_ALL="C.UTF-8"'
} >> "${CLAUDE_ENV_FILE}"

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
sdkmanager --install "platform-tools" "platforms;android-37.0" "build-tools;37.0.0" >/dev/null

{
  echo "export ANDROID_HOME=\"${ANDROID_SDK_DIR}\""
  echo "export ANDROID_SDK_ROOT=\"${ANDROID_SDK_DIR}\""
  echo "export PATH=\"${ANDROID_SDK_DIR}/cmdline-tools/latest/bin:${ANDROID_SDK_DIR}/platform-tools:\$PATH\""
} >> "${CLAUDE_ENV_FILE}"

echo "Locale C.UTF-8 et Android SDK prêts dans ${ANDROID_SDK_DIR} (platform-tools, platforms;android-37.0, build-tools;37.0.0)."
