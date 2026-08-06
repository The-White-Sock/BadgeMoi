#!/bin/bash
# Hook PostToolUse (Bash) : pose un jalon quand les vérifications de qualité ont
# tourné **et réussi**.
#
# Ce script ne dit rien à personne. Il existe pour que `bilan.sh` puisse répondre
# à une question qu'aucune commande git ne sait résoudre : « la qualité a-t-elle
# été relancée depuis la dernière modification ? »
#
# Le jalon vit dans `.git/`, jamais versionné, effacé avec le conteneur.
#
# Un échec **retire** le jalon plutôt que de le poser : une suite rouge doit
# continuer d'appeler le rappel, sinon le garde-fou couvre exactement le cas
# qu'il devait signaler.
#
# Entrée : JSON sur stdin (`.tool_input.command`, `.tool_response`).
# Sortie : rien.
# Retour : toujours 0.
set -uo pipefail

export LC_ALL=C.UTF-8
export LANG=C.UTF-8

entree="$(cat)"
commande="$(printf '%s' "${entree}" | jq -r '.tool_input.command // empty' 2>/dev/null || true)"
[ -z "${commande}" ] && exit 0

printf '%s' "${commande}" \
  | grep -qE 'ktlintCheck|detekt|testDebugUnitTest|assembleDebug' || exit 0

racine="$(git rev-parse --git-dir 2>/dev/null)" || exit 0
jalon="${racine}/badgemoi-qualite"

reponse="$(printf '%s' "${entree}" | jq -r '.tool_response | tostring' 2>/dev/null || true)"

if printf '%s' "${reponse}" | grep -qE 'BUILD FAILED|FAILURE:|What went wrong'; then
  rm -f "${jalon}"
  exit 0
fi

touch "${jalon}"
exit 0
