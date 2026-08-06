#!/bin/bash
# Tests des hooks de `.claude/hooks/`.
#
# POURQUOI CE FICHIER EXISTE : `antiseche.sh` a lu le champ `.user_input` alors que
# le harnais envoie `.prompt`. Le hook est donc sorti en silence à chaque tour,
# pendant toute sa vie, sans que rien ne le signale — un hook muet est
# indistinguable d'un hook qui n'avait rien à dire. Ces tests figent le contrat
# d'entrée/sortie de chaque hook pour que le prochain glissement se voie.
#
# Aucun effet de bord sur le dépôt : chaque cas passe par stdin et on ne lit que
# la sortie standard. Les seuls fichiers touchés sont les jalons dans `.git/`,
# restaurés à la fin.
#
# Usage : ./scripts/test-hooks.sh   (0 si tout passe, 1 sinon)
set -uo pipefail

export LC_ALL=C.UTF-8
export LANG=C.UTF-8

racine="$(cd "$(dirname "$0")/.." && pwd)"
hooks="${racine}/.claude/hooks"
cd "${racine}" || exit 1

reussis=0
echecs=0

# cas <description> <hook> <json> <motif attendu | VIDE>
#   VIDE  : le hook doit se taire complètement.
#   motif : ERE cherchée dans la sortie.
cas() {
  local description="$1" hook="$2" entree="$3" attendu="$4"
  local sortie

  # Sans cette garde, un hook absent ou non exécutable ne rend rien — donc tous
  # les cas « VIDE » passeraient au vert. Un test qui réussit parce qu'il n'a
  # rien exécuté est pire que pas de test.
  if [ ! -x "${hooks}/${hook}.sh" ]; then
    echecs=$((echecs + 1))
    printf '  ÉCHEC  %s\n         hook absent ou non exécutable : %s\n' \
      "${description}" "${hooks}/${hook}.sh"
    return
  fi

  sortie="$(printf '%s' "${entree}" | "${hooks}/${hook}.sh" 2>/dev/null)"

  if [ "${attendu}" = "VIDE" ]; then
    if [ -z "${sortie}" ]; then
      reussis=$((reussis + 1))
      return
    fi
    echecs=$((echecs + 1))
    printf '  ÉCHEC  %s\n         attendu : silence\n         obtenu  : %s\n' \
      "${description}" "${sortie:0:120}"
    return
  fi

  if printf '%s' "${sortie}" | grep -qE "${attendu}"; then
    reussis=$((reussis + 1))
    return
  fi
  echecs=$((echecs + 1))
  printf '  ÉCHEC  %s\n         attendu : %s\n         obtenu  : %s\n' \
    "${description}" "${attendu}" "${sortie:0:120}"
}

echo "antiseche.sh"
# Le test qui aurait attrapé le bug d'origine.
cas "lit le champ .prompt" antiseche \
  '{"prompt":"question de placement pour le pouce","permission_mode":"default"}' \
  'ergonomie\.md'
cas "repli sur .user_input" antiseche \
  '{"user_input":"question de placement pour le pouce"}' \
  'ergonomie\.md'
cas "intention : fonctionnalité" antiseche \
  '{"prompt":"ajoute un ecran de statistiques","permission_mode":"default"}' \
  '/cadrer'
cas "intention tue en mode plan" antiseche \
  '{"prompt":"ajoute un ecran de statistiques","permission_mode":"plan"}' \
  VIDE
cas "intention : livraison" antiseche \
  '{"prompt":"on pousse le travail","permission_mode":"default"}' \
  '/pousser'
cas "commande slash ignorée" antiseche \
  '{"prompt":"/pousser 42","permission_mode":"default"}' \
  VIDE
cas "silence quand rien ne matche" antiseche \
  '{"prompt":"bonjour","permission_mode":"default"}' \
  VIDE
