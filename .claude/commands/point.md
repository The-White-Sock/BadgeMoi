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

## Relever ce que le harnais a fait

Deux journaux, cinq interrogations à recopier telles quelles.

**Ce que ces relevés veulent dire — et surtout ce qu'on a le droit d'en conclure — est
dans [`docs/journaux-du-harnais.md`](../../docs/journaux-du-harnais.md).** L'ouvrir
avant de tirer la moindre conclusion d'un résultat, et en particulier avant de conclure
à un défaut : ici les silences trompeurs sont la règle, pas l'exception. Un journal muet
après avoir ouvert des fichiers, une liste pleine de règles « jamais chargées », un
cumul en baisse — les trois ont l'air de pannes et n'en sont pas.

### Ce qui s'est réellement chargé

Le hook `InstructionsLoaded` journalise chaque chargement, avec son `file_path`, son
`memory_type` et son `load_reason`.

```bash
# Le hook écrit dans `git rev-parse --git-dir` : on le résout pareil, sinon les deux
# divergent dans un worktree lié, où `.git` est un fichier pointeur et non un répertoire.
journal="$(git rev-parse --git-dir)/badgemoi-instructions.log"
racine="$(git rev-parse --show-toplevel)"

# Ce qui est en contexte **maintenant** : la fenêtre courante, bornée par la dernière
# **suite** de lignes `session_start` ou `compact` — le harnais émet les deux. C'est le
# seul relevé qui réponde à « ai-je cette règle sous les yeux ».
cut -f2 "$journal" \
  | jq -rR 'fromjson? | "\(.load_reason // "raison absente")\t\(((.file_path // "chemin absent") | split("/") | last))"' \
  | awk -F'\t' '
      /^(session_start|compact)\t/ { if (!suite || ($2 in vu)) { n = 0; split("", vu) }
                                     suite = 1; vu[$2] = 1; l[n++] = $0; next }
                                   { suite = 0; l[n++] = $0 }
      END                          { for (i = 0; i < n; i++) print l[i] }' \
  | sort -u

# Le cumul, toutes les séances que ce conteneur a vues
cut -f2 "$journal" | jq -rR 'fromjson? | .load_reason // "inconnue"' | sort | uniq -c

# Les règles qu'aucune séance n'a jamais chargées
comm -13 \
  <(cut -f2 "$journal" | jq -rR 'fromjson? | .file_path // empty' | sed 's|.*/||' | sort -u) \
  <(ls "$racine"/.claude/rules/*.md | sed 's|.*/||' | sort)
```

### Ce que le harnais a réellement fait

Le journal d'instructions dit ce qui s'est **chargé**. Celui-ci dit ce qui s'est
**déclenché**, et avec quelle issue : `muet` (examiné, rien trouvé), `hors-perimetre`
(rien à examiner) ou `alerte`.

```bash
usage="$(git rev-parse --git-dir)/badgemoi-usage.log"

# Par hook, la répartition des issues. C'est le relevé qui compte.
awk -F'\t' '{print $2"\t"$3}' "$usage" | sort | uniq -c | sort -rn

# Les **skills** invoquées dans la séance, les plus fréquentes d'abord. Ce relevé ne
# voit pas les commandes intégrées (`/clear`, `/compact`, `/batch`…) : elles sont
# interceptées par le client. Portée réelle dans `docs/journaux-du-harnais.md`.
awk -F'\t' '$3 == "commande" {print $4}' "$usage" | sort | uniq -c | sort -rn
```

Les trois issues ne sont pas interchangeables, et c'est leur **équilibre** qui alarme,
pas leur présence. La lecture est dans le fichier de référence.

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

**Ne pas couper la prose d'un fichier destiné à `SendUserFile` : un paragraphe, une
ligne.** La règle vaut pour la **destination**, pas pour cette commande — tout fichier
qui part en rendu plutôt qu'en diff relève d'elle.

La raison, sans laquelle la consigne sera défaite au premier mimétisme avec `docs/` :
le lecteur qui affiche ce rendu **honore les retours à la ligne simples**. Chaque
coupure de la source devient une coupure visible, et sur un écran étroit elle s'ajoute
à celle du lecteur — le texte se brise alors en plein milieu des phrases. Constaté à
l'écran sur la passation du 9 août, capture à l'appui.

La convention des 88 colonnes n'est pas en cause, sa **portée** l'est : couper sert à
relire un diff et à tenir une revue de PR lisible. Une passation va hors du dépôt sans
exception — jamais diffée, jamais relue en PR, jamais commitée. Elle payait donc tout
le coût d'une contrainte dont elle ne tirait aucun bénéfice. Les fichiers versionnés,
eux, restent coupés à 88 : les deux règles portent sur des destinations différentes et
ne se contredisent pas.

Tableaux, blocs de code et listes ne sont pas concernés : leur mise en forme est portée
par leur syntaxe, que le lecteur respecte déjà.

La réponse, elle, tient en **une ligne** : où est le fichier, et l'état en une phrase.
Recopier la note à côté annulerait le seul bénéfice — une passation fait couramment
quatre-vingts à cent vingt lignes, et `/point` s'appelle précisément quand le contexte
est saturé.

Concis mais complet — ce qui n'est pas écrit ici sera reconstruit à partir du code, et
la reconstruction se trompe.
