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

La locale `C.UTF-8` est posée par le hook et par `.claude/settings.json` : **aucun préfixe
manuel n'est nécessaire**. Sans elle, le compilateur Kotlin échoue à écrire le `.class`
d'une lambda déclarée dans un test au nom français accentué. Le remède si l'erreur
survient malgré tout est dans `.claude/rules/tests.md`.

Note : `--offline` ne fonctionne pas tant que le cache Gradle est vide (les plugins
Android/Kotlin doivent être résolus depuis le réseau au premier build).

## Outillage de session

- **`.claude/rules/*.md`** — rappels chargés **automatiquement** selon les fichiers
  ouverts : composants Compose, pureté du domaine, tests, CI/build, documentation. Ce
  sont des digests avec pointeurs ; `docs/` reste la source.

### Commandes

- **`/cadrer`** — fixer la spec d'une demande floue **avant** d'écrire : lire ce qui est
  déjà tranché, poser les questions ouvertes, rendre critères d'acceptation et texte
  d'issue.
- **`/qualite`** — les quatre tâches Gradle et la cohérence des docs en une passe, avec
  la lecture des échecs courants.
- **`/revue`** — relecture adverse du diff par les deux sous-agents ci-dessous, avant
  livraison.
- **`/pousser`** — le rituel de livraison complet : qualité, état de branche, commit
  gitmoji, push, PR avec `Closes #N` **en anglais**, auto-merge armé, suivi planifié.
- **`/ecart`** — consigner au §9 une décision qui contredit le cahier, dans ses quatre
  emplacements liés.
- **`/point`** — la passation avant un `/clear`, un rewind ou une fin de séance.

### Sous-agents

Lecture seule, contexte neuf, invoqués par `/revue` ou à la demande :

- **`relecteur-ergonomie`** — le diff Compose contre `docs/ergonomie.md` : 48 dp, zone du
  pouce, couleurs, chaînes, previews.
- **`gardien-du-cahier`** — ce diff contredit-il le cahier, et si oui l'écart §9
  est-il consigné ?

### Hooks

Tous **consultatifs** sauf un, signalé comme tel :

- Après édition, un hook signale les couleurs littérales, les textes en dur et les
  imports Android dans `domain/`. ktlint, detekt et la CI restent l'arbitre.
- Au prompt, l'antisèche injecte au plus une orientation vers la commande adaptée et
  trois pointeurs vers les sources qui font autorité sur les sujets détectés.
- En fin de tour, un bilan rappelle ce qui reste : qualité non relancée, travail non
  commité, commits non poussés. Une ligne, et rien tant que l'état ne change pas.
- **`avant-livraison.sh` bloque**, lui — c'est la seule entorse au principe
  consultatif, limitée à trois erreurs déjà commises et mal réparables : commit sans
  gitmoji (aucune version ne sort), push direct sur `main`, fermeture d'issue rédigée en
  français (elle ne ferme rien). Il ne refuse que ce qu'il a su lire : un message passé
  par `-F` ou heredoc passe sans contrôle.

`./scripts/test-hooks.sh` fige le contrat d'entrée/sortie de chaque hook. Le lancer après
toute modification de `.claude/hooks/` : un hook muet est indistinguable d'un hook qui
n'avait rien à dire, et c'est exactement comme ça que l'antisèche est restée morte
plusieurs semaines.

## Auto-merge des PR

Toute PR ouverte par Claude Code est mise en **auto-merge (squash)** juste après sa
création, sauf mention explicite du contraire ou contrainte technique (auto-merge
désactivé sur le dépôt, PR en *draft*, aucun check requis, ou conflit / check requis en
échec). Elle fusionne donc d'elle-même dès que les checks requis passent.