cas "prompt trop large : sujets lâchés, intention gardée" antiseche \
  '{"prompt":"on reprend : couleur du widget, export csv, module domain, publication f-droid","permission_mode":"default"}' \
  '/point'

echo "avant-livraison.sh"
# `ask` et non `deny` : un commit de travail voué au rebase est légitime, et le
# hook ne sait pas l'en distinguer. Le garde-fou avertit, la personne tranche.
cas "rend la main sur un commit sans gitmoji" avant-livraison \
  '{"tool_name":"Bash","tool_input":{"command":"git commit -m \"ajoute un truc\""}}' \
  '"permissionDecision": *"ask"'
cas "laisse passer un commit gitmoji" avant-livraison \
  '{"tool_name":"Bash","tool_input":{"command":"git commit -m \"✨(ui) : ajoute un ecran\""}}' \
  VIDE
cas "se tait sur un message illisible (-F)" avant-livraison \
  '{"tool_name":"Bash","tool_input":{"command":"git commit -F message.txt"}}' \
  VIDE
cas "se tait sur --amend --no-edit" avant-livraison \
  '{"tool_name":"Bash","tool_input":{"command":"git commit --amend --no-edit"}}' \
  VIDE
cas "refuse un push sur main" avant-livraison \
  '{"tool_name":"Bash","tool_input":{"command":"git push origin main"}}' \
  '"permissionDecision": *"deny"'
cas "refuse un push HEAD:main" avant-livraison \
  '{"tool_name":"Bash","tool_input":{"command":"git push -u origin HEAD:main"}}' \
  '"permissionDecision": *"deny"'
cas "laisse passer une branche de travail" avant-livraison \
  '{"tool_name":"Bash","tool_input":{"command":"git push -u origin claude/une-branche"}}' \
  VIDE
# Le piège : `main` en sous-chaîne d'un nom de branche, et `main` dans une
# commande enchaînée après le push.
cas "ne confond pas claude/main-refactor" avant-livraison \
  '{"tool_name":"Bash","tool_input":{"command":"git push -u origin claude/main-refactor"}}' \
  VIDE
cas "ne confond pas un checkout main enchaîné" avant-livraison \
  '{"tool_name":"Bash","tool_input":{"command":"git push -u origin ma-branche && git checkout main"}}' \
  VIDE
cas "refuse une fermeture d'issue en français" avant-livraison \
  '{"tool_name":"mcp__github__create_pull_request","tool_input":{"body":"Ce travail ferme #42."}}' \
  '"permissionDecision": *"deny"'
cas "laisse passer Closes #N" avant-livraison \
  '{"tool_name":"mcp__github__create_pull_request","tool_input":{"body":"Livre le widget.\n\nCloses #42"}}' \
  VIDE
cas "ignore les outils non concernés" avant-livraison \
  '{"tool_name":"Read","tool_input":{"file_path":"/x"}}' \
  VIDE

echo "bilan.sh"
gitdir="$(git rev-parse --git-dir 2>/dev/null || echo .git)"
memo="${gitdir}/badgemoi-bilan"
sauvegarde=""
[ -e "${memo}" ] && sauvegarde="$(cat "${memo}")"

rm -f "${memo}"
cas "se tait en mode plan" bilan \
  "{\"cwd\":\"${racine}\",\"permission_mode\":\"plan\"}" \
  VIDE
cas "se tait si stop_hook_active" bilan \
  "{\"cwd\":\"${racine}\",\"permission_mode\":\"default\",\"stop_hook_active\":true}" \
  VIDE

rm -f "${memo}"
if [ -n "$(git status --porcelain 2>/dev/null)" ]; then
  cas "signale un arbre sale" bilan \
    "{\"cwd\":\"${racine}\",\"permission_mode\":\"default\"}" \
    'systemMessage'
  cas "ne se répète pas à état constant" bilan \
    "{\"cwd\":\"${racine}\",\"permission_mode\":\"default\"}" \
    VIDE
