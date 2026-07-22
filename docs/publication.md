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

Réglage GitHub configuré (Settings → General → Pull Requests) : "Allow merge commits"
et "Allow rebase merging" décochés, seule "Allow squash merging" reste active — le dépôt
n'accepte donc que le squash merge.

## Protection de `main` (ruleset)

Réglages GitHub (Settings → Rules → Rulesets), configurés :

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
   GitHub spécifiquement pour l'automatisation de confiance (notre bot de release,
   déclenché manuellement mais qui pousse ensuite sans intervention humaine le commit
   de version bump).
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

Déclencher le pipeline de release (§ suivante) sans commit "releasable" depuis le
dernier tag ne produit aucune release (comportement normal, pas une erreur).

## Builds de test (CI)

Chaque push et chaque PR déclenchent [`android-ci.yml`](../.github/workflows/android-ci.yml),
qui construit l'APK debug et le publie comme **artifact de workflow GitHub Actions**
(onglet Actions → run → Artifacts), téléchargeable et sideloadable pour tester une
branche en cours de dev. Rétention 30 jours, pas de tag, pas de numéro de version, pas
de GitHub Release : ça n'a aucun effet sur le pipeline de release ni sur l'historique
public des livrables. C'est le canal à utiliser pour tester avant qu'un changement soit
prêt à devenir une version officielle.

## Pipeline de release

Déclenché **manuellement** (`workflow_dispatch`) sur
[`.github/workflows/release.yml`](../.github/workflows/release.yml), depuis l'onglet
Actions. Volontairement pas automatique sur chaque push sur `main` : en dev solo
trunk-based, la quasi-totalité des PR mergées contiennent un commit "releasable"
(`✨`, `🐛`...) — un déclenchement automatique produirait une release numérotée à
chaque merge, y compris pour du travail intermédiaire pas encore prêt à être distribué.
Le déclenchement manuel réserve les releases aux moments où c'est réellement voulu (fin
de lot, version stable à distribuer) :

1. `semantic-release-gitmoji` analyse les commits et calcule le prochain numéro de
   version (tag `vX.Y.Z`) depuis le dernier tag — inchangé, que le déclenchement soit
   manuel ou automatique.
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

## Cycle de vie d'un livrable

Ce qui se passe après la publication d'une release, une fois `vX.Y.Z` créée.

- **Rétention** : les GitHub Releases sont conservées **indéfiniment**, aucune purge —
  le volume reste faible pour un projet solo et l'historique sert de changelog. La
  rétention des artifacts de build de test (§ précédente) est distincte et courte
  (30 jours) car ils sont reproductibles depuis n'importe quel commit.
- **Version défectueuse** : pas de suppression de la release fautive (elle fait partie
  de la traçabilité). Le correctif part sur une branche `fix/`, merge normal, la
  prochaine release patch (`🐛`) la remplace. Sur la release fautive elle-même, cocher
  a posteriori **"Set as a pre-release"** pour signaler qu'elle est déconseillée sans la
  retirer.
- **Mise à jour côté utilisateur** : tant que la distribution passe uniquement par
  GitHub Releases, l'APK sideloadé n'a pas d'auto-update — limite connue et acceptée,
  propre à ce canal transitoire (usage perso pendant le dev, lots 1 à 7). Elle disparaît
  d'elle-même au passage sur F-Droid, qui gère l'auto-update nativement — pas
  d'investissement à faire ici.
- **Fin de vie d'un canal** : les canaux de distribution sont **cumulatifs**, pas de
  dépréciation forcée. GitHub Releases reste actif même une fois F-Droid disponible ;
  F-Droid devient simplement le canal recommandé dans la doc utilisateur. Même logique
  plus tard pour Play Store.

## Limite connue

Les notes de release générées par `semantic-release-gitmoji` utilisent le gabarit par
défaut de l'outil (anglais). Les personnaliser en français nécessiterait un template
Handlebars dédié — pas fait pour l'instant, à reconsidérer si ça devient gênant.
