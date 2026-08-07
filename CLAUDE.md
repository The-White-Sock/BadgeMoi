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

- **`.claude/rules/*.md`** — digests avec pointeurs ; `docs/` reste la source. Deux
  régimes, et c'est le frontmatter `paths:` qui les sépare :
  - **Avec `paths:`** — chargés à l'ouverture d'un fichier de leur zone : composants
    Compose, pureté du domaine, tests, CI/build, documentation, hooks et scripts.
    **Ils ne survivent pas à un `/compact`** : ce fichier-ci est ré-injecté, eux
    attendent qu'un fichier de leur zone soit relu. Après une compaction, rouvrir un
    fichier concerné avant d'y écrire — `/point` détaille la vérification.
  - **Sans `paths:`** — chargés à chaque lancement et ré-injectés après compaction,
    au même rang que ce fichier. Réservé à ce qui vaut en fin de séance, quand plus
    aucun fichier n'est ouvert : `langue.md` seul aujourd'hui. Leur poids est compté
    avec celui de `CLAUDE.md` par `check-docs-coherence.sh`.

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
- Au chargement d'un fichier d'instructions, un hook journalise l'événement dans
  `.git/badgemoi-instructions.log` — c'est ce qui permet de savoir si une règle s'est
  vraiment chargée, et combien s'accumulent. Il n'écrit rien en session ; `/point` le lit.
- **`avant-livraison.sh` arrête**, lui — c'est la seule entorse au principe
  consultatif, limitée à trois erreurs déjà commises et mal réparables. Il **refuse** le
  push direct sur `main` et la fermeture d'issue rédigée en français (elle ne ferme rien) :
  aucun des deux n'est jamais correct ici. Il **rend la main** sur un commit sans gitmoji,
  parce qu'un commit de travail voué au rebase est légitime et qu'il ne sait pas l'en
  distinguer. Et il n'arrête que ce qu'il a su lire : un message passé par `-F` ou heredoc
  passe sans contrôle.

`./scripts/test-hooks.sh` fige le contrat d'entrée/sortie de chaque hook. Le lancer après
toute modification de `.claude/hooks/` : un hook muet est indistinguable d'un hook qui
n'avait rien à dire, et c'est exactement comme ça que l'antisèche est restée morte
plusieurs semaines.

<!--
NOTE DE MAINTENANCE — pour qui fait évoluer le harnais, pas pour la session.
Les commentaires HTML de bloc sont retirés avant injection en contexte : ce bloc
ne coûte rien au budget d'instructions. Y mettre la provenance, pas des consignes.

Provenance des choix ci-dessus :
- « instructions plutôt que configuration », cible des 200 lignes, non-réinjection
  des règles à `paths:` après compaction, retrait des commentaires HTML :
  code.claude.com/docs/en/memory
- `deny` / `ask` et l'idée qu'un blocage doit finir par rendre la main :
  claude.com/blog/auto-mode, qui décrit le classificateur PreToolUse d'Anthropic.
- Le budget se compte en **instructions** (~150-200 suivables, dont ~50 déjà prises
  par le prompt système) et la dégradation est **uniforme** — une instruction faible
  en plus dégrade aussi les fortes : humanlayer.dev/blog/writing-a-good-claude-md.
  Chiffres non référencés par la source : argumentés, pas mesurés.
- La « règle des 60 lignes » attribuée à HumanLayer par des index tiers est une
  mauvaise citation : l'article recommande < 300 lignes et mentionne 60 comme un
  simple constat sur son propre dépôt. Ne pas reprendre ce chiffre.

Point clos : le schéma de `InstructionsLoaded` a été relevé au journal (`file_path`,
`memory_type`, `load_reason`) et noté dans l'en-tête du hook, qui continue de
journaliser le JSON brut — ces noms sont observés, pas spécifiés. Le mécanisme des
règles à `paths:` est vérifié : les cinq ont émis un `path_glob_match` en une
poignée de lectures. Et le glob se déclenche sur le **chemin visé**, pas sur
l'existence du fichier — lire un chemin inexistant de la zone suffit à charger la
règle, ce qui rend le remède post-compaction gratuit.

