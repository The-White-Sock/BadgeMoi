#!/bin/bash
# Hook UserPromptSubmit : injecte un **pointeur** vers la source qui fait autorité
# sur le sujet détecté dans le prompt, et **au plus un** vers l'outil qui
# correspond à la forme de la demande.
#
# Complète `.claude/rules/*.md`, qui se déclenchent sur les fichiers lus. Une
# question posée avant d'ouvrir un fichier — « où mettre ce bouton ? » — ne
# déclenche aucune règle : c'est ce trou que ce hook comble.
#
# Deux couches, indépendantes :
#   - INTENTION : la *forme* de la demande (livrer, vérifier, déroger…) et la
#     commande qui l'exécute en entier. Une ligne maximum, premier motif gagnant.
#   - SUJETS    : le *thème* abordé (ergonomie, CSV, widget…) et la source qui
#     fait autorité dessus. Trois lignes maximum.
#
# RÈGLE DE CONCEPTION : des pointeurs, jamais du contenu. Un hook qui déverse du
# texte à chaque tour consomme le budget de contexte qu'il est censé préserver.
# Quatre lignes maximum au total, et silence complet quand rien ne correspond.
#
# Entrée  : JSON sur stdin (champs `.prompt` et `.permission_mode`).
# Sortie  : JSON `hookSpecificOutput.additionalContext`, ou rien.
# Retour  : toujours 0 — ce hook ne bloque jamais un prompt.
set -uo pipefail

# Locale forcée **ici**, et pas héritée : `iconv //TRANSLIT` rend « ? » au lieu de
# « e » quand LANG est vide, ce qui est précisément l'état par défaut du conteneur.
# Un hook doit tenir debout sans rien supposer de son environnement.
export LC_ALL=C.UTF-8
export LANG=C.UTF-8

entree="$(cat)"

# Le harnais envoie `.prompt`. `.user_input` est conservé en repli : c'est le nom
# que ce hook lisait seul, et il a rendu le hook muet à chaque tour sans que rien
# ne le signale — un hook silencieux est indistinguable d'un hook sans occurrence.
# D'où `scripts/test-hooks.sh`, qui fige désormais ce contrat.
prompt="$(printf '%s' "${entree}" | jq -r '.prompt // .user_input // empty' 2>/dev/null || true)"
[ -z "${prompt}" ] && exit 0

# Une commande slash porte déjà ses propres instructions : lui suggérer un outil
# reviendrait le plus souvent à lui suggérer celui qu'on est en train de lancer.
case "${prompt}" in
  /*) exit 0 ;;
esac

mode="$(printf '%s' "${entree}" | jq -r '.permission_mode // empty' 2>/dev/null || true)"

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

# ---------------------------------------------------------------------------
# Couche 1 — l'intention : quelle commande fait ce travail en entier ?
# ---------------------------------------------------------------------------
# Premier motif gagnant, une seule ligne. Empiler les intentions les rendrait
# contradictoires (« cadre d'abord » et « livre » dans le même souffle), et une
# demande qui en déclenche plusieurs est justement celle qu'aucune ne décrit.
intention=""

retenir() {
  [ -n "${intention}" ] && return 0
  if printf '%s' "${sujet}" | grep -qE "$1"; then
    intention="$2"
  fi
}

# Le mode plan cadre déjà : l'y renvoyer serait du bruit.
if [ "${mode}" != "plan" ]; then
  retenir 'ajoute|implemente|implémente|cree|crée|créé|developpe|développe|refais|remplace|nouvelle fonction|nouvel ecran|nouvel écran' \
    "Demande de fonctionnalité → cadrer avant d'écrire. \`/cadrer\` fixe la spec et les cas limites ; le mode plan fait le reste."
fi

retenir 'pousse|livre|pull request|fusionne|merge|commit' \
  "Livraison → \`/pousser\` fait la séquence complète : qualité, état de branche, commit gitmoji, PR, auto-merge, suivi."

retenir 'teste|verifie|vérifie|lint|ktlint|detekt|compile|ca passe|ça passe' \
  "Vérification → \`/qualite\` lance les quatre tâches Gradle et la cohérence des docs en une passe."

retenir 'finalement|plutot que|plutôt que|au lieu de|on change|deroge|déroge|contredit' \
  "Un choix qui contredit le cahier se consigne au §9 — \`/ecart\` fait les quatre gestes d'un coup."

retenir 'plante|erreur|crash|marche pas|echoue|échoue|exception|stacktrace' \
  "Débogage → coller la **trace complète** et laisser l'approche ouverte ; le diagnostic micro-géré est plus lent que le diagnostic informé."

retenir 'reprend|on continue|ou on en est|où on en est|recap|récap|passation' \
  "Reprise de session → \`/point\` produit la passation (fait, reste, fichiers, décisions) avant un \`/clear\` ou un rewind."

# ---------------------------------------------------------------------------
# Couche 2 — les sujets : quelle source fait autorité ?
# ---------------------------------------------------------------------------
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

# Au-delà de trois familles, le prompt est trop large pour qu'un pointeur de sujet
# aide : on abandonne les sujets plutôt que de rendre le rappel illisible.
# L'intention, elle, survit — elle reste juste quel que soit le nombre de thèmes.
if [ ${#pointeurs[@]} -gt 3 ]; then
  pointeurs=()
fi

[ -z "${intention}" ] && [ ${#pointeurs[@]} -eq 0 ] && exit 0

contexte="Antisèche BadgeMoi :"

if [ -n "${intention}" ]; then
  contexte+=$'\n'"- ${intention}"
fi

if [ ${#pointeurs[@]} -gt 0 ]; then
  contexte+=$'\n'"Sources qui font autorité sur les sujets détectés :"
  for p in "${pointeurs[@]}"; do
    contexte+=$'\n'"- ${p}"
  done
fi

jq -n --arg c "${contexte}" \
  '{hookSpecificOutput: {hookEventName: "UserPromptSubmit", additionalContext: $c}}'
