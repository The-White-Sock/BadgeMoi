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
#
# Le hook écrit dans le dépôt courant, donc dans le **journal vivant** de la séance —
# celui que `/point` interroge ensuite. L'y laisser travailler avait deux défauts,
# tous deux constatés et non théoriques :
#   - un événement arrivant pendant la batterie ajoutait une ligne et faisait échouer
#     l'assertion des trois lignes ci-dessous. C'est un rouge intermittent, le pire
#     genre : il ne se reproduit pas au passage suivant et on le met sur le compte du
#     hasard ;
#   - la restauration de l'instantané en fin de section **détruisait en silence** tout
#     ce qui s'était journalisé pendant la course. Lancer les tests corrompait donc la
#     mesure que la commande `/point` allait lire juste après.
#
# On détourne le hook vers un dépôt jetable. `GIT_DIR` doit pointer sur un **vrai**
# dépôt : sur un répertoire nu, `git rev-parse` échoue, le hook sort sans rien écrire,
# et toute cette section passerait au vert sans avoir rien testé.
bac="$(mktemp -d)"
git init --bare -q "${bac}/journal.git"
export GIT_DIR="${bac}/journal.git"
journal="${GIT_DIR}/badgemoi-instructions.log"

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

unset GIT_DIR
rm -rf "${bac}"

echo "interrogations de /point"
# Le journal n'est utile que si la commande sait le lire. On éprouve donc les trois
# pipelines de `.claude/commands/point.md` sur un journal fabriqué, contenant
# **exprès** une ligne de repli brut au milieu des lignes JSON : c'est le cas que
# `fromjson?` protège, et celui qui casserait un `jq` naïf.
#
# Le témoin reproduit une **compaction** : une règle chargée, puis les `session_start`
# qui rebornent la fenêtre, puis une autre règle. Le `session_id` est volontairement le
# même partout — c'est le fait qui rendait l'ancien filtre par `session_id` incapable
# de séparer les deux fenêtres, et qui lui faisait annoncer comme chargée une règle
# évincée depuis longtemps.
#
# Chaque fenêtre ouvre sur **deux** `session_start` — `CLAUDE.md` puis la règle non
# scopée `langue.md` — parce que c'est la forme réelle du journal depuis que cette règle
# existe. Un témoin à un seul `session_start` par fenêtre passait au vert avec l'`awk`
# qui tronquait comme avec celui qui borne juste : il ne mordait sur rien.
fenetre_de() {
  cut -f2 "$1" \
    | jq -rR 'fromjson? | "\(.load_reason // "raison absente")\t\(((.file_path // "chemin absent") | split("/") | last))"' \
    | awk '
        /^session_start\t/ { if (!suite || ($0 in vu)) { n = 0; split("", vu) }
                             suite = 1; vu[$0] = 1; l[n++] = $0; next }
                           { suite = 0; l[n++] = $0 }
        END                { for (i = 0; i < n; i++) print l[i] }' \
    | sort -u
}

temoin="$(mktemp)"
{
  printf '%s\t%s\n' '2026-01-01T00:00:00Z' \
    '{"session_id":"S1","file_path":"/r/CLAUDE.md","memory_type":"Project","load_reason":"session_start"}'
  printf '%s\t%s\n' '2026-01-01T00:00:01Z' \
    '{"session_id":"S1","file_path":"/r/.claude/rules/langue.md","memory_type":"Project","load_reason":"session_start"}'
  printf '%s\t%s\n' '2026-01-01T00:00:02Z' \
    '{"session_id":"S1","file_path":"/r/.claude/rules/docs-decisions.md","memory_type":"Project","load_reason":"path_glob_match"}'
  printf '%s\t%s\n' '2026-01-01T00:00:03Z' 'ligne brute, pas du JSON'
  printf '%s\t%s\n' '2026-01-01T00:00:04Z' \
    '{"session_id":"S1","file_path":"/r/CLAUDE.md","memory_type":"Project","load_reason":"session_start"}'
  printf '%s\t%s\n' '2026-01-01T00:00:05Z' \
    '{"session_id":"S1","file_path":"/r/.claude/rules/langue.md","memory_type":"Project","load_reason":"session_start"}'
  printf '%s\t%s\n' '2026-01-01T00:00:06Z' \
    '{"session_id":"S1","file_path":"/r/.claude/rules/ui-compose.md","memory_type":"Project","load_reason":"path_glob_match"}'
  # Du JSON **valide** auquel manquent les champs attendus : c'est la dérive de schéma,
  # et `fromjson?` n'en protège pas — lui ne filtre que le non-JSON. Placée après les
  # `session_start` pour tomber dans la fenêtre courante et non dans une fenêtre révolue.
  printf '%s\t%s\n' '2026-01-01T00:00:07Z' '{"marqueur":"schema_inattendu"}'
} > "${temoin}"

