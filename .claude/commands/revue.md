---
description: Relecture adverse du diff par deux relecteurs à contexte neuf
---

Relis le travail en cours **contre** les règles du dépôt, avant de le livrer. Portée
éventuelle : `$ARGUMENTS` (par défaut, tout le diff par rapport à `origin/main`).

L'intérêt n'est pas de relire une seconde fois avec les mêmes yeux : c'est de faire lire
par un contexte qui n'a pas écrit le code. Celui qui vient d'implémenter sait ce qu'il a
voulu faire, et lit donc son intention plutôt que son diff.

## 1. Qualité d'abord

Lancer `/qualite`. Inutile de faire relire un diff qui ne compile pas.

## 2. Les deux relecteurs, en parallèle

Rassembler le diff, puis lancer **dans le même message** :

- l'agent `relecteur-ergonomie` — si et seulement si le diff touche `ui/**` ;
- l'agent `gardien-du-cahier` — toujours.

Leur passer le diff et la liste des fichiers touchés. Ils sont en lecture seule : ils
rapportent, ils ne corrigent pas.

## 3. Synthèse

Rendre **une** liste, chaque point classé :

- **à corriger** — contredit une règle écrite, sans contrepartie ;
- **à assumer** — contredit une règle, mais délibérément. Si c'est le cahier qui est
  contredit, alors `/ecart` est obligatoire, pas optionnel ;
- **écarté** — le relecteur s'est trompé. Dire pourquoi, en une ligne.

Ne pas corriger en cours de relecture : la liste d'abord, les corrections ensuite. Un
diff qui bouge pendant sa propre relecture rend les deux rapports caducs.

Les relecteurs ont un contexte neuf, donc pas de mémoire de la séance : un point écarté
reviendra à la prochaine `/revue`. C'est le prix du regard neuf, pas un défaut.
