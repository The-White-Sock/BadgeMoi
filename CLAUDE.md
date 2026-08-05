# BadgeMoi

Application Android (Kotlin + Jetpack Compose) qui chronomètre un trajet domicile-travail
en Onewheel, jalon par jalon. Portage natif du POC HTML `docs/poc/trajet.html`.

Ce fichier ne duplique pas la documentation du projet — il s'appuie dessus et
n'ajoute que ce qui est spécifique aux sessions Claude Code. Avant toute évolution,
lire :

- **[`docs/cahier-des-charges.md`](docs/cahier-des-charges.md)** — périmètre
  fonctionnel, choix d'architecture, décisions déjà tranchées (§9). Ne pas rouvrir un
  point déjà tranché sans validation explicite.
- **[`docs/ergonomie.md`](docs/ergonomie.md)** — usage au pouce : zones d'atteinte,
  tailles de cibles, placement des modales et des actions destructives. Fait autorité sur
  le **placement** de tout élément interactif ; le cahier dit quoi afficher, ce fichier dit
  où le poser.
- **[`docs/conventions.md`](docs/conventions.md)** — stack, structure des packages,
  nomenclature, conventions de commit/branches, commandes de qualité de code. Fait
  autorité sur ces sujets ; toute mise à jour de convention se fait dans ce fichier,
  pas ici.
- **[`docs/publication.md`](docs/publication.md)** — cinématique de publication :
  squash merge, versioning automatique (semantic-release + gitmoji), pipeline de
  release, distribution (APK de test → F-Droid → Play Store).

## Spécificités des sessions Claude Code

Le SDK Android est installé automatiquement dans les sessions Claude Code on the web via
`.claude/hooks/session-start.sh` (command-line tools, `platforms;android-37.0`,
`build-tools;37.0.0`).

**`./gradlew` fonctionne dans ces sessions** : la distribution Gradle se télécharge et
`assembleDebug`, `testDebugUnitTest`, `ktlintCheck` et `detekt` s'exécutent localement.
Les lancer avant de pousser évite un aller-retour de CI. Celle-ci reste l'arbitre final
(environnement propre, accès réseau complet).

⚠️ **Préfixer les commandes Gradle par `LANG=C.UTF-8`.** Le conteneur démarre avec `LANG`
vide, donc `sun.jnu.encoding = ANSI_X3.4-1968` : le compilateur Kotlin échoue alors à
écrire le fichier `.class` d'une lambda déclarée dans un test au nom français accentué —
un test de dépôt écrit avec `= runTest { … }` produit un nom de fichier portant les
accents du nom de la méthode. Les runners GitHub étant en UTF-8, ce point ne concerne
que les exécutions locales en session.

```bash
LANG=C.UTF-8 ./gradlew testDebugUnitTest
```

Le démon Kotlin hérite de la locale du premier lancement : si une commande a déjà été
lancée sans `LANG`, l'erreur persiste malgré le préfixe. Le remède est de repartir d'un
démon neuf.

```bash
./gradlew --stop && pkill -f KotlinCompileDaemon
```

Note : `--offline` ne fonctionne pas tant que le cache Gradle est vide (les plugins
Android/Kotlin doivent être résolus depuis le réseau au premier build).

## Auto-merge des PR

Toute PR ouverte par Claude Code est mise en **auto-merge (squash)** juste après sa
création, sauf mention explicite du contraire ou contrainte technique (auto-merge
désactivé sur le dépôt, PR en *draft*, aucun check requis, ou conflit / check requis en
échec). Elle fusionne donc d'elle-même dès que les checks requis passent.
