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
