---
paths:
  - "app/src/test/**/*.kt"
---

# Tests unitaires

## Deux défauts déjà commis dans ce dépôt

**1. Le test tautologique.** Un test ne doit pas réimplémenter la règle qu'il
prétend vérifier, puis la comparer à elle-même. C'est arrivé sur l'échappement CSV :
le test reconstruisait la chaîne échappée avec la même logique que le code. Il
passait, et n'éprouvait rien.

Le remède retenu : rendre la fonction `internal` et l'éprouver **directement**, avec
des valeurs attendues écrites en clair. Une visibilité élargie pour le test vaut
mieux qu'un test qui ne teste pas. Voir `TripCsv.escape` et son test.

**2. La doublure infidèle.** `FakeArchiveRepository.add` ajoutait à la liste là où
le DAO Room **remplace** en cas de conflit d'identifiant. La doublure était plus
permissive que le vrai dépôt, et masquait donc une correction perdue sur un trajet
archivé.

Avant d'écrire une doublure, lire l'implémentation réelle et reproduire sa
sémantique — en particulier sur les conflits, les remplacements et les ordres.

## Attendus

- Les noms de tests sont des phrases françaises entre accents graves.
- Un test de reproduction porte la mention du défaut qu'il fige.
- Un comportement partagé par deux appelants se teste **une fois**, sur le code
  partagé, pas deux fois sur chaque appelant.

## Locale

Les noms de méthodes accentués produisent des fichiers `.class` accentués : sans
locale UTF-8, le compilateur Kotlin échoue. Le hook `SessionStart` pose
`LANG=C.UTF-8` — aucun préfixe manuel n'est requis.

Si l'erreur apparaît malgré tout, c'est qu'un démon Kotlin lancé avant le hook a
hérité de l'ancienne locale. Il faut repartir d'un démon neuf :

```bash
./gradlew --stop && pkill -f KotlinCompileDaemon
```
