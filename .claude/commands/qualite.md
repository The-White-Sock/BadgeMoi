---
description: Les quatre tâches Gradle et la cohérence des docs, en une passe
---

Vérifie le travail en cours. C'est l'étape 1 de `/pousser`, isolée pour être lançable
seule — au milieu d'un développement, on veut savoir si c'est vert sans enclencher tout
le rituel de livraison.

```bash
./gradlew ktlintCheck detekt testDebugUnitTest assembleDebug
```

Les quatre dans le même appel : Gradle réutilise la configuration et la compilation, et
quatre invocations séparées coûtent plusieurs minutes de plus pour le même résultat.

Si des fichiers de `docs/`, `CLAUDE.md` ou `README.md` ont changé :

```bash
./scripts/check-docs-coherence.sh
```

## Lecture des échecs

- **Style seul** (`ktlintCheck` rouge, le reste vert) → `./gradlew ktlintFormat` puis
  relancer. Ne pas corriger le style à la main.
- **`detekt`** → lire la règle citée avant de la désactiver. `config/detekt/detekt.yml`
  ne contient que des écarts délibérés ; en ajouter un se justifie dans le commit.
- **Test rouge** → lire `.claude/rules/tests.md` avant de toucher au test. Deux défauts
  déjà commis ici : le test tautologique qui réimplémente la logique qu'il vérifie, et le
  double infidèle qui diverge du vrai DAO.
- **Erreur d'encodage sur un nom de test accentué** → démon Kotlin périmé lancé sans
  locale : `./gradlew --stop && pkill -f KotlinCompileDaemon`, puis relancer.

## Ensuite

Tout vert et le travail est fini → `/pousser`. Tout vert mais le diff touche l'UI ou
contredit peut-être le cahier → `/revue` d'abord.

La CI reste l'arbitre final : environnement propre, réseau complet. Ce que cette commande
achète, c'est l'aller-retour évité.
