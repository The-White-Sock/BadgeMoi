---
description: Passation avant un /clear, un rewind ou une fin de séance
---

Écris la note que le Claude suivant aimerait trouver. Sujet éventuel à cadrer :
`$ARGUMENTS`.

Une séance longue perd en précision bien avant de perdre sa place : la passation se fait
donc **avant** que le contexte soit saturé, pas quand il craque. C'est aussi ce qu'il
faut écrire avant un rewind — un mot du Claude présent à son propre passé.

## Ce que la note contient

**1. L'objectif.** Ce qu'on cherche à obtenir, et l'issue visée s'il y en a une. Une
phrase.

**2. Fait.** Ce qui est acquis et vérifié. Distinguer ce qui est *écrit* de ce qui est
*vert* : un fichier modifié n'est pas un test qui passe.

**3. Reste à faire.** Dans l'ordre où il faut le reprendre, avec le premier geste
concret — pas « finir l'écran » mais « brancher `HistoryViewModel.onSelect` sur le
bouton ».

**4. Fichiers touchés.** Chemins réels, avec ce qui a changé dans chacun. C'est ce qui
coûte le plus cher à retrouver.

**5. Décisions prises en séance.** Les choix arrêtés et leur raison. Sans ça ils seront
refaits à l'envers, de bonne foi. Si l'un d'eux contredit le cahier, dire s'il est déjà
consigné au §9 ou s'il reste à passer par `/ecart`.

**6. Pistes écartées.** Ce qui a été essayé sans marcher, et pourquoi. C'est la partie
qu'on oublie d'écrire et qu'on repaye intégralement.

**7. État git.** Branche, ce qui est commité, ce qui est poussé, l'état de la PR.

## Ce qui survit à une compaction

À vérifier **avant** de conclure, parce que c'est invisible autrement :

- Le `CLAUDE.md` racine est relu depuis le disque et ré-injecté après un `/compact`.
- **Les règles à `paths:` ne le sont pas.** Elles attendent qu'un fichier correspondant
  soit relu. Après une compaction, on peut donc écrire du Compose sans que
  `ui-compose.md` soit chargé, ou toucher au domaine sans `domain-purity.md`.

Le remède est mécanique : rouvrir un fichier de la zone concernée avant d'y écrire.

## Ce qui s'est réellement chargé

Le hook `InstructionsLoaded` écrit une ligne par chargement : un horodatage, une
tabulation, puis l'événement JSON. Chaque événement porte **un seul** `file_path`, un
`memory_type` et un `load_reason`.

```bash
journal=.git/badgemoi-instructions.log

# Pourquoi — une ligne par raison de chargement.
cut -f2 "$journal" | jq -r '.load_reason // "raison absente"' | sort | uniq -c | sort -rn

# Quoi — chaque fichier d'instructions chargé, et combien de fois.
cut -f2 "$journal" | jq -r '.file_path // "chemin absent"' | sort | uniq -c | sort -rn

# Ce qui manque — les règles à `paths:` qui ne se sont jamais chargées.
comm -23 <(ls .claude/rules/*.md | xargs -n1 basename | sort) \
         <(cut -f2 "$journal" | jq -r '.file_path // empty' | xargs -r -n1 basename | sort -u)
```

En tirer trois constats pour la passation :

- **Ce qui manque.** La troisième commande est la plus utile : une règle qui n'apparaît
  jamais alors qu'on a passé la séance dans sa zone ne se charge pas, et tout ce qu'elle
  contient est lettre morte. À ne pas confondre avec une règle simplement hors sujet —
  vérifier d'abord qu'un fichier de sa zone a bien été ouvert.
- **Sous quelle raison.** `path_glob_match` prouve qu'une règle s'est déclenchée sur le
  chemin d'un fichier ouvert ; `session_start` ne concerne que ce qui se charge d'office.
  Une entrée `compact` confirme un rechargement post-compaction, son absence le dément.
- **Combien** se sont cumulées. Le nombre d'instructions qu'un modèle suit de façon fiable
  est fini et la dégradation est uniforme : au-delà d'un certain cumul, ce ne sont pas les
  dernières règles qui passent à la trappe, ce sont toutes.

Journal vide alors que des fichiers ont été ouverts → le dire dans la passation. C'est un
défaut de harnais, pas un détail. Idem pour un `raison absente` ou un `chemin absent` en
nombre : le schéma aurait bougé en amont, et c'est le journal brut qui le dira.

## Forme

Rendre la note **dans la réponse**, en markdown, prête à être collée dans le prompt
suivant. Ne pas créer de fichier : une passation périmée traînant dans le dépôt est pire
que pas de passation.

Concis mais complet — ce qui n'est pas écrit ici sera reconstruit à partir du code, et
la reconstruction se trompe.
