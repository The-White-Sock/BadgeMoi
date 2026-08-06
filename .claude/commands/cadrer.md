---
description: Fixer la spec d'une demande floue avant d'écrire la moindre ligne
---

Transforme une demande en spec exécutable. Demande : `$ARGUMENTS`.

Le coût d'une ambiguïté n'est pas dans la question qu'elle fait poser, il est dans les
trois quarts d'heure d'implémentation qui partent dans la mauvaise direction avant que
quelqu'un s'en aperçoive. Cette commande paye la question d'abord.

## 1. Lire avant de demander

Ne jamais poser une question dont la réponse est déjà écrite. Vérifier dans l'ordre :

- `docs/cahier-des-charges.md` §9 — le point est peut-être **déjà tranché**. Un point
  tranché ne se rouvre pas sans validation explicite.
- `docs/ergonomie.md` — fait autorité sur le **placement** de tout élément interactif.
  Le cahier dit quoi afficher, l'ergonomie dit où le poser. La question « où mettre ce
  bouton » a presque toujours sa réponse ici.
- `docs/conventions.md` — nommage, structure, stack.

Ce qui est déjà écrit se cite, ne se redemande pas.

## 2. Demander ce qui reste

Utiliser `AskUserQuestion` pour ce qui est réellement ouvert, avec une recommandation en
première position quand il y en a une. Les questions qui paient :

- **Périmètre** — qu'est-ce qui est explicitement *hors* de cette demande ?
- **Cas limites** — zéro élément, un seul, valeur absente, trajet en cours, hors ligne.
  Ce sont eux qui décident de la forme du code, pas le cas nominal.
- **Cas d'erreur** — que voit-on quand ça échoue ?
- **Migration** — que deviennent les données déjà en base ?

Ne pas demander ce qu'on peut trancher soi-même avec un défaut raisonnable : annoncer le
défaut et avancer.

## 3. Rendre la spec

Court, en français, dans la réponse :

- **Comportement attendu**, cas nominal puis cas limites ;
- **Critères d'acceptation** vérifiables — « le bouton fait 48 dp » se vérifie, « c'est
  ergonomique » non ;
- **Hors périmètre**, explicite ;
- **Écart éventuel** : si la spec contredit le cahier, le signaler ici. La consignation
  passe par `/ecart`, dans le même lot que le code.

Terminer par un **texte d'issue prêt à coller** : titre à l'impératif, corps reprenant
les critères.

## 4. Passer la main

La spec arrêtée, enchaîner sur le mode plan pour l'implémentation. Cadrer et implémenter
dans le même souffle est exactement ce que cette commande sert à éviter.