fenetre="$(fenetre_de "${temoin}")"
# Quatre côtés, sinon un pipeline muet passerait au vert : les deux `session_start` de la
# fenêtre courante doivent sortir **tous les deux** — c'est la troncature réparée, celle
# qui faisait disparaître `CLAUDE.md` du relevé alors qu'il est en contexte —, la règle
# d'après la compaction doit sortir, celle d'avant ne doit pas, et l'entrée au schéma
# inattendu doit sortir avec ses replis plutôt que de disparaître. Sans les `//`, `split`
# échoue sur le champ absent, la ligne s'évapore et `jq` sort quand même avec un code 0.
if printf '%s\n' "${fenetre}" | grep -q 'session_start.*CLAUDE\.md' \
  && printf '%s\n' "${fenetre}" | grep -q 'session_start.*langue\.md' \
  && printf '%s\n' "${fenetre}" | grep -q 'path_glob_match.*ui-compose\.md' \
  && ! printf '%s\n' "${fenetre}" | grep -q 'docs-decisions\.md' \
  && printf '%s\n' "${fenetre}" | grep -q 'raison absente.*chemin absent'; then
  reussis=$((reussis + 1))
else
  echecs=$((echecs + 1))
  echo "  ÉCHEC  borne le relevé à la fenêtre de contexte courante (obtenu : ${fenetre})"
fi

# Le cas que le seul « début de suite » ne couvre pas : une fenêtre qui n'a chargé aucune
# règle scopée est suivie **immédiatement** de la suivante, et les deux suites de
# `session_start` se touchent. Rien ne les sépare qu'un `session_start` déjà vu au tampon.
# Sans ce cas, `langue.md` serait annoncée en contexte alors qu'elle appartient à une
# fenêtre révolue — le défaut même qu'on répare, sous une autre forme.
temoin_colle="$(mktemp)"
{
  printf '%s\t%s\n' '2026-01-01T00:00:00Z' \
    '{"session_id":"S1","file_path":"/r/CLAUDE.md","memory_type":"Project","load_reason":"session_start"}'
  printf '%s\t%s\n' '2026-01-01T00:00:01Z' \
    '{"session_id":"S1","file_path":"/r/.claude/rules/langue.md","memory_type":"Project","load_reason":"session_start"}'
  printf '%s\t%s\n' '2026-01-01T00:00:02Z' \
    '{"session_id":"S1","file_path":"/r/CLAUDE.md","memory_type":"Project","load_reason":"session_start"}'
  printf '%s\t%s\n' '2026-01-01T00:00:03Z' \
    '{"session_id":"S1","file_path":"/r/.claude/rules/ui-compose.md","memory_type":"Project","load_reason":"path_glob_match"}'
} > "${temoin_colle}"

collee="$(fenetre_de "${temoin_colle}")"
if printf '%s\n' "${collee}" | grep -q 'session_start.*CLAUDE\.md' \
  && printf '%s\n' "${collee}" | grep -q 'path_glob_match.*ui-compose\.md' \
  && ! printf '%s\n' "${collee}" | grep -q 'langue\.md'; then
  reussis=$((reussis + 1))
