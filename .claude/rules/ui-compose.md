---
paths:
  - "app/src/main/kotlin/**/ui/**/*.kt"
---

# Écrans et composants Compose

Digest de rappel. `docs/cahier-des-charges.md` et `docs/ergonomie.md` restent les
sources ; en cas de contradiction, ce sont eux qui ont raison.

## Interdits

- **Aucune couleur littérale** (`Color(0xFF…)`) hors de `ui/theme/`. Tout passe par
  `MaterialTheme.colorScheme` ou `BadgeMoiTheme.extendedColors` (cahier §5).
- **Aucun texte en dur.** Chaque libellé visible est une ressource de chaîne,
  nommée `<ecran>_<usage>`. Un mot déjà employé ailleurs pour la même grandeur se
  **partage** au lieu de se dupliquer sous un second nom.
- **Aucune liste paresseuse dans une zone défilante.** Un `LazyColumn` mesuré sous
  une hauteur maximale infinie fait planter Compose. Dans un `verticalScroll`, la
  liste reste une `Column`.

## Placement

`docs/ergonomie.md` **fait autorité** sur le placement de tout élément interactif.
Le cahier dit quoi afficher, l'ergonomie dit où le poser.

- Action primaire : bouton pleine largeur ancré en bas.
- Confirmation et saisie : en bas — donc feuille basse, pas fenêtre centrée.
- Action destructive : volontairement hors de la zone du pouce, mais découvrable.
- Toute cible : **48 dp** minimum, **8 dp** entre deux cibles distinctes.

## Attendus

- Un aperçu `@Preview` en thème **jour et nuit** pour tout composant nouveau.
- Un état vide, absent ou non mesuré s'affiche par le tiret `milestone_no_value`,
  jamais par une chaîne vide : la ligne garde sa hauteur et l'absence se distingue
  d'un oubli d'affichage.
- Rendre un élément cliquable lui impose ses 48 dp **au même commit**. C'est le
  défaut qui est passé deux fois (`SegmentList`, `RecentTripList`).
