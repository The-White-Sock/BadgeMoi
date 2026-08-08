#!/bin/bash
# Ligne d'état : branche, remplissage du contexte, ampleur du diff en cours.
#
# POURQUOI : le dépôt mesurait déjà deux budgets, et **aucun des deux ne disait le
# remplissage de la fenêtre en cours**. `check-docs-coherence.sh` compte les lignes
# injectées au lancement, le journal d'instructions compte des chargements : tous deux
# portent sur le démarrage. La grandeur qui manquait est fournie **déjà calculée** par
# le harnais, il ne restait qu'à l'afficher.
#
# AUCUN SEUIL N'EST MARQUÉ ICI, et c'est une décision. Les chiffres qui circulent
# (« rester sous 40 % ») sont, à la source, une heuristique de débutant explicitement
# relativisée par son auteur, et le rot de contexte est donné côté Anthropic comme
# « highly dependent on the task — not a fast rule ». Peindre un seuil en rouge lui
# donnerait une autorité que ses sources lui refusent. On affiche la mesure ; le
# réglage viendra des données, pas d'un chiffre emprunté.
#
# PIÈGE À CONNAÎTRE : `disableAllHooks` coupe **aussi** la ligne d'état. Les deux
# mécanismes paraissent indépendants et ne le sont pas — une coupure d'urgence du
# harnais fera donc disparaître cette jauge, sans que ce soit une panne.
#
# Entrée : JSON sur stdin (voir `statusLine` dans la doc des réglages).
# Sortie : une ligne, courte. Retour toujours 0 — une ligne d'état ne casse rien.
set -uo pipefail

export LC_ALL=C.UTF-8
export LANG=C.UTF-8

entree="$(cat 2>/dev/null || true)"

lire() {
  [ -z "${entree}" ] && return 0
  printf '%s' "${entree}" | jq -r "$1 // empty" 2>/dev/null || true
}

morceaux=()

# La branche vient du JSON quand il la porte, sinon de git — un worktree lié n'a pas
# la même que le dépôt principal, et c'est justement là qu'on se trompe de cible.
branche="$(git branch --show-current 2>/dev/null || true)"
[ -n "${branche}" ] && morceaux+=("${branche}")

# Le champ est **pré-calculé** par le harnais : ne pas le recalculer depuis les
# compteurs de jetons, qui ne tiennent pas compte du cache.
pct="$(lire '.context_window.used_percentage')"
if [ -n "${pct}" ]; then
  morceaux+=("ctx $(printf '%.0f' "${pct}" 2>/dev/null || printf '%s' "${pct}")%")
fi

# Ampleur du diff de la séance. Sert de rappel discret : les PR de ce dépôt sont
# grosses au regard des distributions publiées, et l'écart ne se voit qu'après coup.
ajoutees="$(lire '.cost.total_lines_added')"
retirees="$(lire '.cost.total_lines_removed')"
if [ -n "${ajoutees}" ] || [ -n "${retirees}" ]; then
  total=$(( ${ajoutees:-0} + ${retirees:-0} ))
  [ "${total}" -gt 0 ] && morceaux+=("${total} l.")
fi

[ ${#morceaux[@]} -eq 0 ] && exit 0

printf '%s' "${morceaux[0]}"
for m in "${morceaux[@]:1}"; do printf ' · %s' "${m}"; done
printf '\n'

exit 0
