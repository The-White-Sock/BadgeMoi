# BadgeMoi

Application Android (Kotlin + Jetpack Compose) pour chronométrer un trajet
domicile-travail en Onewheel, jalon par jalon.

- Cahier des charges : [`docs/cahier-des-charges.md`](docs/cahier-des-charges.md)
- POC HTML de référence (design/ergonomie) : [`docs/poc/trajet.html`](docs/poc/trajet.html)
- Conventions du dépôt (stack, structure, nommage, commandes) : [`docs/conventions.md`](docs/conventions.md)
- Cinématique de publication (branches, versioning, release, distribution) : [`docs/publication.md`](docs/publication.md)

## Polices

L'application embarque deux polices variables, sous **SIL Open Font License 1.1** :

- [Manrope](https://github.com/sharanda/manrope) — texte d'interface ;
- [JetBrains Mono](https://github.com/JetBrains/JetBrainsMono) — valeurs chiffrées.

Le texte des licences est distribué avec l'application (`app/src/main/res/raw/`).
Les polices sont embarquées plutôt que chargées à distance : l'application n'émet
aucune requête réseau et ne déclare pas la permission `INTERNET`.

## Compilation

```bash
./gradlew assembleDebug
```

Nécessite Android Studio / le SDK Android en local (compileSdk 37, JDK 17).

Sans environnement de build local, chaque push et chaque PR produisent un APK de
test téléchargeable depuis l'onglet **Actions** → run concerné → section **Artifacts**
(rétention 30 jours). Voir [`docs/publication.md`](docs/publication.md) pour la
distinction entre ces builds de test et les releases officielles.
