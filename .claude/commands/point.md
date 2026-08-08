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
  survient en cours de route.
- La borne d'une fenêtre de contexte est le **début d'une suite** de `session_start`,
  pas chacune de ses lignes. La nuance n'est pas théorique : depuis que `langue.md`
  existe, une fenêtre en ouvre **deux** — `CLAUDE.md` puis la règle non scopée — et
  toute règle qui viendra la rejoindre en ajoutera une.
- **Les règles à `paths:` ne sont pas ré-injectées.** Aucune ne réapparaît à la
  compaction. On peut donc, juste après, écrire du Compose sans que `ui-compose.md`
  soit chargé, ou toucher au domaine sans `domain-purity.md`.
- **Le remède qu'on croyait acquis n'existe pas**, et c'est la correction la plus
  coûteuse de ce fichier. On a soutenu ici, plusieurs séances durant, que rouvrir un
  fichier de la zone ré-émettait un `path_glob_match` même pour une règle déjà chargée,
  la déduplication portant sur la fenêtre et la compaction la remettant à zéro. **C'est
  faux.** La déduplication porte sur la **séance** : une règle scopée se charge une fois,
  et rien ne la recharge ensuite — ni rouvrir le fichier qui l'avait déclenchée, ni en
  ouvrir un autre de sa zone.

Le relevé qui tranche tient en trois lectures, dans une **même** fenêtre, à quelques
secondes d'écart — c'est l'appariement qui fait la preuve, pas la lecture isolée :

| geste | règle visée | déjà chargée cette séance ? | journal |
|---|---|---|---|
| ouvrir un fichier de `.claude/hooks/` | `harnais.md` | oui, 4 fenêtres plus tôt | **rien** |
| ouvrir un fichier de `ui/` | `ui-compose.md` | non | `path_glob_match` |
| lire `.claude/rules/harnais.md` | — | — | rien, mais le contenu est en contexte |

Le deuxième geste est le **témoin**, et il est indispensable : sans lui, un journal muet
ne se distingue pas d'un mécanisme mort. Le mécanisme est vivant — il ne sert qu'une
fois. C'est ce qui donne l'illusion qu'il marche : sur une règle encore jamais chargée,
rouvrir la zone fonctionne parfaitement.

**Le remède réel est de lire le fichier de règle lui-même** (`.claude/rules/<nom>.md`).
Ce n'est pas un chargement d'instruction — rien n'est journalisé — mais le contenu
arrive en contexte comme contenu de fichier, ce qui est le seul effet recherché.

Ce relevé vaut pour ce harnais-ci, sur un conteneur et une séance. Le refaire s'il
change de version : trois lectures suffisent.

## Ce qui s'est réellement chargé

Le hook `InstructionsLoaded` journalise chaque chargement, avec son `file_path`, son
`memory_type` et son `load_reason`. Trois interrogations, à recopier telles quelles :

```bash
# Le hook écrit dans `git rev-parse --git-dir` : on le résout pareil, sinon les deux
# divergent dans un worktree lié, où `.git` est un fichier pointeur et non un répertoire.
journal="$(git rev-parse --git-dir)/badgemoi-instructions.log"
racine="$(git rev-parse --show-toplevel)"

# Ce qui est en contexte **maintenant** : la fenêtre courante, bornée par la dernière
# **suite** de `session_start`. C'est le seul relevé qui réponde à « ai-je cette règle
# sous les yeux ».
cut -f2 "$journal" \
  | jq -rR 'fromjson? | "\(.load_reason // "raison absente")\t\(((.file_path // "chemin absent") | split("/") | last))"' \
  | awk '
      /^session_start\t/ { if (!suite || ($0 in vu)) { n = 0; split("", vu) }
                           suite = 1; vu[$0] = 1; l[n++] = $0; next }
                         { suite = 0; l[n++] = $0 }
      END                { for (i = 0; i < n; i++) print l[i] }' \
  | sort -u

# Le cumul, toutes les séances que ce conteneur a vues
cut -f2 "$journal" | jq -rR 'fromjson? | .load_reason // "inconnue"' | sort | uniq -c

# Les règles qu'aucune séance n'a jamais chargées
comm -13 \
  <(cut -f2 "$journal" | jq -rR 'fromjson? | .file_path // empty' | sed 's|.*/||' | sort -u) \
  <(ls "$racine"/.claude/rules/*.md | sed 's|.*/||' | sort)
```

