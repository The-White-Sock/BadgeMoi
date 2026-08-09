---
paths:
  - ".github/workflows/**"
  - ".github/dependabot.yml"
  - "gradle/**"
  - "**/build.gradle.kts"
  - "settings.gradle.kts"
---

# CI, build et dépendances

`docs/conventions.md` § « Sécurité et automatisation CI » est la source. Rappels des
pièges qui ne se voient pas à la lecture du diff.

## Trois choses à ne pas casser

- **Plafond Kotlin < 2.4.10.** L'extracteur Kotlin du bundle CodeQL le refuse
  au-delà et fait échouer le job `analyze`. Ce plafond est **doublé d'une exclusion
  Dependabot** sur les deux plugins Kotlin : lever l'un sans l'autre laisse soit une
  PR rouge chaque semaine, soit plus aucune alerte de version. Les deux se retirent
  **ensemble**.
- **Pas de `--build-cache` sur le job CodeQL.** Une compilation servie par le cache
  n'invoque pas le compilateur, et CodeQL n'a alors rien à tracer. Le cache de
  *dépendances*, lui, reste actif — ne pas le désactiver non plus.
- **Actions épinglées par SHA complet**, jamais par tag flottant. Format :
  `uses: owner/action@<sha> # vX.Y.Z`.

## Versions

Toutes les versions vivent dans `gradle/libs.versions.toml`. Jamais de numéro écrit
en dur dans un `build.gradle.kts` : passer par `alias(libs.plugins.x)` / `libs.x.y`.

Toute version annoncée dans `docs/conventions.md` est vérifiée chaque semaine par
`scripts/check-docs-coherence.sh` — la changer sans mettre la doc à jour ouvre une
issue d'audit.

## Si une dépendance d'outillage de test apparaît

`targetSdk = 37` **ferme l'outillage de capture d'écran**. Mesuré en instruisant #149, et
écrit nulle part ailleurs qui se charge en séance — d'où ce rappel.

- **`compileSdk` et `targetSdk` ne jouent pas le même rôle ici.** Le premier est **sans
  effet** sur l'outillage de test ; le second est **bloquant**. C'est cette distinction
  qui a débloqué le sujet, là où chercher à contourner la contrainte n'y était pas
  parvenu.
- Robolectric 4.16.1 (stable) refuse le projet :
  `Package targetSdkVersion=37 > maxSdkVersion=36`.
- Les autres portes sont **mesurées et fermées** : Roborazzi est bâti sur Robolectric et
  hérite du plafond ; Paparazzi n'offre qu'une `2.0.0-alpha` ; la 4.17-beta-2 accepte
  l'API 37 puis casse sur `Failed to interact with raw FileDescriptor internals` — en
  JDK 21 alors que la CI est en 17, ce qui rend cet échec **ininterprétable** (#159).

**Ne pas trancher ici.** `targetSdk` n'a jamais été arbitré : posé par `df72902`, le commit
le plus ancien du dépôt, jamais retouché depuis. C'est #158 qui doit écrire la décision au
§9 du cahier — **quelle que soit** la valeur retenue, rester en 37 compris. #149 en dépend.

## Si un module Gradle apparaît

<important if="le diff ajoute un module Gradle ou un répertoire racine">

Trois régressions **silencieuses** attendent au tournant, aucune ne produit
d'erreur (voir #123) :

1. `./gradlew testDebugUnitTest` est une tâche de variante Android. Un module JVM
   n'en a pas — ses tests cessent d'être exécutés sans que rien n'échoue.
2. ktlint et detekt sont appliqués **dans `app/build.gradle.kts`**, pas à la racine.
   Un module qui ne les applique pas n'est pas analysé.
3. Le filtre de chemins de `codeql.yml` liste `^app/|^gradle/|…`. Un nouveau
   répertoire racine n'y correspond pas, et l'analyse est sautée.

</important>

<!-- La balise ci-dessus est un **prototype**, seul du dépôt, et ne fait pas
     jurisprudence. Elle ne retire rien : si elle est ignorée, la section se lit
     comme avant. Ce qu'elle cadre était déjà conditionnel dans son titre — c'est ce
     qui en fait le bloc le moins risqué à éprouver. Critère de révision et ce qui
     est mesurable : bloc de maintenance de `CLAUDE.md`. -->

