# BadgeMoi

Application Android (Kotlin + Jetpack Compose) pour chronométrer un trajet
domicile-travail en Onewheel, jalon par jalon.

- Cahier des charges : [`docs/cahier-des-charges.md`](docs/cahier-des-charges.md)
- POC HTML de référence (design/ergonomie) : [`docs/poc/trajet.html`](docs/poc/trajet.html)
- Conventions du dépôt (structure, nommage, commandes) : [`CLAUDE.md`](CLAUDE.md)

## Build

```bash
./gradlew assembleDebug
```

Nécessite Android Studio / le SDK Android en local (compileSdk 37, JDK 17).