else
  echecs=$((echecs + 1))
  echo "  ÉCHEC  sépare deux fenêtres dont les suites de session_start se touchent (obtenu : ${collee})"
fi
rm -f "${temoin_colle}"

# Trois raisons attendues, et le compte porte tout le sens : `session_start` et
# `path_glob_match` pour les entrées conformes, `inconnue` pour celle au schéma
# inattendu — qui doit **compter** au lieu de s'évaporer. La ligne brute, elle, ne
# compte pas : c'est `fromjson?` qui l'écarte. Les deux replis se lisent donc ici, et
# un compte de 2 signifierait qu'on a reperdu l'un des deux.
cumul="$(cut -f2 "${temoin}" | jq -rR 'fromjson? | .load_reason // "inconnue"' | sort | uniq -c)"
if [ "$(printf '%s\n' "${cumul}" | wc -l)" -eq 3 ] \
  && printf '%s\n' "${cumul}" | grep -q 'inconnue'; then
  reussis=$((reussis + 1))
else
  echecs=$((echecs + 1))
  echo "  ÉCHEC  compte par raison, ligne brute écartée et schéma inattendu compté (obtenu : ${cumul})"
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

# Sur un journal vide, la même interrogation sort **toutes** les règles. Ce n'est pas
# un défaut de glob mais une absence de données : le journal vit dans `.git/` et repart
# à zéro à chaque nouveau conteneur web. Ce cas fige la limite du pipeline plutôt qu'il
# ne la corrige — c'est `/point` qui porte le garde-fou, et ce test le rend faux si la
# formulation change.
: > "${temoin}"
attendu="$(ls .claude/rules/*.md | wc -l)"
jamais_vide="$(comm -13 \
  <(cut -f2 "${temoin}" | jq -rR 'fromjson? | .file_path // empty' | sed 's|.*/||' | sort -u) \
  <(ls .claude/rules/*.md | sed 's|.*/||' | sort) | grep -c .)"
if [ "${jamais_vide}" -eq "${attendu}" ]; then
  reussis=$((reussis + 1))
else
  echecs=$((echecs + 1))
  echo "  ÉCHEC  journal vide : liste les ${attendu} règles (obtenu : ${jamais_vide})"
fi

rm -f "${temoin}"

# Le hook écrit dans `git rev-parse --git-dir`, `/point` relit au même endroit. Dans un
# worktree lié les deux divergent : `.git` y est un fichier pointeur, pas un répertoire,
# et un chemin écrit en dur échoue sur « Not a directory ». La troisième interrogation
# annonçait alors les cinq règles comme jamais chargées — le faux négatif habituel, dans
# l'environnement où tournent justement les sous-agents isolés. On éprouve donc le
# couplage là où il casse, pas seulement là où il marche.
bac_wt="$(mktemp -d)/wt"
if git worktree add -q --detach "${bac_wt}" HEAD 2>/dev/null; then
  lu_wt="$(cd "${bac_wt}" \
    && printf '%s' '{"file_path":"/r/.claude/rules/tests.md","load_reason":"path_glob_match"}' \
       | .claude/hooks/instructions-chargees.sh \
    && cut -f2 "$(git rev-parse --git-dir)/badgemoi-instructions.log" 2>/dev/null \
       | jq -rR 'fromjson? | .file_path // empty' | sed 's|.*/||')"
  regles_wt="$(cd "${bac_wt}" && ls "$(git rev-parse --show-toplevel)"/.claude/rules/*.md 2>/dev/null | wc -l)"
  if [ "${lu_wt}" = "tests.md" ] && [ "${regles_wt}" -eq "$(ls .claude/rules/*.md | wc -l)" ]; then
    reussis=$((reussis + 1))
  else
    echecs=$((echecs + 1))
    echo "  ÉCHEC  worktree lié : relit le journal du hook (lu : '${lu_wt}', règles : ${regles_wt})"
  fi
  git worktree remove --force "${bac_wt}" >/dev/null 2>&1
  git worktree prune >/dev/null 2>&1