`jq -rR` avec `fromjson?` est délibéré : le hook garde une ligne brute quand l'entrée
n'est pas du JSON, et sans ce filtre une seule ligne de ce genre ferait échouer tout le
bloc. Elle est ignorée en silence.

**Les `//` ne font pas double emploi avec lui, ils couvrent l'autre moitié du problème.**
`fromjson?` protège du non-JSON ; les `//` protègent du JSON valide auquel il *manque* un
champ attendu — le cas exact d'une dérive de schéma en amont, celui pour lequel le hook
journalise l'événement entier. Sans eux, `split("/")` échoue sur un `file_path` absent et
la ligne disparaît du relevé. Le manque est resté invisible parce que `jq` sort alors avec
un code **0** : rien ne signale la perte, sinon un message sur la sortie d'erreur que
personne ne regarde.

**L'`awk` de la première interrogation n'est pas un raffinement, il répare une réponse
fausse.** Cette interrogation filtrait avant sur le `session_id`, qui ne change pas à la
compaction : elle mélangeait donc toutes les fenêtres d'une même séance et annonçait
comme chargées des règles évincées depuis longtemps — le contraire de ce qu'on lui
demande. Ce `awk` vide son tampon à l'ouverture d'une fenêtre et ne garde que la
dernière. Le `sort -u` remplace le `uniq -c` pour la même raison : à l'intérieur d'une
fenêtre le chargement est dédupliqué, un compte n'y apporte rien et un compte supérieur
à 1 n'y signifierait rien.

**Il a fallu deux conditions pour borner cette fenêtre, et la seconde n'est pas
décorative.** Vider le tampon à chaque `session_start` tronquait le relevé à la dernière
ligne de la suite : `CLAUDE.md` disparaissait alors qu'il est en contexte — la mesure
mentait sur la règle qui compte le plus. Ne le vider qu'au *début* d'une suite (`suite`)
répare ce cas mais en laisse un autre : deux fenêtres consécutives dont la première n'a
chargé aucune règle scopée ont leurs suites qui **se touchent**, et l'`awk` les fusionne.
D'où `vu` : à l'intérieur d'une fenêtre le chargement est dédupliqué, donc un
`session_start` déjà présent au tampon ne peut qu'ouvrir la fenêtre suivante. C'est
`split("", vu)` et non `delete vu`, pour rester dans le `mawk` du conteneur.

**Le journal vit dans `.git/`, donc il meurt avec le conteneur.** En session web le dépôt
est recloné à neuf : le fichier repart vide, et « toutes séances confondues » ne couvre
en réalité que les séances de ce conteneur-là. Un cumul plus bas que celui annoncé par la
passation précédente ne dit donc rien du harnais — il dit qu'on a changé de machine.

Ce n'est pas la seule raison de ne rien conclure d'un cumul : il **additionne des
fenêtres révolues**. La mesure qui approche le budget d'instructions n'est ni le cumul
du conteneur ni celui de la séance, c'est la **première** interrogation — la seule qui
compte des règles réellement présentes.

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

- **Une règle chargée dans une fenêtre révolue n'est plus en contexte, et rien ne l'y
  ramènera.** Ne pas écrire « rouvrir un fichier de la zone avant d'y toucher » dans une
  passation : c'est le conseil faux, corrigé plus haut. Écrire le geste qui marche —
  lire `.claude/rules/<nom>.md`.

