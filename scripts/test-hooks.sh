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
# Le schéma est connu (`file_path`, `memory_type`, `load_reason`) : les deux premiers
# cas l'exercent tel qu'observé, et une assertion vérifie que `/point` sait relire ce
# que le hook écrit — c'est ce couplage-là qui casse en silence. Les cas suivants
# restent agnostiques : le repli brut doit continuer d'encaisser une forme inattendue,
# c'est lui qui a permis d'apprendre le schéma en premier lieu.
journal="${gitdir}/badgemoi-instructions.log"
journal_sauve=""
[ -e "${journal}" ] && journal_sauve="$(cat "${journal}")"
rm -f "${journal}"

cas "ne écrit rien sur stdout" instructions-chargees \
  '{"hook_event_name":"InstructionsLoaded","file_path":"/x/CLAUDE.md","memory_type":"Project","load_reason":"session_start"}' \
  VIDE
cas "encaisse une règle à paths:" instructions-chargees \
  '{"hook_event_name":"InstructionsLoaded","file_path":"/x/.claude/rules/ui-compose.md","memory_type":"Project","load_reason":"path_glob_match"}' \
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

if [ "$(wc -l < "${journal}" 2>/dev/null || echo 0)" -eq 4 ]; then
  reussis=$((reussis + 1))
else
  echecs=$((echecs + 1))
  echo "  ÉCHEC  journalise une ligne par événement non vide (4 attendues, $(wc -l < "${journal}" 2>/dev/null || echo 0) obtenues)"
fi

if grep -q 'un_champ_jamais_vu' "${journal}" 2>/dev/null; then
  reussis=$((reussis + 1))
else
  echecs=$((echecs + 1))
  echo "  ÉCHEC  le journal conserve le JSON reçu tel quel"
fi

# Le contrat que `/point` consomme : ses deux `jq` doivent retrouver les champs dans
# le journal. Sans ça, le hook écrit correctement et la commande lit dans le vide —
# exactement le décalage qui a rendu `antiseche.sh` muette.
raisons="$(cut -f2 "${journal}" | jq -r '.load_reason // empty' 2>/dev/null | sort -u | tr '\n' ' ')"
chemins="$(cut -f2 "${journal}" | jq -r '.file_path // empty' 2>/dev/null | wc -l)"
if [ "${raisons}" = "path_glob_match session_start " ] && [ "${chemins}" -eq 2 ]; then
  reussis=$((reussis + 1))
else
  echecs=$((echecs + 1))
  printf '  ÉCHEC  /point sait relire le journal (raisons : %s| chemins : %s)\n' \
    "${raisons}" "${chemins}"
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
