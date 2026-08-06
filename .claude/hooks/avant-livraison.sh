#!/bin/bash
# Hook PreToolUse : arrête les trois gestes de livraison qu'on ne rattrape pas.
#
# ATTENTION — c'est le seul hook **bloquant** du dépôt. Tous les autres sont
# consultatifs par principe (voir `garde-fous.sh`). L'exception est assumée et
# limitée à des erreurs déjà commises ici, dont la réparation coûte plus cher que
# l'arrêt :
#
#   1. Un commit sans gitmoji ne déclenche aucune version. `semantic-release` lit
#      l'emoji, pas la prose (`docs/publication.md`). Le commit part, la release
#      ne sort pas, et personne ne le voit avant de chercher le tag. **`ask`** :
#      un commit de travail voué au rebase est un cas légitime, la personne
#      tranche.
#   2. Un push direct sur `main` contourne la PR, donc la CI, donc l'auto-merge.
#      `main` est protégé par ruleset : le refus arrive de toute façon, mais plus
#      tard et moins clairement. **`deny`** : jamais correct ici.
#   3. « Ferme #12 » ne ferme pas l'issue #12. GitHub ne connaît que les mots-clés
#      anglais. C'est l'erreur qui a laissé une série d'issues ouvertes après leur
#      livraison (`/pousser` §5). **`deny`** : jamais correct non plus.
#
# PRINCIPE DE PRUDENCE : ne refuser que ce qu'on a su **lire**. Un commit passé
# par `-F`, par heredoc ou en `--amend --no-edit` n'expose pas son message : on se
# tait. Un garde-fou qui refuse dans le doute finit contourné, et il emporte le
# reste du harnais avec lui.
#
# Entrée : JSON sur stdin (`.tool_name`, `.tool_input`).
# Sortie : JSON `hookSpecificOutput.permissionDecision` valant `deny` ou `ask`,
#          ou rien du tout.
# Retour : toujours 0 — la décision passe par le JSON, pas par le code de sortie.
set -uo pipefail

export LC_ALL=C.UTF-8
export LANG=C.UTF-8

entree="$(cat)"
outil="$(printf '%s' "${entree}" | jq -r '.tool_name // empty' 2>/dev/null || true)"

# `deny` refuse sec, `ask` rend la main à la personne avec le même avertissement.
#
# La distinction vient du classificateur PreToolUse d'Anthropic (billet « auto
# mode »), qui occupe la même position dans le cycle de vie : il redirige vers une
# autre approche, et si l'agent insiste il finit par **rendre la main à
# l'utilisateur** plutôt que de refuser indéfiniment. Un blocage sans issue est un
# défaut de conception, pas une rigueur.
#
# D'où la règle ici : `deny` pour ce qui n'est **jamais** correct dans ce dépôt,
# `ask` pour ce qui a un cas d'usage légitime que le hook ne sait pas distinguer.
decider() {
  jq -n --arg d "$1" --arg r "$2" \
    '{hookSpecificOutput: {hookEventName: "PreToolUse",
                           permissionDecision: $d,
                           permissionDecisionReason: $r}}'
  exit 0
}

refuser() { decider deny "$1"; }
demander() { decider ask "$1"; }

# Mots-clés de fermeture : GitHub ne les reconnaît qu'en anglais. Un corps de PR
# qui annonce une fermeture en français et n'en contient aucun en anglais ferme
# zéro issue, en le laissant croire.
verifier_fermeture() {
  local corps="$1"
  [ -z "${corps}" ] && return 0
  if printf '%s' "${corps}" | grep -qiE '(ferme|ferment|corrige|resout|résout|regle|règle)[[:space:]]+#[0-9]+' \
    && ! printf '%s' "${corps}" | grep -qiE '(close[sd]?|fixe[sd]?|resolve[sd]?)[[:space:]]+#[0-9]+'; then
    refuser "Corps de PR : une fermeture d'issue est annoncée en français, et aucun mot-clé anglais n'est présent. GitHub ne reconnaît que \`Closes #N\`, \`Fixes #N\` et \`Resolves #N\` — « Ferme #N » est une simple mention et ne ferme rien. C'est l'erreur qui a laissé une série d'issues ouvertes après leur livraison (voir /pousser §5). Le reste du corps reste en français."
  fi
}