Le mécanisme lui-même est acquis : les règles se chargent bien sur `path_glob_match`, et
le glob se déclenche sur le **chemin visé**, pas sur l'existence du fichier. En revanche
un journal muet alors que des fichiers ont été ouverts n'est **pas** une anomalie : c'est
le cas normal dès que les règles concernées ont déjà servi dans la séance. Ne conclure à
un défaut de glob qu'après avoir vu `check-docs-coherence.sh` rouge.

## Ce que le harnais a réellement fait

Le journal d'instructions dit ce qui s'est **chargé**. Celui-ci dit ce qui s'est
**déclenché**, et surtout avec quelle issue. Deux interrogations :

```bash
usage="$(git rev-parse --git-dir)/badgemoi-usage.log"

# Par hook, la répartition des issues. C'est le relevé qui compte.
awk -F'\t' '{print $2"\t"$3}' "$usage" | sort | uniq -c | sort -rn

# Les commandes invoquées dans la séance, les plus fréquentes d'abord
awk -F'\t' '$3 == "commande" {print $4}' "$usage" | sort | uniq -c | sort -rn
```

**Comment lire la répartition**, et c'est tout l'intérêt du journal :

- `muet` — le hook a examiné sa cible et n'a rien trouvé. Son silence est un
  **résultat**, et c'est le cas sain le plus fréquent.
- `hors-perimetre` — le hook a tourné mais n'avait rien à examiner. Son silence est
  **normal** et ne prouve rien sur son bon fonctionnement.
- `alerte` — il a trouvé et l'a dit.

**Le signal d'alarme est un déséquilibre entre les deux premiers.** Une séance qui a
édité du Kotlin et ne montre que des `garde-fous hors-perimetre` veut dire que la coupe
`*.kt` ne mord plus — le défaut exact de 2026, que la batterie laissait passer verte.
Mesuré : coupe cassée, quatre fichiers Kotlin édités donnent quatre `hors-perimetre` au
lieu de quatre `muet`.

Deux limites à ne pas oublier :

- **Un hook absent de la répartition n'a pas tourné du tout.** C'est plus grave qu'une
  mauvaise issue, et ça ne se voit qu'en cherchant ce qui *manque* — le relevé ne peut
  pas signaler une ligne qui n'existe pas.
- **Comme le journal d'instructions, celui-ci meurt avec le conteneur.** Un compte plus
  bas que la passation précédente dit qu'on a changé de machine, pas que le harnais a
  régressé.

## Forme

Écrire la note en markdown dans un fichier, puis l'**envoyer** — et ne pas la répéter
dans la réponse. Une passation vit pour être transmise ; tant qu'elle n'existe que dans
le flux du terminal, elle ne l'est pas : la TUI capture la souris, et la sélectionner
suppose de connaître le contournement (Maj sous Linux, Option sur macOS).

**Le fichier va hors du dépôt**, sans exception : le répertoire de travail de session
quand le prompt système en indique un, sinon un `mktemp -d`. C'est le point à ne pas
relâcher — une passation périmée qui traîne dans l'arbre de travail, ou pire qui part
dans un commit, est pire que pas de passation du tout. C'est le dépôt qu'il faut tenir
propre, pas le disque.

Nommer le fichier `passation-AAAA-MM-JJ-HHMM.md`. Deux passations dans la même séance
se suivent, elles ne s'écrasent pas.

L'envoyer en **rendu inline** plutôt qu'en simple pièce jointe : elle reste ainsi
lisible d'un coup d'œil, sans manipulation.

La réponse, elle, tient en **une ligne** : où est le fichier, et l'état en une phrase.
Recopier la note à côté annulerait le seul bénéfice — une passation fait couramment
quatre-vingts à cent vingt lignes, et `/point` s'appelle précisément quand le contexte
est saturé.

Concis mais complet — ce qui n'est pas écrit ici sera reconstruit à partir du code, et
la reconstruction se trompe.