else
  cas "se tait sur un arbre propre et à jour" bilan \
    "{\"cwd\":\"${racine}\",\"permission_mode\":\"default\"}" \
    VIDE
fi

rm -f "${memo}"
[ -n "${sauvegarde}" ] && printf '%s' "${sauvegarde}" > "${memo}"

echo "instructions-chargees.sh"
# La forme d'entrée est désormais connue — relevée au journal, pas lue dans une doc.
# Ces cas figent donc deux choses distinctes, et il ne faut pas confondre leur rôle :
# que les trois champs observés traversent le hook intacts, ET que rien de ce qu'il
# reçoit ne le fasse échouer, y compris une forme qu'on n'a jamais vue. Le second
# groupe garde toute sa raison d'être : les noms de champs sont observés, pas
# spécifiés, donc c'est le repli brut qui reste la garantie.
journal="${gitdir}/badgemoi-instructions.log"
journal_sauve=""
[ -e "${journal}" ] && journal_sauve="$(cat "${journal}")"
rm -f "${journal}"

# Forme réelle, telle qu'observée en séance.
cas "ne écrit rien sur stdout" instructions-chargees \
  '{"session_id":"S","hook_event_name":"InstructionsLoaded","file_path":"/r/CLAUDE.md","memory_type":"Project","load_reason":"session_start"}' \
  VIDE
cas "encaisse un schéma inattendu" instructions-chargees \
  '{"un_champ_jamais_vu":["a","b"]}' \
  VIDE
cas "encaisse une entrée non-JSON" instructions-chargees \
  'ceci nest pas du json' \
  VIDE
cas "encaisse une entrée vide" instructions-chargees \
  '' \
  VIDE

if [ "$(wc -l < "${journal}" 2>/dev/null || echo 0)" -eq 3 ]; then
  reussis=$((reussis + 1))
else
  echecs=$((echecs + 1))
  echo "  ÉCHEC  journalise une ligne par événement non vide (3 attendues, $(wc -l < "${journal}" 2>/dev/null || echo 0) obtenues)"
fi

if grep -q 'un_champ_jamais_vu' "${journal}" 2>/dev/null; then
  reussis=$((reussis + 1))
else
  echecs=$((echecs + 1))
  echo "  ÉCHEC  le journal conserve le JSON reçu tel quel"
fi

# Les trois champs que `/point` interroge doivent traverser intacts. Si l'un d'eux
# est renommé en amont, c'est ici que ça se voit — pas six mois plus tard devant un
# journal qu'on ne sait plus lire.
manquants=""
for champ in file_path memory_type load_reason; do
  grep -q "\"${champ}\"" "${journal}" 2>/dev/null || manquants="${manquants} ${champ}"
done
if [ -z "${manquants}" ]; then
  reussis=$((reussis + 1))
else
  echecs=$((echecs + 1))
  echo "  ÉCHEC  le journal porte les champs interrogés par /point (manque :${manquants})"
fi

# Le plafond : une séance longue ne doit pas laisser un journal illisible.
for _ in $(seq 1 520); do
  printf '%s' '{"n":1}' | "${hooks}/instructions-chargees.sh" >/dev/null 2>&1
done
if [ "$(wc -l < "${journal}" 2>/dev/null || echo 0)" -le 500 ]; then
  reussis=$((reussis + 1))
else
  echecs=$((echecs + 1))
  echo "  ÉCHEC  tronque le journal au-delà de 500 lignes"
fi

rm -f "${journal}"
[ -n "${journal_sauve}" ] && printf '%s\n' "${journal_sauve}" > "${journal}"

