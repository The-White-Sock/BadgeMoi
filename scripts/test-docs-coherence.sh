#!/bin/bash
# Tests de `scripts/check-docs-coherence.sh`.
#
# POURQUOI CE FICHIER EXISTE : ce contrôle est le seul garde-fou automatique sur la
# documentation et sur le budget d'instructions, et il n'avait aucune couverture. Ses
# trois épreuves de budget ont été refaites **à la main**, en bac à sable hors dépôt,
# deux séances de suite — une vérification qu'on refait à la main est une vérification
# qu'on finira par ne plus faire. Elles sont figées ici.
#
# Le mode de panne à surveiller est celui d'un contrôle **muet** : un contrôle qui ne
# mord plus rend le même « aucun écart détecté » qu'un dépôt sain. Chaque contrôle a
# donc son cas qui mord, et ceux qui ont un revers (commentaire HTML, règle scopée,
# motif de glob légitime) ont aussi leur cas qui doit rester silencieux.
#
# Aucun effet de bord sur le dépôt : tout se passe dans des copies jetables d'une
# archive de `HEAD`. Le travail en cours n'est donc **pas** éprouvé — c'est délibéré,
# on teste le contrôle, pas l'état de l'arbre.
#
# Usage : ./scripts/test-docs-coherence.sh   (0 si tout passe, 1 sinon)
set -uo pipefail

export LC_ALL=C.UTF-8
export LANG=C.UTF-8

racine="$(cd "$(dirname "$0")/.." && pwd)"
cd "${racine}" || exit 1

reussis=0
echecs=0

# --- Bac de référence --------------------------------------------------------
# Une archive de `HEAD`, **réindexée**. Le `git init && git add` n'est pas cosmétique :
# le contrôle de motif mort éprouve les globs sur `git ls-files`. Sans index cette
# liste est vide, toutes les règles sont déclarées mortes, et le cas vierge partirait
# au rouge pendant que les autres passeraient au vert pour la mauvaise raison.
reference="$(mktemp -d)/reference"
mkdir -p "${reference}"
if ! git archive HEAD | tar -x -C "${reference}"; then
  echo "ÉCHEC  impossible d'archiver HEAD — aucun cas n'a été éprouvé."
  exit 1
fi
git -C "${reference}" init -q
git -C "${reference}" add -A

if [ ! -x "${reference}/scripts/check-docs-coherence.sh" ]; then
  echo "ÉCHEC  le contrôle n'est pas exécutable dans l'archive — aucun cas n'a été éprouvé."
  rm -rf "$(dirname "${reference}")"
  exit 1
fi

# 200 lignes de prose : de quoi faire mordre le budget de lancement à coup sûr, quelle
# que soit la taille courante de `CLAUDE.md`.
remplissage() {
  local i
  for i in $(seq 1 200); do
    echo "Ligne de remplissage numero ${i} pour eprouver le budget de lancement."
  done
}

# cas <description> <mutation shell, exécutée dans le bac> <motif attendu | AUCUN>
#   AUCUN : le contrôle doit sortir 0 en annonçant qu'il n'a rien trouvé.
#   motif : ERE cherchée dans le rapport, avec une sortie 1.
#
# Les deux branches vérifient **le code de sortie et le texte**. Un contrôle qui
# n'aurait pas tourné du tout rend une sortie vide avec un code non nul : il échoue
# donc des deux côtés, au lieu de faire passer au vert tous les cas « AUCUN ».
cas() {
  local description="$1" mutation="$2" attendu="$3"
  local bac sortie code

  bac="$(mktemp -d)/depot"
  cp -a "${reference}" "${bac}"

  if ! ( cd "${bac}" && eval "${mutation}" ) >/dev/null 2>&1; then
    echecs=$((echecs + 1))
    printf '  ÉCHEC  %s\n         la mutation a échoué — ce cas n%séprouve rien\n' \
      "${description}" "'"
    rm -rf "$(dirname "${bac}")"
    return
  fi

  sortie="$(cd "${bac}" && ./scripts/check-docs-coherence.sh 2>&1)"
  code=$?
  rm -rf "$(dirname "${bac}")"

  if [ "${attendu}" = "AUCUN" ]; then
    if [ "${code}" -eq 0 ] && grep -qE 'aucun écart détecté' <<< "${sortie}"; then
      reussis=$((reussis + 1))
      return
    fi
    echecs=$((echecs + 1))
    printf '  ÉCHEC  %s\n         attendu : aucun écart (sortie 0)\n         obtenu  : (%s) %s\n' \
      "${description}" "${code}" "${sortie:0:200}"
    return
  fi

  if [ "${code}" -ne 0 ] && grep -qE "${attendu}" <<< "${sortie}"; then
    reussis=$((reussis + 1))
    return
  fi
  echecs=$((echecs + 1))
  printf '  ÉCHEC  %s\n         attendu : %s (sortie 1)\n         obtenu  : (%s) %s\n' \
    "${description}" "${attendu}" "${code}" "${sortie:0:200}"
}

