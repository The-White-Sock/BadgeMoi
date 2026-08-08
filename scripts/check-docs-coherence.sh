#!/bin/bash
# Vérifie que la documentation reste cohérente avec l'état réel du dépôt.
# Appelé par .github/workflows/docs-coherence.yml (cron hebdomadaire).
#
# Ne vérifie que des invariants *mécaniques* (versions, existence des fichiers
# référencés) : la dérive de la prose (conventions, décisions d'architecture)
# reste du ressort de la relecture humaine.
#
# Sortie   : rapport Markdown des écarts sur stdout.
# Retour   : 0 si tout est cohérent, 1 si au moins un écart est détecté.
set -uo pipefail

cd "$(dirname "$0")/.."

ecarts=()
add() { ecarts+=("$1"); }

CONVENTIONS="docs/conventions.md"

# --- 1. Versions de la stack annoncées dans la doc ---------------------------
stack_line="$(grep -m1 -E 'Kotlin [0-9.]+.*AGP [0-9.]+.*Gradle [0-9.]+' "${CONVENTIONS}" || true)"

if [ -z "${stack_line}" ]; then
  add "\`${CONVENTIONS}\` : la ligne « stack » (Kotlin / AGP / Gradle) est introuvable — son format a changé, ce contrôle ne peut plus vérifier les versions. Adapter \`scripts/check-docs-coherence.sh\`."