echo "interrogations de /point"
# Le journal n'est utile que si la commande sait le lire. On éprouve donc les trois
# pipelines de `.claude/commands/point.md` sur un journal fabriqué, contenant
# **exprès** une ligne de repli brut au milieu des lignes JSON : c'est le cas que
# `fromjson?` protège, et celui qui casserait un `jq` naïf.
temoin="$(mktemp)"
{
  printf '%s\t%s\n' '2026-01-01T00:00:00Z' \
    '{"session_id":"S1","file_path":"/r/CLAUDE.md","memory_type":"Project","load_reason":"session_start"}'
  printf '%s\t%s\n' '2026-01-01T00:00:01Z' 'ligne brute, pas du JSON'
  printf '%s\t%s\n' '2026-01-01T00:00:02Z' \
    '{"session_id":"S2","file_path":"/r/.claude/rules/ui-compose.md","memory_type":"Project","load_reason":"path_glob_match"}'
} > "${temoin}"

session="$(tail -1 "${temoin}" | cut -f2 | jq -r '.session_id // empty')"
seance="$(cut -f2 "${temoin}" | jq -rR --arg s "${session}" \
  'fromjson? | select(.session_id == $s) | "\(.load_reason)\t\(.file_path | split("/") | last)"' \
  | sort | uniq -c)"
if printf '%s' "${seance}" | grep -q 'path_glob_match.*ui-compose\.md' \
  && [ "$(printf '%s\n' "${seance}" | wc -l)" -eq 1 ]; then
  reussis=$((reussis + 1))
else
  echecs=$((echecs + 1))
  echo "  ÉCHEC  isole les chargements de la séance courante (obtenu : ${seance})"
fi

cumul="$(cut -f2 "${temoin}" | jq -rR 'fromjson? | .load_reason // "inconnue"' | sort | uniq -c)"
if [ "$(printf '%s\n' "${cumul}" | wc -l)" -eq 2 ]; then
  reussis=$((reussis + 1))
else
  echecs=$((echecs + 1))
  echo "  ÉCHEC  compte par raison en ignorant la ligne brute (obtenu : ${cumul})"
fi

jamais="$(comm -13 \
  <(cut -f2 "${temoin}" | jq -rR 'fromjson? | .file_path // empty' | sed 's|.*/||' | sort -u) \
  <(ls .claude/rules/*.md | sed 's|.*/||' | sort))"
# Deux côtés, sinon un pipeline muet passerait au vert : la règle présente au
# journal ne doit pas sortir, et une règle absente doit sortir.
if ! printf '%s' "${jamais}" | grep -q 'ui-compose\.md' \
  && printf '%s' "${jamais}" | grep -q 'tests\.md'; then
  reussis=$((reussis + 1))
else
  echecs=$((echecs + 1))
  echo "  ÉCHEC  distingue les règles jamais chargées de celles présentes au journal (obtenu : ${jamais})"
fi

rm -f "${temoin}"

echo "jalon-qualite.sh"
jalon="${gitdir}/badgemoi-qualite"
jalon_sauve=0
[ -e "${jalon}" ] && jalon_sauve=1

rm -f "${jalon}"
printf '%s' '{"tool_input":{"command":"./gradlew ktlintCheck"},"tool_response":{"stdout":"BUILD SUCCESSFUL"}}' \
  | "${hooks}/jalon-qualite.sh" >/dev/null 2>&1
if [ -e "${jalon}" ]; then
  reussis=$((reussis + 1))
else
  echecs=$((echecs + 1)); echo "  ÉCHEC  pose le jalon après un build vert"
fi

printf '%s' '{"tool_input":{"command":"./gradlew ktlintCheck"},"tool_response":{"stdout":"FAILURE: Build failed"}}' \
  | "${hooks}/jalon-qualite.sh" >/dev/null 2>&1
if [ -e "${jalon}" ]; then
  echecs=$((echecs + 1)); echo "  ÉCHEC  retire le jalon après un build rouge"
else
  reussis=$((reussis + 1))
fi

rm -f "${jalon}"
[ "${jalon_sauve}" -eq 1 ] && touch "${jalon}"

echo
if [ "${echecs}" -eq 0 ]; then
  echo "${reussis} cas, tous verts."
  exit 0
fi
echo "${reussis} verts, ${echecs} en échec."
exit 1
