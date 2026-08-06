#!/bin/bash
# Hook UserPromptSubmit : injecte un **pointeur** vers la source qui fait autorité
# sur le sujet détecté dans le prompt.
#
# Complète `.claude/rules/*.md`, qui se déclenchent sur les fichiers lus. Une
# question posée avant d'ouvrir un fichier — « où mettre ce bouton ? » — ne
# déclenche aucune règle : c'est ce trou que ce hook comble.
#
# RÈGLE DE CONCEPTION : des pointeurs, jamais du contenu. Un hook qui déverse du
# texte à chaque tour consomme le budget de contexte qu'il est censé préserver.
# Trois lignes maximum, et silence complet quand rien ne correspond.
#
# Entrée  : JSON sur stdin (champ `.user_input`).
# Sortie  : JSON `hookSpecificOutput.additionalContext`, ou rien.
# Retour  : toujours 0 — ce hook ne bloque jamais un prompt.
set -uo pipefail

# Locale forcée **ici**, et pas héritée : `iconv //TRANSLIT` rend « ? » au lieu de
# « e » quand LANG est vide, ce qui est précisément l'état par défaut du conteneur.
# Un hook doit tenir debout sans rien supposer de son environnement.
export LC_ALL=C.UTF-8
export LANG=C.UTF-8

entree="$(cat)"
prompt="$(printf '%s' "${entree}" | jq -r '.user_input // empty' 2>/dev/null || true)"
[ -z "${prompt}" ] && exit 0

# Minuscules et accents repliés : « Écart » et « ecart » doivent matcher pareil.
# `tr` seul ne suffit pas, il ne connaît pas l'UTF-8 multi-octets.
sujet="$(printf '%s' "${prompt}" \
  | iconv -f UTF-8 -t ASCII//TRANSLIT 2>/dev/null \
  | tr '[:upper:]' '[:lower:]' || true)"

# Ceinture : si la translittération a échoué ou rendu des « ? », on retombe sur le
# prompt brut en minuscules ASCII. Les motifs ci-dessous listent donc aussi les
# formes accentuées des mots déclencheurs.
if [ -z "${sujet}" ] || printf '%s' "${sujet}" | grep -q '?'; then
  sujet="$(printf '%s' "${prompt}" | tr '[:upper:]' '[:lower:]')"
fi

pointeurs=()

# Chaque famille : un motif étendu, un pointeur d'une ligne.
ajouter() {
  if printf '%s' "${sujet}" | grep -qE "$1"; then
    pointeurs+=("$2")
  fi
}

ajouter 'ergonomi|pouce|placement|atteignab|zone de|cible tactile|une main' \
  "Placement d'un élément interactif → \`docs/ergonomie.md\` fait autorité (§3 règles, §4 les 48 dp / 8 dp). Le cahier dit quoi afficher, l'ergonomie dit où le poser."

ajouter 'publi|release|f-droid|fdroid|play store|apk|version|semantic-release|tag' \
  "Publication et versioning → \`docs/publication.md\` (squash merge, semantic-release + gitmoji, F-Droid avant Play Store)."

ajouter 'ecart|écart|deroge|déroge|contredi|deja tranch|déjà tranch|decision|décision|cahier des charges' \
  "Un choix qui contredit le cahier se consigne au §9 : ligne du tableau + section de prose + compte + renvoi. La commande \`/ecart\` fait les quatre."

ajouter 'widget|glance|ecran d.accueil|écran d.accueil' \
  "Widget Glance → lot 6, issues #113 (socle) #114 (démarrer) #115 (état en cours) #116 (rafraîchissement)."

ajouter 'wear|montre|poignet|smartwatch' \
  "Wear OS → #122. Décidé : montre de **saisie** seulement (ni historique ni statistiques), file d'attente et non archive, deux canaux de publication. Point ouvert : le transport sans services Google Play."

ajouter 'csv|export|encodage|tableur|sheets|excel' \
  "Export CSV → ASCII pur, sans BOM, accents repliés (§9, écart 13). Séparateur \`;\`, dates ISO, une ligne par jalon."

ajouter 'couleur|theme|thème|palette|jour.*nuit|nuit.*jour|token' \
  "Couleurs → jamais de littéral hors \`ui/theme/\`. \`MaterialTheme.colorScheme\` ou \`BadgeMoiTheme.extendedColors\` (cahier §5)."

ajouter 'module|:domain|multi-module|extraction' \
  "Extraction du module \`:domain\` → #123. Quatre régressions silencieuses à traiter dans le même commit (tests non exécutés, linters non appliqués, CodeQL sauté, catalogue incomplet)."

[ ${#pointeurs[@]} -eq 0 ] && exit 0

# Au-delà de trois familles, le prompt est trop large pour qu'un pointeur aide :
# on se tait plutôt que de rendre le rappel illisible.
if [ ${#pointeurs[@]} -gt 3 ]; then
  exit 0
fi

contexte="Antisèche BadgeMoi — sources qui font autorité sur les sujets détectés :"
for p in "${pointeurs[@]}"; do
  contexte+=$'\n'"- ${p}"
done

jq -n --arg c "${contexte}" \
  '{hookSpecificOutput: {hookEventName: "UserPromptSubmit", additionalContext: $c}}'