else
  echecs=$((echecs + 1))
  echo "  ÉCHEC  worktree lié : impossible d'en créer un pour l'épreuve"
fi
rm -rf "$(dirname "${bac_wt}")"

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

echo "garde-fous.sh"
# Ce hook lit le fichier **sur le disque**, pas le contenu du payload : ses cas ont
# donc besoin de vrais fichiers. Ils sont créés hors du dépôt et retirés à la fin,
# pour tenir la promesse d'absence d'effet de bord faite en tête de ce script.
bac="$(mktemp -d)"
mkdir -p "${bac}/ui/theme" "${bac}/ui/summary" "${bac}/domain"

printf 'val Rouge = Color(0xFFAA0000)\n' > "${bac}/ui/summary/Ecran.kt"
cas "couleur littérale hors du thème" garde-fous \
  "{\"tool_input\":{\"file_path\":\"${bac}/ui/summary/Ecran.kt\"}}" 'Couleur litt'

printf 'val Rouge = Color(0xFFAA0000)\n' > "${bac}/ui/theme/Couleurs.kt"
cas "la même couleur dans ui/theme/ est légitime" garde-fous \
  "{\"tool_input\":{\"file_path\":\"${bac}/ui/theme/Couleurs.kt\"}}" VIDE

printf 'import androidx.room.Entity\n' > "${bac}/domain/Trajet.kt"
cas "import Android dans domain/" garde-fous \
  "{\"tool_input\":{\"file_path\":\"${bac}/domain/Trajet.kt\"}}" 'domain'

printf 'Text("Depart du trajet")\n' > "${bac}/ui/summary/Dur.kt"
cas "texte en dur dans un composable" garde-fous \
  "{\"tool_input\":{\"file_path\":\"${bac}/ui/summary/Dur.kt\"}}" 'en dur'

printf 'Text(stringResource(R.string.summary_titre))\n' > "${bac}/ui/summary/Propre.kt"
cas "stringResource ne déclenche rien" garde-fous \
  "{\"tool_input\":{\"file_path\":\"${bac}/ui/summary/Propre.kt\"}}" VIDE

printf '@Preview\nText("Libelle de demonstration")\n' > "${bac}/ui/summary/Apercu.kt"
cas "l'analyse s'arrête au premier @Preview" garde-fous \
  "{\"tool_input\":{\"file_path\":\"${bac}/ui/summary/Apercu.kt\"}}" VIDE

# Le cas de régression : sous `pipefail`, enchaîner les deux `grep` faisait sortir
# le premier en 141 (SIGPIPE) dès que `grep -q` s'arrêtait au premier suspect. Le
# test ne mord que si le tube a de quoi être coupé — d'où le volume.
for _ in $(seq 1 4000); do printf 'Text("Libelle ecrit en dur")\n'; done \
  > "${bac}/ui/summary/Volumineux.kt"
cas "alerte encore sur un fichier volumineux (SIGPIPE)" garde-fous \
  "{\"tool_input\":{\"file_path\":\"${bac}/ui/summary/Volumineux.kt\"}}" 'en dur'

cas "se tait sur un fichier qui n'est pas du Kotlin" garde-fous \
  "{\"tool_input\":{\"file_path\":\"${bac}/ui/summary/notes.md\"}}" VIDE

cas "se tait sur un chemin inexistant" garde-fous \
  "{\"tool_input\":{\"file_path\":\"${bac}/ui/summary/Absent.kt\"}}" VIDE

rm -rf "${bac}"

echo
if [ "${echecs}" -eq 0 ]; then
  echo "${reussis} cas, tous verts."
  exit 0
fi
echo "${reussis} verts, ${echecs} en échec."
exit 1
