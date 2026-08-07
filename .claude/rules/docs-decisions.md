---
paths:
  - "docs/**/*.md"
  - "CLAUDE.md"
  - "README.md"
---

# Documentation et décisions

La langue du dépôt a sa propre règle, `langue.md`, sans `paths:` — elle valait pour
tout commit et toute PR alors qu'elle ne se chargeait qu'en ouvrant un `.md`.

## Consigner un écart au cahier

Un choix qui **contredit la lettre** du cahier des charges se consigne au §9, en
deux endroits qui doivent rester d'accord :

1. une ligne dans le **tableau** des décisions, numérotée ;
2. une **section de prose** `**N. Titre.**` qui donne la raison — ce que la règle
   d'origine protégeait, et pourquoi elle ne le protège plus ici ;
3. le compte annoncé juste avant les sections (« Ces N points ») ;
4. un renvoi `(§9, écart N)` depuis la section fonctionnelle concernée.

`scripts/check-docs-coherence.sh` vérifie mécaniquement 1, 2 et 3. Le 4 reste à la
relecture.

La commande `/ecart` fait les quatre.

## Ne pas rouvrir

Le §9 porte les points **déjà tranchés**. Ne pas les rediscuter sans validation
explicite : min SDK 29, F-Droid avant Play Store, pas de reprise de l'historique
web, widget dès le lot 6, Hilt pour l'injection.

## Écrire

- Dire ce que la décision protège, pas seulement ce qu'elle est. Une doc qui
  n'énonce que la règle se fait contourner dès que la règle gêne.
- Un chemin cité entre backticks doit exister : l'audit hebdomadaire le vérifie.
- Pas de duplication entre `docs/` et `.claude/rules/` — les règles sont des
  digests avec pointeurs, la source reste `docs/`.
