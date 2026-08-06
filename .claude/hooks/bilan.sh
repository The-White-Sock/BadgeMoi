#!/bin/bash
# Hook Stop : rappelle en fin de tour ce qui reste à faire du travail entamé.
#
# Le dépôt a un rituel de livraison complet (`/pousser`), mais rien ne le
# déclenche : si personne ne tape la commande, aucune vérification n'a lieu et la
# convention ne vit que dans de la prose que personne ne relit. Ce hook est le
# déclencheur manquant.
#
# RÈGLE DE CONCEPTION, héritée de `antiseche.sh` : **une seule ligne**, par ordre
# de priorité, et silence complet quand l'arbre est propre. Un rappel qui se
# répète à chaque tour cesse d'être lu au troisième.
#
# D'où le mémo `.git/badgemoi-bilan` : on ne redit une chose que si l'état a
# changé depuis la dernière fois qu'on l'a dite.
#
# JAMAIS BLOQUANT. Un `decision: "block"` relancerait le tour, et un tour relancé
# se retermine par un nouveau Stop : la boucle est immédiate. On informe, on
# n'impose pas — le refus reste réservé à `avant-livraison.sh`.
#
# Entrée : JSON sur stdin (`.stop_hook_active`, `.permission_mode`, `.cwd`).
# Sortie : `systemMessage` (pour la personne) + `additionalContext` (pour Claude).
# Retour : toujours 0.
set -uo pipefail

export LC_ALL=C.UTF-8
export LANG=C.UTF-8

entree="$(cat)"

# Ceinture anti-boucle : même sans `decision: "block"`, on ne rejoue pas un bilan
# sur un tour que ce hook a lui-même prolongé.
[ "$(printf '%s' "${entree}" | jq -r '.stop_hook_active // false' 2>/dev/null)" = "true" ] && exit 0

# En mode plan, rien n'est écrit : il n'y a rien à vérifier ni à livrer.
[ "$(printf '%s' "${entree}" | jq -r '.permission_mode // empty' 2>/dev/null)" = "plan" ] && exit 0

cwd="$(printf '%s' "${entree}" | jq -r '.cwd // empty' 2>/dev/null || true)"
cd "${cwd:-${CLAUDE_PROJECT_DIR:-.}}" 2>/dev/null || exit 0

gitdir="$(git rev-parse --git-dir 2>/dev/null)" || exit 0
jalon="${gitdir}/badgemoi-qualite"
memo="${gitdir}/badgemoi-bilan"

etat="$(git status --porcelain 2>/dev/null || true)"

# Sources modifiées non couvertes par le dernier passage de la qualité.
# Sans jalon, toute source modifiée compte comme non vérifiée.
kt_a_verifier=0
while IFS= read -r ligne; do
  [ -z "${ligne}" ] && continue
  fichier="${ligne:3}"
  case "${fichier}" in
    *.kt | *.kts) ;;
    *) continue ;;
  esac
  # Le renommage « ancien -> nouveau » : seule la destination existe sur disque.
  case "${fichier}" in *' -> '*) fichier="${fichier##* -> }" ;; esac
  [ -e "${fichier}" ] || continue
  if [ ! -e "${jalon}" ] || [ -n "$(find "${fichier}" -newer "${jalon}" 2>/dev/null)" ]; then
    kt_a_verifier=$((kt_a_verifier + 1))
  fi
done <<< "${etat}"

# Commits en avance sur la ref distante. Sans upstream, la branche n'a jamais été
# poussée : on se compare alors à `origin/main`.
avance="$(git rev-list --count '@{u}..HEAD' 2>/dev/null || true)"
jamais_poussee=0
if [ -z "${avance}" ]; then
  jamais_poussee=1
  avance="$(git rev-list --count 'origin/main..HEAD' 2>/dev/null || echo 0)"
fi

# Une seule ligne, par ordre de priorité : on ne peut pas livrer ce qui n'est pas
# vérifié, ni pousser ce qui n'est pas commité.
message=""
if [ "${kt_a_verifier}" -gt 0 ]; then
  message="Qualité non relancée depuis la dernière modification (${kt_a_verifier} fichier(s) Kotlin) → \`/qualite\`."
elif [ -n "${etat}" ]; then
  message="Travail vérifié mais non commité → \`/pousser\` fait la séquence complète."
elif [ "${avance}" -gt 0 ] 2>/dev/null; then
  if [ "${jamais_poussee}" -eq 1 ]; then
    message="${avance} commit(s) sur une branche jamais poussée → \`/pousser\`."
  else
    message="${avance} commit(s) en avance sur l'origine → \`/pousser\` (et l'auto-merge à armer)."
  fi
fi

[ -z "${message}" ] && { rm -f "${memo}"; exit 0; }

# Ne rien redire tant que l'état n'a pas bougé.
if [ -e "${memo}" ] && [ "$(cat "${memo}" 2>/dev/null)" = "${message}" ]; then
  exit 0
fi
printf '%s' "${message}" > "${memo}" 2>/dev/null || true

jq -n --arg m "Bilan BadgeMoi — ${message}" \
  '{systemMessage: $m,
    hookSpecificOutput: {hookEventName: "Stop", additionalContext: $m}}'
exit 0
