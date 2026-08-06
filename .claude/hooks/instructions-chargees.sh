#!/bin/bash
# Hook InstructionsLoaded : journalise quels fichiers d'instructions se chargent,
# quand et pourquoi.
#
# POURQUOI : les cinq règles de `.claude/rules/` se déclenchent sur le chemin des
# fichiers ouverts, donc leur silence est indistinguable d'une absence d'occurrence
# — exactement le mode de défaillance qui a laissé `antiseche.sh` muette depuis sa
# création. Le journal a depuis tranché la question qui l'avait fait naître : les
# cinq se chargent bien, sur `path_glob_match`. Il reste utile pour situer la
# fenêtre de contexte courante, que rien d'autre ne donne à voir.
#
# Ce que le journal permet de voir :
#   - qu'une règle se charge (ou pas) quand on ouvre un fichier de sa zone ;
#   - sous quelle raison (`session_start`, `nested_traversal`, `path_glob_match`,
#     `include`). Une compaction réelle a été observée journalisant `session_start`
#     sur `CLAUDE.md` : **il n'y a pas de raison `compact`** à attendre ici, et un
#     `session_start` en cours de séance est la borne d'une fenêtre de contexte ;
#   - **combien** de règles se cumulent en même temps. Le budget d'instructions
#     qu'un modèle suit de façon fiable est fini, et la dégradation est uniforme :
#     une instruction faible en plus dégrade aussi les fortes. Le cumul se mesure,
#     il ne s'estime pas.
#
# SCHÉMA RELEVÉ AU JOURNAL, PAS LU DANS UNE SPEC. La documentation de cet événement
# ne publie toujours pas sa forme d'entrée. Les trois champs utiles ci-dessous ont
# été observés en séance, ce qui est une preuve mais pas une garantie :
#
#   file_path    le fichier chargé — **un seul par événement**, jamais une liste ;
#   memory_type  `Project` pour tout ce que porte ce dépôt ;
#   load_reason  `session_start` pour `CLAUDE.md`, `path_glob_match` pour une règle
#                de `.claude/rules/` déclenchée par un chemin ouvert.
#
# Trois relevés au passage, tous contre-intuitifs :
#   - le glob se déclenche sur le **chemin visé**, pas sur l'existence du fichier.
#     Lire un chemin inexistant de la zone charge quand même la règle — c'est ce qui
#     rend le remède post-`/compact` (rouvrir un fichier de la zone avant d'y écrire)
#     gratuit ;
#   - le chargement est dédupliqué par **fenêtre de contexte**, et non par séance.
#     Rouvrir un second fichier de la même zone n'émet rien de plus, mais une
#     compaction remet ce compteur à zéro : la même règle se recharge alors sous le
#     même `session_id`. C'est ce qui rend le remède post-compaction opérant — sans
#     cette remise à zéro, rouvrir un fichier de la zone n'aurait rien rechargé.
#     Conséquence pour la lecture : compter sur toute la séance additionne des
#     fenêtres révolues, il faut se borner à la dernière ;
#   - le journal est **écrit dans `.git/`, donc il meurt avec le conteneur**. En
#     session web le dépôt est recloné à neuf et le fichier repart vide : le cumul
#     « toutes séances » ne couvre que les séances de ce conteneur. C'est voulu —
#     un journal versionné polluerait chaque diff — mais ça se dit, sinon un cumul
#     en baisse se lit comme une régression du harnais. Aucun cumul ne mesure le
#     budget d'instructions, cela dit — ni celui du conteneur ni celui de la séance,
#     puisque tous deux additionnent des fenêtres révolues. Seule la fenêtre courante
#     le fait.
#
# LE REPLI BRUT RESTE, et ce n'est pas de la prudence de façade : ces noms viennent
# d'une observation, donc ils peuvent changer sans préavis ni avertissement. On
# journalise le JSON **entier** plutôt que trois champs extraits ; ce fichier ne lit
# aucun champ nommé, c'est `/point` qui interroge. Le jour où la forme change, le
# journal continue de dire la vérité et seule la commande est à reprendre. C'est
# l'inverse du bug qui a fait lire `.user_input` à `antiseche.sh` pendant toute sa
# vie, pendant que le harnais envoyait `.prompt`.
#
# Entrée : JSON sur stdin, forme ci-dessus, sans garantie.
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