Portée du journal : il est écrit dans `.git/`, donc recloné à vide à chaque nouveau
conteneur web. Le cumul « toutes séances » n'a que la durée de vie de la machine, et
la liste des « règles jamais chargées » sort pleine au démarrage sans que rien ne
soit cassé. Détail et garde-fous dans `/point`.

Point clos aussi, sur une compaction réelle cette fois : `/compact` ré-injecte bien
`CLAUDE.md` et **aucune** règle à `paths:`, ce qui n'était jusque-là qu'une lecture de
la documentation. Deux relevés inattendus au passage. D'abord **il n'existe pas de
raison `compact`** : la compaction journalise `session_start`, si bien qu'un
`session_start` en cours de séance est la borne d'une fenêtre de contexte — c'est ce
qui permet enfin de mesurer ce qui est *réellement* chargé. Ensuite la déduplication
porte sur la fenêtre, pas sur la séance : une compaction la remet à zéro, et c'est ce
qui rend le remède (rouvrir un fichier de la zone) opérant là où on en a besoin. Sans
cette remise à zéro il aurait été inopérant, et personne ne l'aurait su.

Ce qui a été corrigé en conséquence : la première interrogation de `/point` filtrait
sur le `session_id`, qui ne change pas à la compaction. Elle annonçait donc chargées
des règles évincées depuis plusieurs fenêtres — l'erreur exactement dans le sens qui
trompe. Quatrième occurrence du même motif après le bug `.user_input` : un pipeline
qui affirme plus que ses données ne portent. Le réflexe à garder est moins « vérifier
le glob » que « vérifier ce que la mesure prétend mesurer ».
-->

<!-- Prototype en cours : une balise `<important if="...">` (hlyr.dev), une seule,
     sur la section « Si un module Gradle apparaît » de `.claude/rules/ci-build.md`.
     Piste rouverte sur décision explicite après le diagnostic ci-dessus. Bloc choisi
     parce qu'il est déjà conditionnel dans son titre, qu'il pèse ~15 lignes et qu'il
     est hors sujet pour presque toute édition de CI. Condition rédigée en français,
     seul le nom de balise reste anglais — comme `paths:` l'est déjà.

     Observé depuis : sur une séance neuve, lire `gradle/libs.versions.toml` charge
     bien `ci-build.md`, et la section injectée porte la balise **verbatim** — le
     harnais ne la retire pas et ne l'interprète pas. Elle arrive au modèle comme du
     texte, ce qui borne le débat : il n'y a aucun mécanisme derrière, seulement une
     phrase de plus à lire. Le doute « jamais observée chargée » est levé ; le doute
     sur son utilité, lui, reste entier et le restera.

     Ce qui est mesurable : le **coût**. Le journal dit combien de fois `ci-build.md`
     se charge, donc combien de fois ces lignes sont injectées sans être pertinentes.
     Ce qui ne l'est pas : le **bénéfice**. Aucun signal ne dit si le modèle a
     appliqué la condition, et l'auteur de la convention reconnaît lui-même n'avoir
     « no rigorous explanation for why this helps ». Ne pas relire ce prototype comme
     un acquis, ni l'étendre aux quatre autres règles sur sa seule existence.

     Critère de révision : la balise est un échec le jour où un module Gradle
     apparaît (#123) et où l'une des trois régressions silencieuses passe quand
     même. -->


## Auto-merge des PR

Toute PR ouverte par Claude Code est mise en **auto-merge (squash)** juste après sa
création, sauf mention explicite du contraire ou contrainte technique (auto-merge
désactivé sur le dépôt, PR en *draft*, aucun check requis, ou conflit / check requis en
échec). Elle fusionne donc d'elle-même dès que les checks requis passent.
