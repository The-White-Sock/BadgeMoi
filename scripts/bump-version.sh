#!/bin/bash
# Met à jour versionName/versionCode dans app/build.gradle.kts.
# Appelé par semantic-release (voir .releaserc.js, plugin @semantic-release/exec)
# avec le nouveau numéro de version semver en argument, ex: ./bump-version.sh 0.2.0
set -euo pipefail

NEW_VERSION_NAME="${1:?Usage: bump-version.sh <versionName>}"
BUILD_FILE="app/build.gradle.kts"

CURRENT_VERSION_CODE="$(grep -oP 'versionCode = \K\d+' "${BUILD_FILE}")"
NEW_VERSION_CODE=$((CURRENT_VERSION_CODE + 1))

sed -i \
  -e "s/versionCode = ${CURRENT_VERSION_CODE}/versionCode = ${NEW_VERSION_CODE}/" \
  -e "s/versionName = \"[^\"]*\"/versionName = \"${NEW_VERSION_NAME}\"/" \
  "${BUILD_FILE}"

echo "versionName -> ${NEW_VERSION_NAME}, versionCode -> ${NEW_VERSION_CODE}"