else
  doc_kotlin="$(sed -nE 's/.*Kotlin ([0-9]+\.[0-9]+\.[0-9]+).*/\1/p' <<<"${stack_line}")"
  doc_agp="$(sed -nE 's/.*AGP ([0-9]+\.[0-9]+\.[0-9]+).*/\1/p' <<<"${stack_line}")"
  doc_gradle="$(sed -nE 's/.*Gradle ([0-9]+\.[0-9]+\.[0-9]+).*/\1/p' <<<"${stack_line}")"

  real_kotlin="$(sed -nE 's/^kotlin = "([^"]+)".*/\1/p' gradle/libs.versions.toml)"
  real_agp="$(sed -nE 's/^agp = "([^"]+)".*/\1/p' gradle/libs.versions.toml)"
  real_gradle="$(sed -nE 's/.*gradle-([0-9.]+)-bin\.zip.*/\1/p' gradle/wrapper/gradle-wrapper.properties)"

  [ "${doc_kotlin}" = "${real_kotlin}" ] || \
    add "**Version Kotlin** — \`${CONVENTIONS}\` annonce \`${doc_kotlin}\`, \`gradle/libs.versions.toml\` déclare \`${real_kotlin}\`. Corriger la ligne « stack » de la doc."
  [ "${doc_agp}" = "${real_agp}" ] || \
    add "**Version AGP** — \`${CONVENTIONS}\` annonce \`${doc_agp}\`, \`gradle/libs.versions.toml\` déclare \`${real_agp}\`. Corriger la ligne « stack » de la doc."
  [ "${doc_gradle}" = "${real_gradle}" ] || \
    add "**Version Gradle** — \`${CONVENTIONS}\` annonce \`${doc_gradle}\`, \`gradle/wrapper/gradle-wrapper.properties\` utilise \`${real_gradle}\`. Corriger la ligne « stack » de la doc."

  # Contrainte CodeQL documentée : Kotlin doit rester sous 2.4.10, sinon le job
  # `analyze` échoue (« Kotlin version 2.4.10 is too recent »).
  if [ -n "${real_kotlin}" ]; then
    plus_basse="$(printf '%s\n%s\n' "2.4.10" "${real_kotlin}" | sort -V | head -1)"
    if [ "${plus_basse}" = "2.4.10" ]; then
      add "**Contrainte CodeQL non respectée** — Kotlin est en \`${real_kotlin}\`, or \`${CONVENTIONS}\` documente un plafond strict sous \`2.4.10\` (l'extracteur Kotlin de CodeQL refuse au-delà et fait échouer le job \`analyze\`). Soit redescendre Kotlin, soit mettre la doc à jour si un bundle CodeQL plus récent a levé le plafond."
    fi
  fi
fi

# --- 2. Fichiers référencés par la doc ---------------------------------------
DOCS=(docs/*.md CLAUDE.md README.md)

# 2a. Chemins entre backticks, relatifs à la racine du dépôt. On se limite aux
# chemins partant d'un dossier connu ET portant une extension de fichier, pour
# ne pas confondre avec un slug de dépôt GitHub (`gradle/gradle-distributions`),
# un type MIME (`text/csv`) ou un chemin relatif à un package (`ui/theme/X.kt`).
while read -r chemin; do
  [ -z "${chemin}" ] && continue
  case "${chemin}" in
    .github/*|.claude/*|docs/*|scripts/*|app/*|gradle/*|config/*) ;;
    *) continue ;;
  esac
  case "${chemin}" in
    *.yml|*.yaml|*.kts|*.toml|*.md|*.sh|*.js|*.json|*.properties|*.kt|*.xml|*.jar) ;;
    *) continue ;;
  esac
  [ -e "${chemin}" ] || add "**Fichier référencé introuvable** — la documentation cite \`${chemin}\`, qui n'existe pas dans le dépôt. Corriger le chemin ou retirer la référence."
done < <(grep -rhoE '`[A-Za-z0-9_.-]+(/[A-Za-z0-9_.-]+)+`' "${DOCS[@]}" 2>/dev/null | tr -d '`' | sort -u)

# 2b. Liens Markdown relatifs : résolus depuis le dossier du fichier source.
for doc in "${DOCS[@]}"; do
  [ -f "${doc}" ] || continue
  dossier="$(dirname "${doc}")"
  while read -r cible; do
    [ -z "${cible}" ] && continue
    case "${cible}" in
      http*|\#*|mailto:*) continue ;;
    esac
    cible="${cible%%#*}"
    [ -z "${cible}" ] && continue
    [ -e "${dossier}/${cible}" ] || [ -e "${cible}" ] || \
      add "**Lien cassé** — \`${doc}\` pointe vers \`${cible}\`, introuvable. Corriger le lien."
  done < <(grep -oE '\]\([^)]+\)' "${doc}" 2>/dev/null | sed -E 's/^\]\(//; s/\)$//' | sort -u)
done

# --- 3. Cohérence interne du §9 (écarts au périmètre) ------------------------
# Un écart se consigne en trois endroits qui doivent rester d'accord : une ligne
# du tableau, une section de prose, et le compte annoncé. Les tenir à la main les
# désynchronise tôt ou tard — c'est mécanique, donc c'est vérifiable.
CAHIER="docs/cahier-des-charges.md"

if [ -f "${CAHIER}" ]; then
  # Numéros du tableau des décisions : lignes « | N | … | … | ».
  tableau="$(grep -oE '^\| [0-9]+ \|' "${CAHIER}" | grep -oE '[0-9]+' | sort -n | uniq)"
  # Numéros des sections de prose : « **N. Titre.** ».
  prose="$(grep -oE '^\*\*[0-9]+\. ' "${CAHIER}" | grep -oE '[0-9]+' | sort -n | uniq)"

  # Le premier écart rédigé en prose porte le numéro 6 : les décisions 1 à 5 sont
  # des choix, pas des dérogations, et n'ont pas de section.
  for n in ${tableau}; do
    [ "${n}" -lt 6 ] && continue
    printf '%s\n' ${prose} | grep -qx "${n}" || \
      add "**Écart §9 sans justification** — la décision \`${n}\` figure au tableau de \`${CAHIER}\` mais n'a pas de section \`**${n}. …**\`. Un écart sans sa raison sera « corrigé » plus tard de bonne foi. Ajouter la section, ou utiliser \`/ecart\`."
  done

  for n in ${prose}; do
    printf '%s\n' ${tableau} | grep -qx "${n}" || \
      add "**Écart §9 hors tableau** — la section \`**${n}. …**\` de \`${CAHIER}\` n'a pas de ligne correspondante dans le tableau des décisions. Ajouter la ligne."
  done

  # Le compte annoncé juste avant les sections de prose.
  annonce="$(grep -oE 'Ces (deux|trois|quatre|cinq|six|sept|huit|neuf|dix|onze|douze) points' "${CAHIER}" | head -1 | awk '{print $2}')"
  reel="$(printf '%s\n' ${prose} | grep -c '[0-9]' || true)"
  case "${reel}" in
    2) attendu="deux" ;; 3) attendu="trois" ;; 4) attendu="quatre" ;; 5) attendu="cinq" ;;
    6) attendu="six" ;; 7) attendu="sept" ;; 8) attendu="huit" ;; 9) attendu="neuf" ;;
    10) attendu="dix" ;; 11) attendu="onze" ;; 12) attendu="douze" ;; *) attendu="" ;;
  esac
  if [ -n "${annonce}" ] && [ -n "${attendu}" ] && [ "${annonce}" != "${attendu}" ]; then
    add "**Compte des écarts faux** — \`${CAHIER}\` annonce « Ces ${annonce} points » alors qu'il compte ${reel} sections d'écart (« ${attendu} »). Mettre la phrase à jour, ainsi que la plage du titre."
  fi
fi

# --- 4. Configuration Claude Code --------------------------------------------
# Ce qui est chargé au **lancement** de chaque session, et ré-injecté après une
# compaction : `CLAUDE.md` **et** les règles `.claude/rules/` sans frontmatter
# `paths:` — la documentation les charge « with the same priority as
# `.claude/CLAUDE.md` ». Une règle *avec* `paths:` n'en est pas : elle n'arrive
# qu'en ouvrant un fichier de sa zone, et repart à la compaction. C'est donc la
# somme des non scopées qu'il faut tenir, pas le seul `CLAUDE.md` : ne mesurer que
# lui laisserait une porte par laquelle le chargement de session grossit sans que
# rien ne le compte.
#
# Ce qui compte est le contexte **réellement injecté**, pas la taille des fichiers :
# frontmatter et commentaires HTML de bloc en sont retirés, donc gratuits pour le
# budget d'instructions — c'est d'ailleurs pourquoi la provenance des choix est
# rangée dans un commentaire. Les facturer ferait mordre le contrôle sur des lignes
# qui ne coûtent rien, et son message ordonnerait de sortir la seule chose qu'il est
# sans risque de garder. Ne pas revenir à un `wc -l` du fichier entier.
#
# `dans` passe à 1 avant le test d'impression : un commentaire ouvert et fermé sur
# une même ligne est donc écarté lui aussi.
injectees() {
  awk '
    NR == 1 && /^---[[:space:]]*$/ { fm = 1; next }
    fm && /^---[[:space:]]*$/      { fm = 0; next }
    fm                             { next }
    /<!--/                         { dans = 1 }
    !dans                          { print }
    /-->/                          { dans = 0 }
  ' "$1" | wc -l
}

# Le frontmatter seul : de la première ligne `---` jusqu'au `---` suivant. C'est la
# seule zone où `paths:` est une **clé** ; ailleurs c'est de la prose ou un bloc de
# code qui l'illustre. Chercher la clé dans tout le fichier se trompait dans les deux
# sens, et les deux fois en silence : une règle non scopée qui documente la clé
# sortait du budget de lancement, et une règle sans frontmatter du tout était
# déclarée inopérante sur un motif qu'elle ne porte pas.
frontmatter() {
  awk '
    NR == 1 && /^---[[:space:]]*$/ { dans = 1; next }
    dans && /^---[[:space:]]*$/    { exit }
    dans                           { print }
  ' "$1"
}

lancement=0
detail=""
for charge in CLAUDE.md .claude/rules/*.md; do
  [ -f "${charge}" ] || continue
  # Une règle à `paths:` se charge par zone : hors budget de lancement. Here-string
  # et non tube : `grep -q` s'arrête au premier match et SIGPIPE l'amont, ce qui
  # vaut 141 sous `pipefail` (voir `.claude/rules/harnais.md`).
  if [ "${charge}" != "CLAUDE.md" ] && grep -q '^paths:' <<< "$(frontmatter "${charge}")"; then
    continue
  fi
  n="$(injectees "${charge}")"
  lancement=$(( lancement + n ))
  detail="${detail}${detail:+, }\`${charge}\` ${n}"
done

[ "${lancement}" -le 200 ] || \
  add "**Chargement de session trop lourd** — ${lancement} lignes injectées à chaque lancement, pour une cible de 200 (${detail}). Ces fichiers sont chargés en entier et ré-injectés après compaction. Déplacer ce qui n'est pas un invariant de toutes les sessions vers une règle \`.claude/rules/\` **avec** \`paths:\` (chargement par zone) ou vers \`docs/\`."

# Une règle dont le glob ne matche plus aucun fichier est morte **en silence** :
# elle ne se déclenche jamais et rien ne le signale. C'est le mode de panne
# propre à ce mécanisme, donc celui qu'il faut surveiller.
#
# `find -path` ne sait pas exprimer `**` (zéro **ou** plusieurs répertoires) : on
# traduit donc le glob en expression rationnelle, qu'on éprouve sur les fichiers
# suivis par git.
glob_vers_regex() {
  printf '%s' "$1" | sed -E \
    -e 's/\./\\./g' \
    -e 's#\*\*/#\x01#g' \
    -e 's#\*\*#\x02#g' \
    -e 's#\*#[^/]*#g' \
    -e 's#\x01#(.*/)?#g' \
    -e 's#\x02#.*#g'
}

suivis="$(git ls-files 2>/dev/null || true)"

for regle in .claude/rules/*.md; do
  [ -f "${regle}" ] || continue
  # Les items de `paths:`, lus dans le **frontmatter** et nulle part ailleurs, en
  # s'arrêtant à la première ligne qui n'en est pas un — sans quoi la clé suivante
  # du frontmatter serait lue comme un motif.
  motifs="$(awk '
    /^paths:/ { dans = 1; next }
    dans && /^[[:space:]]*-[[:space:]]/ {
      sub(/^[[:space:]]*-[[:space:]]*"?/, ""); sub(/"[[:space:]]*$/, ""); print; next
    }
    dans { exit }
  ' <<< "$(frontmatter "${regle}")")"
  [ -z "${motifs}" ] && continue

  while read -r motif; do
    [ -z "${motif}" ] && continue
    if ! printf '%s\n' "${suivis}" | grep -qE "^$(glob_vers_regex "${motif}")$"; then
      add "**Règle Claude Code inopérante** — le motif \`${motif}\` de \`${regle}\` ne correspond à aucun fichier suivi. La règle ne se déclenchera jamais, sans que rien ne l'indique. Corriger le motif ou retirer la règle."
    fi
  done <<< "${motifs}"
done

# --- Rapport -----------------------------------------------------------------
if [ ${#ecarts[@]} -eq 0 ]; then
  echo "Documentation cohérente — aucun écart détecté."
  exit 0
fi

echo "L'audit hebdomadaire a détecté ${#ecarts[@]} écart(s) entre la documentation et l'état réel du dépôt."
echo
for e in "${ecarts[@]}"; do
  echo "- ${e}"
done
echo
echo "---"
echo "_Rapport généré automatiquement par \`.github/workflows/docs-coherence.yml\`._"
exit 1
