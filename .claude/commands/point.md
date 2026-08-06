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

Le hook `InstructionsLoaded` journalise chaque chargement, avec son `file_path`, son
`memory_type` et son `load_reason`. Trois interrogations, à recopier telles quelles :

```bash
journal=.git/badgemoi-instructions.log
session=$(tail -1 "$journal" | cut -f2 | jq -r '.session_id // empty')

# Ce qui s'est chargé dans cette séance, et sous quelle raison
cut -f2 "$journal" | jq -rR --arg s "$session" \
  'fromjson? | select(.session_id == $s) | "\(.load_reason)\t\(.file_path | split("/") | last)"' \
  | sort | uniq -c

# Le cumul, toutes les séances que ce conteneur a vues
cut -f2 "$journal" | jq -rR 'fromjson? | .load_reason // "inconnue"' | sort | uniq -c

# Les règles qu'aucune séance n'a jamais chargées
comm -13 \
  <(cut -f2 "$journal" | jq -rR 'fromjson? | .file_path // empty' | sed 's|.*/||' | sort -u) \
  <(ls .claude/rules/*.md | sed 's|.*/||' | sort)
```

`jq -rR` avec `fromjson?` est délibéré : le hook garde une ligne brute quand l'entrée
n'est pas du JSON, et sans ce filtre une seule ligne de ce genre ferait échouer tout le
bloc. Elle est ignorée en silence.

**Le journal vit dans `.git/`, donc il meurt avec le conteneur.** En session web le dépôt
est recloné à neuf : le fichier repart vide, et « toutes séances confondues » ne couvre
en réalité que les séances de ce conteneur-là. Un cumul plus bas que celui annoncé par la
passation précédente ne dit donc rien du harnais — il dit qu'on a changé de machine. La
mesure qui compte, elle, est intacte : le cumul **de la séance courante** est ce qui
approche le budget d'instructions, et il est complet par construction.

Ce qu'on en tire pour la passation :

- **Une règle jamais chargée alors que sa zone a été touchée** est un défaut de glob, pas
  une fatalité. `./scripts/check-docs-coherence.sh` détecte déjà le motif qui ne
  correspond à aucun fichier suivi ; le lancer avant de conclure. La condition « alors que
  sa zone a été touchée » porte tout le sens : sur un conteneur neuf, la troisième
  interrogation liste les cinq règles parce que le journal est vide, pas parce que les
  globs sont cassés. Ne rien conclure d'une liste pleine sans avoir ouvert un fichier de
  la zone d'abord.
- **Une raison `compact` absente** après une compaction confirme la non-réinjection des
  règles à `paths:` — c'est la vérification de la section précédente, faite sur pièce.
- **Le cumul** est l'indicateur à surveiller. Le nombre d'instructions qu'un modèle suit
  de façon fiable est fini et la dégradation est uniforme : au-delà d'un certain cumul, ce
  ne sont pas les dernières règles qui passent à la trappe, ce sont toutes. Le chargement
  étant dédupliqué par séance, ce compte est bien celui des règles **distinctes** en
  contexte — pas un compte d'injections répétées.

Le mécanisme lui-même est acquis : les règles se chargent bien sur `path_glob_match`, et
le glob se déclenche sur le **chemin visé**, pas sur l'existence du fichier. Un journal
vide alors que des fichiers ont été ouverts est donc anormal, et se dit dans la passation.

## Forme

Rendre la note **dans la réponse**, en markdown, prête à être collée dans le prompt
suivant. Ne pas créer de fichier : une passation périmée traînant dans le dépôt est pire
que pas de passation.

Concis mais complet — ce qui n'est pas écrit ici sera reconstruit à partir du code, et
la reconstruction se trompe.
