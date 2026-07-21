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
