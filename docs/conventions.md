# Conventions du dépôt

Ce document rassemble la stack technique, la structure du projet et les conventions de
développement de BadgeMoi. Il fait autorité sur ces sujets pour tout contributeur,
humain ou agent — voir aussi [`cahier-des-charges.md`](cahier-des-charges.md) pour le
périmètre fonctionnel et les choix d'architecture.

## Langue

Tout le contenu du dépôt est en **français** : commentaires de code, KDoc, messages de
commit, description de PR, documentation (`docs/`, `README.md`, `CLAUDE.md`). Les
identifiants (classes, fonctions, variables, packages, noms de branches) restent en
anglais/technique par convention Kotlin/Android standard — seuls le texte libre et les
commentaires sont concernés.

## Stack

- Kotlin 2.3.20, Jetpack Compose + Material 3, AGP 9.2.0, Gradle 9.4.1 (JDK 17).
- Module unique `:app`, organisé **par fonctionnalité** (pas par couche technique) :
  pas de multi-module tant que le projet reste solo et de cette taille.
- Injection de dépendances : **Hilt**.
- Persistance : Room (archive des trajets) + DataStore Preferences (trajet en cours,
  thème) — partagée entre l'appli et le widget Glance. Sérialisation via
  `kotlinx-serialization` (`StoredTrip`). Le modèle du domaine (`domain/`) reste pur
  (aucune annotation Room/serialization) ; le mapping vit dans `data/local/`.
- 100% hors-ligne : ne jamais ajouter la permission `INTERNET` ni de dépendance réseau.

Toutes les versions sont centralisées dans `gradle/libs.versions.toml`. Ne jamais
écrire un numéro de version en dur dans un `build.gradle.kts` : passer par le catalogue
(`alias(libs.plugins.x)`, `libs.x.y`).

## Structure des packages (`fr.whitytoes.badgemoi`)

```
fr.whitytoes.badgemoi/
  BadgeMoiApplication.kt   # @HiltAndroidApp
  MainActivity.kt
  ui/
    theme/                 # Color.kt, Theme.kt, Type.kt — tokens du design system
    home/                  # écran d'accueil (lot 2)
    trip/                  # écran "trajet actif" (lot 3)
    summary/                # écran récapitulatif (lot 4)
    history/               # écran historique (lot 5)
    widget/                # widget Glance (lot 6)
  domain/                  # modèles + interfaces de repository (lot 1)
  data/                    # implémentations Room/DataStore (lot 1)
  di/                      # modules Hilt
```

Ne créez un package que lorsqu'il contient réellement du code — pas de dossiers vides
« au cas où ».

## Conventions de nommage

- **Kotlin** : style officiel Kotlin (appliqué par ktlint). `PascalCase` pour
  classes/objects/composables, `camelCase` pour fonctions/variables,
  `SCREAMING_SNAKE_CASE` pour les constantes top-level.
- **Composables** : un composable qui affiche un écran entier est suffixé `Screen`
  (ex: `TripActiveScreen`). Un composable privé interne à un fichier est préfixé par
  rien de spécial mais reste `private`.
- **Fichiers Kotlin** : un fichier = un type public principal, même nom
  (`TripViewModel.kt` contient `TripViewModel`). Exception : petits regroupements de
  composables très liés (ex: `Theme.kt` contient `BadgeMoiTheme` + son objet compagnon).
- **Ressources Android** : `snake_case` partout, préfixé par l'écran/le contexte —
  `ic_<nom>` (drawables vectoriels), `<ecran>_<usage>` pour les strings
  (ex: `trip_active_validate_button`), jamais de texte en dur dans les composables.
- **Couleurs** : jamais de `Color(0x...)` littéral dans un composable d'écran — tout
  passe par `MaterialTheme.colorScheme` ou `BadgeMoiTheme.extendedColors`
  (voir `ui/theme/Theme.kt`). C'est la règle explicite du cahier des charges §5.

## Commits et branches

- Messages de commit : **gitmoji**, format `<emoji>(<scope>): <description au présent>`.
  Emojis courants : `✨` (nouvelle fonctionnalité), `🐛` (correctif), `♻️` (refactor),
  `✅` (tests), `📝` (docs), `🔧` (config/outillage), `💄` (UI/visuel), `🚀` (perf),
  `💥` (breaking change). Exemple : `✨(trip): ajoute la validation séquentielle des jalons`.
  L'emoji détermine aussi le versioning automatique — voir
  [`publication.md`](publication.md).
- Branches : `feat/<sujet>`, `fix/<sujet>`, `chore/<sujet>` en anglais ou français
  court, cohérent avec le scope du commit principal.
- Les PR sont fusionnées vers `main` en **squash merge** — voir
  [`publication.md`](publication.md) pour la cinématique complète (branches, versioning,
  release, distribution).

## Qualité de code

```bash
./gradlew ktlintCheck   # style, formatage (ktlintFormat pour corriger automatiquement)
./gradlew detekt        # analyse statique (config/detekt/detekt.yml)
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

Ces quatre commandes tournent en CI sur chaque push/PR (`.github/workflows/android-ci.yml`).
Un build/lint local avant de pousser évite les allers-retours CI.

## Sécurité et automatisation CI

- **CodeQL** (`.github/workflows/codeql.yml`) : analyse statique de sécurité (Java/Kotlin),
  sur chaque push/PR vers `main` et une fois par semaine.
- **Dependabot** (`.github/dependabot.yml`) : met à jour automatiquement les dépendances
  Gradle (`gradle/libs.versions.toml`), npm (`package.json`) et les Actions GitHub.
- **Dependency Review** (`.github/workflows/dependency-review.yml`) : sur chaque PR,
  bloque l'introduction d'une dépendance connue comme vulnérable (sévérité modérée ou
  plus). Complémentaire de Dependabot, qui met à jour l'existant de façon planifiée
  alors que Dependency Review agit *avant* le merge sur ce qu'une PR ajoute.
- **Actions épinglées par SHA** : toute Action tierce dans un workflow (`.github/workflows/`)
  est référencée par son SHA de commit complet, jamais par un tag flottant (`@v4`) —
  un tag peut être déplacé, un SHA ne peut pas. Format :
  `uses: owner/action@<sha-complet> # vX.Y.Z` (le commentaire de version est pour la
  lecture humaine, Dependabot se charge de garder le SHA à jour).

## Points déjà tranchés (ne pas redemander)

Voir [`cahier-des-charges.md`](cahier-des-charges.md) §9 : min SDK 29, distribution
F-Droid en premier puis Play Store, pas de migration de l'historique web, widget
d'écran d'accueil dès le lot 6. DI : Hilt (décidé lors de la mise en place de la
structure du repo).
