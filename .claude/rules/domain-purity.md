---
paths:
  - "app/src/main/kotlin/**/domain/**/*.kt"
---

# Pureté du domaine

`domain/` est du **Kotlin/JVM pur**. C'est un invariant vérifiable, pas une
préférence de style : c'est ce qui rendra l'extraction du module `:domain` (#123)
mécanique, et c'est ce qui permettrait un jour une seconde cible.

## Interdits

Aucun import de :

- `android.*` ni `androidx.*`
- `dagger.*` ni `javax.inject.*`
- annotation de persistance (`@Entity`, `@Dao`) ou de sérialisation (`@Serializable`)

## Où va ce qui est interdit

- La sérialisation vit en `data/local/`, derrière un DTO de stockage et son
  `toStored()`. Le modèle du domaine ne connaît pas sa forme persistée.
- L'injection se déclare en `di/`. Le domaine expose des interfaces, il ne les
  annote pas.

## Dépendances tolérées

`kotlin.*`, `java.time.*`, et `kotlinx.coroutines.flow.Flow` pour les seules
interfaces de dépôt. Toute autre dépendance se discute avant d'être ajoutée.

## Corollaire

Un identifiant de trajet est un `UUID` et un trajet archivé est **immuable**. Deux
archives se fusionnent donc par union sur l'identifiant, sans arbitrage — propriété
sur laquelle repose la piste Wear (#122). Ne pas la casser en rendant un trajet
archivé modifiable en place.
