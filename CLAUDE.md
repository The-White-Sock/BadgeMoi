# BadgeMoi

Application Android (Kotlin + Jetpack Compose) qui chronomètre un trajet domicile-travail
en Onewheel, jalon par jalon. Portage natif du POC HTML `docs/poc/trajet.html`.

Ce fichier ne duplique pas la documentation du projet — il s'appuie dessus et
n'ajoute que ce qui est spécifique aux sessions Claude Code. Avant toute évolution,
lire :

- **[`docs/cahier-des-charges.md`](docs/cahier-des-charges.md)** — périmètre
  fonctionnel, choix d'architecture, décisions déjà tranchées (§9). Ne pas rouvrir un
  point déjà tranché sans validation explicite.
- **[`docs/conventions.md`](docs/conventions.md)** — stack, structure des packages,
  nomenclature, conventions de commit/branches, commandes de qualité de code. Fait
  autorité sur ces sujets ; toute mise à jour de convention se fait dans ce fichier,
  pas ici.

## Spécificités des sessions Claude Code

Le SDK Android est installé automatiquement dans les sessions Claude Code on the web via
`.claude/hooks/session-start.sh` (command-line tools, `platforms;android-37.0`,
`build-tools;37.0.0`). Le téléchargement de la distribution Gradle elle-même
(`gradle-9.4.1-bin.zip`, redirigée vers un release GitHub `gradle/gradle-distributions`)
reste bloqué par le scope GitHub de la session — `./gradlew` local n'y fonctionne donc
pas encore, mais la CI GitHub Actions a un accès réseau complet et fait foi : se fier à
ses résultats pour valider un build plutôt qu'à une exécution locale dans cette session.
