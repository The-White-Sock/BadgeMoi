#!/bin/bash
# Bibliothèque **sourcée** par les hooks — pas un hook, aucun événement ne la lance.
#
# POURQUOI : les deux batteries prouvent qu'un hook **peut** se déclencher. Elles ne
# disent rien de ce qui se passe en séance réelle, et c'est précisément là que le
# dépôt s'est fait avoir deux fois — `antiseche.sh` muette sur un `.user_input`
# devenu `.prompt`, `garde-fous.sh` privé de sa coupe `*.kt`. Dans les deux cas la
# batterie était verte et le hook inutile.
#
# CE QUE CE JOURNAL TRANCHE, et qui n'est visible nulle part ailleurs : la
# différence entre un contrôle **qui n'a pas tourné** et un contrôle **qui a tourné
# sans rien trouver**. C'est l'ambiguïté que `.claude/rules/harnais.md` désigne comme
# le mode de défaillance récurrent ici. Un compteur qui les confondrait rouvrirait le
# trou qu'il prétend fermer — d'où trois issues, et non deux :
#
#   hors-perimetre  le hook a tourné, mais rien à examiner (autre type de fichier,
#                   autre outil, entrée vide). Son silence est **normal**.
#   muet            le hook a examiné sa cible et n'a rien trouvé. Son silence est
#                   **un résultat**.
#   alerte          le hook a trouvé quelque chose et l'a dit.
#   commande        une commande slash a été invoquée (mesure d'usage, pas de
#                   contrôle).
#
# Une majorité de `hors-perimetre` là où on attend des `muet` est le symptôme exact
# du défaut de 2026 sur `garde-fous.sh` : la coupe ne mord plus.
#
# CONTRAT DE ROBUSTESSE : cette bibliothèque ne doit jamais faire échouer le hook qui
# la source. Toutes les erreurs sont avalées, le retour est toujours 0, et rien n'est
# écrit sur stdout — un octet de trop y corromprait le JSON que le hook rend au
# harnais.
#
# Le journal vit dans le répertoire git, donc hors de l'arbre de travail : il ne
# pollue aucun diff, et il meurt avec le conteneur. Ce dernier point est voulu et se
# dit, sinon un cumul en baisse se lit comme une régression du harnais.

# Résolution par `git rev-parse`, jamais `.git/` en dur : dans un worktree lié,
# `.git` est un fichier pointeur et l'écriture échouerait en silence.
journaliser_usage() {
  local hook="${1:-inconnu}"
  local issue="${2:-inconnu}"
  local detail="${3:-}"
  local gitdir journal lignes

  gitdir="$(git rev-parse --git-dir 2>/dev/null)" || return 0
  [ -n "${gitdir}" ] || return 0
  journal="${gitdir}/badgemoi-usage.log"

  # Tabulations comme séparateur, comme le journal d'instructions : `/point`
  # interroge en `awk -F'\t'`. Le détail est nettoyé de ses tabulations et retours
  # à la ligne, qui casseraient le découpage en colonnes.
  detail="$(printf '%s' "${detail}" | tr '\t\n' '  ')"

  printf '%s\t%s\t%s\t%s\n' \
    "$(date -u +%Y-%m-%dT%H:%M:%SZ)" "${hook}" "${issue}" "${detail}" \
    >> "${journal}" 2>/dev/null || return 0

  # Plafond : une séance longue ne doit pas laisser un journal illisible. On garde
  # les dernières lignes, ce sont celles de la séance en cours.
  lignes="$(wc -l < "${journal}" 2>/dev/null || echo 0)"
  if [ "${lignes}" -gt 500 ] 2>/dev/null; then
    tail -n 400 "${journal}" > "${journal}.tmp" 2>/dev/null \
      && mv "${journal}.tmp" "${journal}" 2>/dev/null
  fi

  return 0
}
