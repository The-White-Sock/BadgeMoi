---
description: Consigner au §9 du cahier une décision qui contredit le périmètre d'origine
---

Consigne dans `docs/cahier-des-charges.md` §9 une décision qui **contredit la lettre**
du cahier. Sujet : `$ARGUMENTS`.

Un écart non consigné devient une incohérence que quelqu'un « corrigera » plus tard,
en toute bonne foi, en rétablissant la règle d'origine.

## Les quatre gestes, tous obligatoires

Ils doivent rester d'accord entre eux — c'est justement ce qui se désynchronise
quand on les fait de mémoire.

**1. La ligne du tableau.** Ajouter une ligne numérotée au tableau des décisions,
en prenant le numéro suivant :

```
| N | <Point concerné> | **<Décision>** — <précision courte>, voir #<issue> |
```

**2. La section de prose.** Sous « Écarts assumés », ajouter une section :

```
**N. <Titre affirmatif.>** <Ce que disait la règle d'origine.>

<Ce que la règle protégeait, et pourquoi elle ne le protège plus ici.>

<La contrepartie assumée.>
```

Une section qui n'énonce que la nouvelle règle est inutile : ce qui se perd, c'est
la **raison**. Écrire ce que l'ancienne règle défendait, et pourquoi cette défense
ne s'applique plus.

**3. Le compte.** Mettre à jour la phrase d'introduction des sections — « Ces N
points contredisent la lettre… » — ainsi que la plage du titre (« 6 à N »).

**4. Le renvoi.** Depuis la section fonctionnelle concernée (§3.x ou §4.x), ajouter
`(§9, écart N)` pour que quelqu'un qui lit la spécification tombe sur la dérogation
sans avoir à connaître son existence.

## Vérification

```bash
./scripts/check-docs-coherence.sh
```

Il vérifie mécaniquement les gestes 1, 2 et 3. Le geste 4 reste à la relecture.
