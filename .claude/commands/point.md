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

**Relevé au journal sur une compaction réelle**, plus seulement lu dans la
documentation :

- Le `CLAUDE.md` racine est relu depuis le disque et ré-injecté. La compaction le
  journalise sous `session_start` — **il n'existe pas de raison `compact`**. Rien ne
  distingue donc au journal une compaction d'un démarrage de séance, sinon qu'elle
  survient en cours de route ; et un `session_start` sur `CLAUDE.md` est la **borne**
  d'une fenêtre de contexte.
- **Les règles à `paths:` ne sont pas ré-injectées.** Aucune ne réapparaît à la
  compaction. On peut donc, juste après, écrire du Compose sans que `ui-compose.md`
  soit chargé, ou toucher au domaine sans `domain-purity.md`.
- **Le remède fonctionne**, et c'était le point douteux : rouvrir un fichier de la zone
  ré-émet bien un `path_glob_match`, y compris pour une règle déjà chargée plus tôt
  dans la **même** séance. La déduplication porte sur la fenêtre de contexte, pas sur
  la séance, et une compaction la remet à zéro. Sans ça le remède aurait été inopérant
  là où on en a le plus besoin.

Le remède est donc mécanique et vérifié : rouvrir un fichier de la zone concernée avant
d'y écrire.

## Ce qui s'est réellement chargé

Le hook `InstructionsLoaded` journalise chaque chargement, avec son `file_path`, son
`memory_type` et son `load_reason`. Trois interrogations, à recopier telles quelles :

```bash
journal=.git/badgemoi-instructions.log

# Ce qui est en contexte **maintenant** : la fenêtre courante, bornée par le dernier
# `session_start`. C'est le seul relevé qui réponde à « ai-je cette règle sous les yeux ».
cut -f2 "$journal" \
  | jq -rR 'fromjson? | "\(.load_reason)\t\(.file_path | split("/") | last)"' \
  | awk '/^session_start\t/ { n = 0 } { l[n++] = $0 } END { for (i = 0; i < n; i++) print l[i] }' \
  | sort -u

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

**L'`awk` de la première interrogation n'est pas un raffinement, il répare une réponse
fausse.** Cette interrogation filtrait avant sur le `session_id`, qui ne change pas à la
compaction : elle mélangeait donc toutes les fenêtres d'une même séance et annonçait
comme chargées des règles évincées depuis longtemps — le contraire de ce qu'on lui
demande. Ce `awk` vide son tampon à chaque `session_start` et ne garde que la dernière
fenêtre. Le `sort -u` remplace le `uniq -c` pour la même raison : à l'intérieur d'une
fenêtre le chargement est dédupliqué, un compte n'y apporte rien et un compte supérieur
à 1 n'y signifierait rien.

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
- **Plusieurs `session_start` sur `CLAUDE.md`** ne sont pas une anomalie : c'est le
  compte des compactions traversées, plus une ou deux au démarrage. Ne pas chercher la
  raison `compact`, elle n'est jamais émise.
- **La fenêtre courante** est l'indicateur à surveiller, pas le cumul. Le nombre
  d'instructions qu'un modèle suit de façon fiable est fini et la dégradation est
  uniforme : au-delà d'un certain seuil, ce ne sont pas les dernières règles qui passent
  à la trappe, ce sont toutes. Seule la première interrogation compte des règles
  **réellement présentes** ; le cumul, lui, additionne des fenêtres révolues et gonfle à
  chaque compaction sans que rien ne s'accumule en contexte.

Le mécanisme lui-même est acquis : les règles se chargent bien sur `path_glob_match`, et
le glob se déclenche sur le **chemin visé**, pas sur l'existence du fichier. Un journal
vide alors que des fichiers ont été ouverts est donc anormal, et se dit dans la passation.

## Forme

Rendre la note **dans la réponse**, en markdown, prête à être collée dans le prompt
suivant. Ne pas créer de fichier : une passation périmée traînant dans le dépôt est pire
que pas de passation.

Concis mais complet — ce qui n'est pas écrit ici sera reconstruit à partir du code, et
la reconstruction se trompe.