# --- Le témoin négatif -------------------------------------------------------
# Sans lui, un contrôle devenu bavard ferait passer au vert tous les cas ci-dessous.
echo "dépôt intact"
cas "l'archive de HEAD ne déclenche rien" ':' AUCUN

# --- 1. Versions de la stack -------------------------------------------------
echo "versions de la stack"
cas "version Kotlin désaccordée avec le catalogue" \
  'sed -i "s/Kotlin 2\.4\.0/Kotlin 2.4.2/" docs/conventions.md' \
  'Version Kotlin'
cas "ligne « stack » disparue de la doc" \
  'sed -i "/Jetpack Compose + Material 3, AGP/d" docs/conventions.md' \
  'stack.*est introuvable'
# Doc et catalogue restent d'accord : seul le plafond CodeQL doit parler.
cas "plafond CodeQL franchi" \
  'sed -i "s/^kotlin = \"2\.4\.0\"/kotlin = \"2.4.10\"/" gradle/libs.versions.toml
   sed -i "s/Kotlin 2\.4\.0/Kotlin 2.4.10/" docs/conventions.md' \
  'Contrainte CodeQL'

# --- 2. Fichiers et liens référencés -----------------------------------------
echo "fichiers et liens référencés"
cas "chemin cité entre backticks, absent du dépôt" \
  'echo "Voir \`docs/inexistant.md\` pour la suite." >> README.md' \
  'Fichier référencé introuvable'
cas "lien Markdown vers une cible absente" \
  'echo "Voir [la suite](docs/absent-du-depot.md)." >> README.md' \
  'Lien cassé'

# --- 3. Cohérence interne du §9 ----------------------------------------------
# Le §9 se tient en trois endroits : une ligne de tableau, une section de prose, un
# compte annoncé. Chacun peut dériver sans les deux autres, d'où trois cas.
echo "cohérence du §9"
cas "décision au tableau, sans section de prose" \
  'echo "| 99 | Décision témoin | Valeur témoin |" >> docs/cahier-des-charges.md' \
  'sans justification'
cas "section de prose, sans ligne au tableau" \
  'echo "**98. Écart témoin.** Corps de l écart témoin." >> docs/cahier-des-charges.md' \
  'hors tableau'
# Tableau et prose d'accord, mais un écart de plus que le compte annoncé.
cas "compte des écarts resté en arrière" \
  'echo "| 97 | Décision témoin | Valeur témoin |" >> docs/cahier-des-charges.md
   echo "**97. Écart témoin.** Corps de l écart témoin." >> docs/cahier-des-charges.md' \
  'Compte des écarts faux'

# --- 4a. Budget de lancement -------------------------------------------------
# Les trois épreuves refaites à la main deux séances de suite, et leurs revers. Le
# budget porte sur ce qui est **réellement injecté** à chaque lancement : ni le
# frontmatter, ni les commentaires HTML, ni les règles scopées n'en font partie.
echo "budget de lancement"
cas "CLAUDE.md gonflé de prose" \
  'remplissage >> CLAUDE.md' \
  'Chargement de session trop lourd'
cas "les mêmes lignes en commentaire HTML sont gratuites" \
  '{ echo "<!--"; remplissage; echo "-->"; } >> CLAUDE.md' \
  AUCUN
cas "règle non scopée comptée avec CLAUDE.md" \
  'remplissage > .claude/rules/temoin.md' \
  'Chargement de session trop lourd'
cas "les mêmes lignes derrière paths: sortent du budget" \
  '{ printf -- "---\npaths:\n  - \"scripts/*\"\n---\n"; remplissage; } > .claude/rules/temoin.md' \
  AUCUN

# --- 4b. Motifs de glob morts ------------------------------------------------
# Une règle dont le glob ne matche plus rien est morte **en silence**. Le revers
# compte autant : une traduction glob → regex trop stricte condamnerait des motifs
# légitimes, et `**` est justement ce que `find -path` ne sait pas exprimer.
echo "motifs de glob"
cas "motif qui ne correspond à aucun fichier suivi" \
  'printf -- "---\npaths:\n  - \"app/inexistant/**\"\n---\n\nCorps.\n" > .claude/rules/temoin.md' \
  'Règle Claude Code inopérante'
cas "motif à double astérisque, légitime" \
  'printf -- "---\npaths:\n  - \"app/**/*.kt\"\n---\n\nCorps.\n" > .claude/rules/temoin.md' \
  AUCUN

# --- Rapport -----------------------------------------------------------------
rm -rf "$(dirname "${reference}")"

echo
if [ "${echecs}" -eq 0 ]; then
  echo "${reussis} cas, tous verts."
  exit 0
fi
echo "${reussis} verts, ${echecs} en échec."
exit 1
