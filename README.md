# BadgeMoi

Application Android (Kotlin + Jetpack Compose) pour chronométrer un trajet
domicile-travail en Onewheel, jalon par jalon.

- Cahier des charges : [`docs/cahier-des-charges.md`](docs/cahier-des-charges.md)
- POC HTML de référence (design/ergonomie) : [`docs/poc/trajet.html`](docs/poc/trajet.html)
- Conventions du dépôt (stack, structure, nommage, commandes) : [`docs/conventions.md`](docs/conventions.md)
- Cinématique de publication (branches, versioning, release, distribution) : [`docs/publication.md`](docs/publication.md)

## Compilation

```bash
./gradlew assembleDebug
```

Nécessite Android Studio / le SDK Android en local (compileSdk 37, JDK 17).

Sans environnement de build local, chaque push et chaque PR produisent un APK de
test téléchargeable depuis l'onglet **Actions** → run concerné → section **Artifacts**
(rétention 30 jours). Voir [`docs/publication.md`](docs/publication.md) pour la
distinction entre ces builds de test et les releases officielles.
