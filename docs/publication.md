# Cinématique de publication

Ce document décrit comment le code passe d'un commit à une version installable :
stratégie de branches, versioning, et distribution. Voir aussi
[`conventions.md`](conventions.md) pour le format des messages de commit et
[`cahier-des-charges.md`](cahier-des-charges.md#410-distribution) pour la décision de
distribution finale (F-Droid puis Play Store).

## Branches et merge

- Développement trunk-based : `main` est toujours dans un état publiable, le travail se
  fait sur des branches courtes `feat/<sujet>`, `fix/<sujet>`, `chore/<sujet>` fusionnées
  via Pull Request.
- **Squash merge uniquement** vers `main` : une PR = un commit sur `main`, quel que soit
  le nombre de commits intermédiaires dedans (corrections de relecture, itérations CI...).
  Ça garde un historique de `main` lisible, un commit = un changement livrable.
- Le message du commit squashé (celui qui atterrit sur `main`) est ce qui compte pour le
  versioning automatique (§ suivante) : respecter le format gitmoji même en résumant
  plusieurs commits d'une PR en un seul.

⚠️ Restreindre le dépôt au squash merge est un réglage GitHub (Settings → General →
Pull Requests → décocher "Allow merge commits" et "Allow rebase merging", garder
uniquement "Allow squash merging") qui n'est pas accessible depuis les outils de cette
session — à faire manuellement une fois.

## Protection de `main` (ruleset)

Réglages GitHub (Settings → Rules → Rulesets), pas accessibles depuis les outils de
cette session — à configurer manuellement :

- Restrict deletions, block force pushes, require linear history : activés.
- Require status checks to pass : le check `build` (`android-ci.yml`) requis avant de
  merger une PR.
- Require a pull request before merging : activé, avec une **exception de bypass** pour
  le commit de version automatique poussé directement sur `main` par `release.yml`
  (voir ci-dessous). Le `GITHUB_TOKEN` par défaut des Actions **n'est pas éligible** au
  bypass d'un ruleset, quel que soit le compte (personnel ou organisation) — il faut un
  deploy key ou une GitHub App dédiée.

### Deploy key pour le bot de release

1. Une paire de clés ed25519 dédiée est générée (`badgemoi-release-bot`, jamais commitée).
2. La **clé publique** est ajoutée en tant que *Deploy key* du dépôt (Settings → Deploy
   keys → Add deploy key), avec **"Allow write access"** coché.
3. La **clé privée** est stockée dans un secret Actions nommé `DEPLOY_KEY` (Settings →
   Secrets and variables → Actions → New repository secret).
4. Ce deploy key est ajouté à la **bypass list** du ruleset sur `main` (Settings → Rules
   → Rulesets → éditer → Bypass list → Deploy keys) — il n'apparaît dans la liste qu'une
   fois ajouté au dépôt (étape 2). Mode de bypass : **Exempt**, pas "Always" — "Always"
   est un bypass "coup de poing" avec signal d'audit à chaque usage, pensé pour une
   action humaine ponctuelle ; "Exempt" contourne la règle silencieusement, prévu par
   GitHub spécifiquement pour l'automatisation de confiance à haute fréquence (notre
   bot de release, qui pousse à chaque merge sur `main`).
5. `release.yml` utilise ce deploy key pour le checkout (`ssh-key: ${{ secrets.DEPLOY_KEY }}`),
   ce qui fait passer les `git push` de `@semantic-release/git` en SSH authentifié par le
   deploy key. Seul le push du commit de version bump a besoin de ce bypass — la création
   de la Release GitHub et l'upload de l'APK (`@semantic-release/github`) passent par
   l'API REST avec le `GITHUB_TOKEN` standard, non concernés par le ruleset de branche.

## Versioning automatique

[semantic-release](https://semantic-release.gitbook.io/) avec le plugin
[`semantic-release-gitmoji`](https://github.com/momocow/semantic-release-gitmoji)
détermine le prochain numéro de version à partir des emojis des commits sur `main`
depuis le dernier tag (config : [`.releaserc.js`](../.releaserc.js)).

| Emoji | Effet sur la version |
|---|---|
| 💥 | Majeure (breaking change) |
| ✨ | Mineure (nouvelle fonctionnalité) |
| 🐛 · ♻️ · 💄 · 🚀 | Patch (correctif, refactor, UI, perf) |
| 📝 · ✅ · 🔧 · autres | Aucune release déclenchée à elles seules |

Un push sur `main` sans commit "releasable" depuis le dernier tag ne produit aucune
release (comportement normal, pas une erreur).

## Pipeline de release

Déclenché par [`.github/workflows/release.yml`](../.github/workflows/release.yml) à
chaque push sur `main` (donc après chaque squash merge) :

1. `semantic-release-gitmoji` analyse les commits et calcule le prochain numéro de
   version (tag `vX.Y.Z`).
2. `scripts/bump-version.sh` met à jour `versionName`/`versionCode` dans
   `app/build.gradle.kts`, puis `./gradlew assembleDebug` construit l'APK.
3. Le changement de version est commité sur `main` (`🔖(release): vX.Y.Z [skip ci]` —
   le `[skip ci]` évite de redéclencher CI/release sur ce commit).
4. Une **GitHub Release** est créée sur le tag `vX.Y.Z`, notes générées depuis les
   commits, avec l'APK joint en asset téléchargeable.

## Distribution

- **Aujourd'hui** : APK de test (build debug, non signé release) distribué via les
  [GitHub Releases](https://github.com/The-White-Sock/BadgeMoi/releases) du dépôt.
  Suffisant pour l'usage personnel pendant le développement (lots 1 à 7).
- **F-Droid** (prochaine étape après une version fonctionnelle, cahier des charges
  §4.10) : soumission d'une recette de métadonnées au dépôt `fdroiddata`. F-Droid build
  et signe lui-même depuis les tags git — pas de gestion de clé de signature de notre
  côté pour ce canal. Le projet remplit déjà les critères d'inclusion (licence GPLv3,
  100 % hors-ligne, aucune dépendance propriétaire).
- **Play Store** (à terme) : nécessitera une clé de signature de release gérée séparément
  (Play App Signing) et un `.aab` signé — non mis en place tant que F-Droid n'est pas
  livré.

## Limite connue

Les notes de release générées par `semantic-release-gitmoji` utilisent le gabarit par
défaut de l'outil (anglais). Les personnaliser en français nécessiterait un template
Handlebars dédié — pas fait pour l'instant, à reconsidérer si ça devient gênant.