case "${outil}" in
  mcp__github__create_pull_request | mcp__github__update_pull_request)
    verifier_fermeture "$(printf '%s' "${entree}" | jq -r '.tool_input.body // empty' 2>/dev/null || true)"
    exit 0
    ;;
  Bash) ;;
  *) exit 0 ;;
esac

commande="$(printf '%s' "${entree}" | jq -r '.tool_input.command // empty' 2>/dev/null || true)"
[ -z "${commande}" ] && exit 0

# --- Règle 1 : commit sans gitmoji -----------------------------------------
if printf '%s' "${commande}" | grep -qE 'git[[:space:]]+commit'; then
  # On ne lit qu'un message passé en clair par `-m`. Tout le reste — `-F`,
  # heredoc, `--amend --no-edit`, éditeur interactif — sort par le silence.
  message="$(printf '%s' "${commande}" \
    | grep -oE -- "-m[[:space:]]+'[^']*'" | head -1 \
    | sed -E "s/^-m[[:space:]]+'//; s/'$//")"
  if [ -z "${message}" ]; then
    message="$(printf '%s' "${commande}" \
      | grep -oE -- '-m[[:space:]]+"[^"]*"' | head -1 \
      | sed -E 's/^-m[[:space:]]+"//; s/"$//')"
  fi

  if [ -n "${message}" ]; then
    case "${message}" in
      ✨* | 🐛* | ♻* | ✅* | 📝* | 🔧* | 💄* | 🚀* | 💥* | 🔖*) ;;
      *)
        # `ask` et non `deny` : un commit de travail (`fixup!`, WIP avant rebase)
        # est un cas réel que ce hook ne sait pas distinguer d'un oubli. La
        # personne tranche, l'avertissement reste affiché.
        demander "Message de commit sans gitmoji : « ${message} ». L'emoji de tête pilote le versioning automatique (\`.releaserc.js\`, \`docs/publication.md\`) — sans lui, aucune version ne sort et rien ne le signale. Format attendu : \`<emoji>(<scope>) : <description au présent>\`, en français. ✨ fonctionnalité (mineure) · 🐛 correctif · ♻️ refactor · 💄 visuel · 🚀 perf (correctifs) · 💥 rupture (majeure) · ✅ tests · 📝 docs · 🔧 outillage. Détail dans /pousser §3. Si c'est un commit de travail destiné à être écrasé au rebase, confirmer ; sinon, corriger le message."
        ;;
    esac
  fi
fi

# --- Règle 2 : push direct sur main ----------------------------------------
if printf '%s' "${commande}" | grep -qE 'git[[:space:]]+push'; then
  # Isoler le segment du push : sans ça, un `git push … && git checkout main`
  # innocent serait refusé pour le `main` de la seconde commande.
  segment="$(printf '%s' "${commande}" \
    | sed -nE 's/.*git[[:space:]]+push[[:space:]]*([^;&|]*).*/\1/p')"
  if printf '%s' " ${segment} " | grep -qE '[[:space:]](main|[^[:space:]]*:main)[[:space:]]'; then
    refuser "Push direct vers \`main\` refusé. La branche de travail est imposée par la session, et \`main\` est protégé par ruleset : le travail passe par une PR en squash avec auto-merge armé. \`/pousser\` fait la séquence entière."
  fi
fi

# --- Règle 3 : fermeture d'issue en français, par la CLI --------------------
if printf '%s' "${commande}" | grep -qE 'gh[[:space:]]+pr[[:space:]]+(create|edit)'; then
  verifier_fermeture "${commande}"
fi

exit 0
