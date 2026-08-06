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
# SCHÉMA CONNU, REPLI CONSERVÉ. Le journal a livré la forme réelle de l'événement.
# Trois champs sont utiles :
#   - `file_path`   : le fichier d'instructions chargé. **Un seul par événement** —
#                     ce n'est pas une liste, c'est un événement par fichier.
#   - `memory_type` : sa nature. `Project` pour le `CLAUDE.md` racine et les règles
#                     de ce dépôt.
#   - `load_reason` : pourquoi il s'est chargé, parmi `session_start`,
#                     `nested_traversal`, `path_glob_match`, `include`, `compact`.
#
# Le hook journalise malgré tout le JSON **brut**, pas ces trois champs extraits.
# Ce n'est pas de la prudence de principe : c'est ce repli qui a rendu ce hook utile
# dès sa première séance, alors que le schéma était encore inconnu — et c'est lui
# qui a permis de l'apprendre. Un champ ajouté en amont apparaîtra tout seul dans le
# journal ; un hook qui n'écrit que ce qu'il connaît est aveugle à ce qu'il ne
# connaît pas encore. La lecture des champs se fait à l'autre bout, dans `/point`,
# où une forme inattendue coûte une ligne vide et non une donnée perdue.
#
# Entrée : JSON sur stdin (schéma ci-dessus ; repli brut si la forme surprend).
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
