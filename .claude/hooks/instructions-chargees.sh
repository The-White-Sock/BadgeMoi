#!/bin/bash
# Hook InstructionsLoaded : journalise quels fichiers d'instructions se chargent,
# quand et pourquoi.
#
# POURQUOI : rien ne confirme aujourd'hui que les cinq règles de `.claude/rules/`
# se chargent réellement. Elles se déclenchent sur le chemin des fichiers ouverts,
# donc leur silence est indistinguable d'une absence d'occurrence — exactement le
# mode de défaillance qui a laissé `antiseche.sh` muette depuis sa création.
#
# Ce que le journal permet de voir :
#   - qu'une règle se charge (ou pas) quand on ouvre un fichier de sa zone ;
#   - sous quelle raison (`session_start`, `nested_traversal`, `path_glob_match`,
#     `include`, `compact`) ;
#   - **combien** de règles se cumulent en même temps. Le budget d'instructions
#     qu'un modèle suit de façon fiable est fini, et la dégradation est uniforme :
#     une instruction faible en plus dégrade aussi les fortes. Le cumul se mesure,
#     il ne s'estime pas.
#
# AGNOSTIQUE AU SCHÉMA, DÉLIBÉRÉMENT. La documentation de cet événement ne publie
# pas son schéma d'entrée : ni le champ portant les fichiers chargés, ni celui
# portant la raison. On ne devine pas un nom de champ — c'est précisément comme ça
# que `antiseche.sh` a lu `.user_input` pendant que le harnais envoyait `.prompt`.
# On journalise donc le JSON **brut**, et c'est le journal qui nous apprendra la
# forme réelle. Toute lecture de champ nommé viendra après, sur preuve, et sera
# notée ici.
#
# Entrée : JSON sur stdin, de forme inconnue.
# Sortie : rien — la doc donne la sortie de cet événement comme ignorée.
# Retour : toujours 0.
set -uo pipefail

export LC_ALL=C.UTF-8
export LANG=C.UTF-8

entree="$(cat)"
[ -z "${entree}" ] && exit 0

gitdir="$(git rev-parse --git-dir 2>/dev/null)" || exit 0
journal="${gitdir}/badgemoi-instructions.log"

# `jq -c` normalise et prouve que l'entrée est du JSON ; sinon on garde la ligne
# brute, qui reste la donnée la plus utile quand la forme surprend.
compacte="$(printf '%s' "${entree}" | jq -c . 2>/dev/null)" || compacte=""
[ -z "${compacte}" ] && compacte="$(printf '%s' "${entree}" | tr -d '\n')"

printf '%s\t%s\n' "$(date -u +%Y-%m-%dT%H:%M:%SZ)" "${compacte}" >> "${journal}" 2>/dev/null || exit 0

# Plafond : une séance longue ne doit pas laisser un journal illisible. On garde
# les dernières lignes, ce sont celles de la séance en cours.
lignes="$(wc -l < "${journal}" 2>/dev/null || echo 0)"
if [ "${lignes}" -gt 500 ] 2>/dev/null; then
  tail -n 400 "${journal}" > "${journal}.tmp" 2>/dev/null \
    && mv "${journal}.tmp" "${journal}" 2>/dev/null
fi

exit 0
