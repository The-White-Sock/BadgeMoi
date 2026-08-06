#!/bin/bash
# Hook PostToolUse (Edit|Write) : signale les conventions mécaniquement vérifiables
# enfreintes par le fichier qui vient d'être écrit.
#
# CONSULTATIF, JAMAIS BLOQUANT. Sort toujours en 0. ktlint, detekt et la CI restent
# l'arbitre ; ce hook ne fait que raccourcir la boucle de plusieurs minutes à zéro.
# Un faux positif doit coûter une ligne de bruit, pas un travail interrompu.
#
# Entrée  : JSON sur stdin (`.tool_input.file_path`).
# Sortie  : JSON `hookSpecificOutput.additionalContext`, ou rien.
set -uo pipefail

export LC_ALL=C.UTF-8
export LANG=C.UTF-8

entree="$(cat)"
fichier="$(printf '%s' "${entree}" | jq -r '.tool_input.file_path // empty' 2>/dev/null || true)"

[ -z "${fichier}" ] && exit 0
[ -f "${fichier}" ] || exit 0

alertes=()

case "${fichier}" in
  *.kt)
    # 1. Couleur littérale hors du thème. `ui/theme/` est l'endroit où les tokens
    #    sont *définis* : c'est le seul fichier qui a le droit d'en écrire.
    case "${fichier}" in
      */ui/theme/*) ;;
      */ui/*)
        if grep -qE '\bColor\(0x[0-9A-Fa-f]{6,8}\)' "${fichier}"; then
          alertes+=("Couleur littérale détectée hors de \`ui/theme/\`. Passer par \`MaterialTheme.colorScheme\` ou \`BadgeMoiTheme.extendedColors\` (cahier §5).")
        fi
        ;;
    esac

    # 2. Import Android dans le domaine. C'est l'invariant que #123 figera dans le
    #    build ; d'ici là, il ne tient qu'à la relecture.
    case "${fichier}" in
      */domain/*)
        if grep -qE '^import (android|androidx|dagger|javax\.inject)\.' "${fichier}"; then
          alertes+=("Import Android/DI dans \`domain/\` : le domaine doit rester du Kotlin pur. La persistance va en \`data/\`, l'injection en \`di/\` (#123).")
        fi
        ;;
    esac

    # 3. Texte visible en dur. Heuristique volontairement étroite — `Text(` suivi
    #    d'un littéral — pour ne pas confondre avec un `contentDescription` ou une
    #    clé technique. Les aperçus sont exclus : un `@Preview` a le droit à ses
    #    données factices.
    case "${fichier}" in
      */ui/*)
        # On cible le seul motif qui trahit à coup sûr un libellé écrit en dur :
        # `Text(` ou `text =` **immédiatement** suivi d'un littéral d'au moins
        # quatre lettres. Classe ASCII seule — `[A-Za-zÀ-ÿ]` est une plage
        # invalide hors locale UTF-8 et ferait échouer `grep` bruyamment.
        #
        # L'analyse s'arrête au premier `@Preview` : un aperçu a le droit à ses
        # données factices, et les aperçus vivent en fin de fichier dans ce dépôt.
        # Sans cette coupe, `ScreenScaffold` était signalé pour le texte de sa
        # propre démonstration.
        if awk '/@Preview/ { exit } { print }' "${fichier}" \
          | grep -E '(Text\(|text = )"[^"]*[A-Za-z]{4}' \
          | grep -qvE 'stringResource|contentDescription|// '; then
          alertes+=("Texte possiblement en dur dans un composable. Tout libellé visible passe par une ressource de chaîne \`<ecran>_<usage>\`.")
        fi
        ;;
    esac
    ;;
esac

[ ${#alertes[@]} -eq 0 ] && exit 0

contexte="Garde-fou BadgeMoi sur \`${fichier}\` (consultatif — à vérifier, pas à croire sur parole) :"
for a in "${alertes[@]}"; do
  contexte+=$'\n'"- ${a}"
done

jq -n --arg c "${contexte}" \
  '{hookSpecificOutput: {hookEventName: "PostToolUse", additionalContext: $c}}'
