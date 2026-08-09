# Lire les journaux du harnais

Savoir de référence pour les relevés que `/point` produit : ce que chaque journal
mesure, pourquoi chaque garde des interrogations est là, et ce qu'on a le droit d'en
conclure.

**Les interrogations elles-mêmes restent dans
[`.claude/commands/point.md`](../.claude/commands/point.md)**, prêtes à être recopiées.
Les sortir de la commande aurait déplacé le coût au lieu de le réduire : `/point`
s'invoque quand le contexte est saturé, et lancer un relevé ne doit pas commencer par
ouvrir un fichier de plus. Ce fichier-ci ne s'ouvre que pour **interpréter** un
relevé — ou quand un résultat surprend.

Ce qui suit est mesuré, pas déduit. Chaque correction ici a coûté une séance ou plus.

---

## Ce qui survit à une compaction

**Relevé au journal sur une compaction réelle**, plus seulement lu dans la
documentation :

- Le `CLAUDE.md` racine est relu depuis le disque et ré-injecté. Le harnais journalise
  ce rechargement sous **deux** raisons, et non une : `session_start` le plus souvent,
  `compact` parfois. Le cumul d'un conteneur donne `98 session_start · 18
  path_glob_match · 2 compact`. Dans quelles conditions il choisit l'une ou l'autre
  n'est pas instruit — ce qui compte est que **les deux bornent une fenêtre**.
- La borne d'une fenêtre de contexte est le **début d'une suite** de lignes de bordure,
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

---

## Journal d'instructions — pourquoi chaque garde est là

Les trois interrogations sont dans `point.md`. Ce qui suit dit ce qu'elles protègent.

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
décorative.** Vider le tampon à chaque ligne de bordure tronquait le relevé à la
dernière ligne de la suite : `CLAUDE.md` disparaissait alors qu'il est en contexte — la
mesure mentait sur la règle qui compte le plus. Ne le vider qu'au *début* d'une suite
(`suite`) répare ce cas mais en laisse un autre : deux fenêtres consécutives dont la
première n'a chargé aucune règle scopée ont leurs suites qui **se touchent**, et l'`awk`
les fusionne. D'où `vu` : à l'intérieur d'une fenêtre le chargement est dédupliqué, donc
une bordure déjà présente au tampon ne peut qu'ouvrir la fenêtre suivante. C'est
`split("", vu)` et non `delete vu`, pour rester dans le `mawk` du conteneur.

**`vu` porte sur la ligne entière, raison comprise — et c'est un choix, pas un
reste.** La question s'est posée en ajoutant `compact` : tant que la seule bordure était
`session_start`, déduire sur la ligne ou sur le seul nom de fichier revenait au même.
Avec deux raisons, non. Les deux variantes ont été mesurées, et l'asymétrie tranche.

| clé | ce qu'elle rate | gravité |
|---|---|---|
| `vu[$0]`, la ligne | deux suites de raisons différentes qui se touchent fusionnent | **bénigne** |
| `vu[$2]`, le fichier | une bordure qui répète un fichier sous deux raisons perd ce qui la précède | **grave** |

La fusion est bénigne parce qu'une fenêtre dont la suite touche la suivante est une
fenêtre qui n'a chargé **aucune** règle scopée : la fusion ne fait que lister deux fois
`CLAUDE.md` et la règle non scopée, que la compaction vient justement de remettre en
contexte. Vérifié, aucune règle évincée ne fuit — c'est la seule chose qui compte.

L'autre variante, elle, rouvre une fenêtre au milieu d'elle-même dès qu'une bordure
répète un fichier, et **perd** les lignes d'avant. C'est le faux négatif que tout cet
`awk` existe pour empêcher : une règle en contexte annoncée absente. Entre une redite
lisible et un silence trompeur, ce dépôt choisit la redite. Un témoin de
`scripts/test-hooks.sh` fige ce choix, sans quoi `vu[$2]` repasserait au vert.

**Cet `awk` est recopié mot pour mot dans `fenetre_de()` de
[`scripts/test-hooks.sh`](../scripts/test-hooks.sh).** Corriger l'un sans l'autre laisse
la batterie verte contre une copie périmée.

---

## Ce qu'on a le droit de conclure d'un relevé de chargement

**Le journal vit dans `.git/`, donc il meurt avec le conteneur.** En session web le dépôt
est recloné à neuf : le fichier repart vide, et « toutes séances confondues » ne couvre
en réalité que les séances de ce conteneur-là. Un cumul plus bas que celui annoncé par la
passation précédente ne dit donc rien du harnais — il dit qu'on a changé de machine.

Ce n'est pas la seule raison de ne rien conclure d'un cumul : il **additionne des
fenêtres révolues**. La mesure qui approche le budget d'instructions n'est ni le cumul
du conteneur ni celui de la séance, c'est la **première** interrogation — la seule qui
compte des règles réellement présentes.

- **Une règle jamais chargée alors que sa zone a été touchée** est un défaut de glob, pas
  une fatalité. `./scripts/check-docs-coherence.sh` détecte déjà le motif qui ne
  correspond à aucun fichier suivi ; le lancer avant de conclure. La condition « alors que
  sa zone a été touchée » porte tout le sens : sur un conteneur neuf, la troisième
  interrogation liste **toutes** les règles parce que le journal est vide, pas parce que
  les globs sont cassés. Ne rien conclure d'une liste pleine sans avoir ouvert un fichier
  de la zone d'abord.
- **Plusieurs rechargements de `CLAUDE.md`** ne sont pas une anomalie : c'est le compte
  des compactions traversées, plus une ou deux au démarrage. Les compter suppose de
  regarder `session_start` **et** `compact` : le harnais émet les deux, et n'en retenir
  qu'une sous-compte les compactions sans que rien ne le signale.
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

---

## Journal d'usage — comment lire la répartition

Le journal d'instructions dit ce qui s'est **chargé**. Celui-ci dit ce qui s'est
**déclenché**, et surtout avec quelle issue. C'est tout l'intérêt du journal :

- `muet` — le hook a examiné sa cible et n'a rien trouvé. Son silence est un
  **résultat**, et c'est le cas sain le plus fréquent.
- `hors-perimetre` — le hook a tourné mais n'avait rien à examiner. Son silence est
  **normal** et ne prouve rien sur son bon fonctionnement.
- `alerte` — il a trouvé et l'a dit.

Une quatrième valeur apparaît dans la première interrogation sans être une issue de
contrôle : `commande`, la mesure d'usage que lit la seconde. Elle ne se compare pas aux
trois autres et ne participe à aucun équilibre — la voir dans la répartition est normal.

**Le signal d'alarme est un déséquilibre entre les deux premiers.** Une séance qui a
édité du Kotlin et ne montre que des `garde-fous hors-perimetre` veut dire que la coupe
`*.kt` ne mord plus — le défaut exact de 2026, que la batterie laissait passer verte.
Mesuré : coupe cassée, quatre fichiers Kotlin édités donnent quatre `hors-perimetre` au
lieu de quatre `muet`.

Trois limites à ne pas oublier :

- **Un hook absent de la répartition n'a pas tourné du tout.** C'est plus grave qu'une
  mauvaise issue, et ça ne se voit qu'en cherchant ce qui *manque* — le relevé ne peut
  pas signaler une ligne qui n'existe pas.
- **Comme le journal d'instructions, celui-ci meurt avec le conteneur.** Un compte plus
  bas que la passation précédente dit qu'on a changé de machine, pas que le harnais a
  régressé.
- **La seconde interrogation compte les *skills*, pas « les commandes ».** Voir
  ci-dessous — c'est une limite du point de mesure, pas un défaut réparable.

### La seconde interrogation ne voit pas les commandes intégrées

Elle s'appuie sur `antiseche.sh`, un hook `UserPromptSubmit` : il ne voit que ce qui
atteint le modèle. Les **commandes intégrées** — `/clear`, `/compact`, `/batch`,
`/resume` — sont interceptées par le client et n'y parviennent jamais. Seules les
commandes du dépôt et les skills passent par là.

Mesuré : `27 /insights · 22 /pousser · 2 /point`, et **zéro** `/compact`, alors que le
journal d'instructions portait deux lignes `compact` le même matin.

**Ce défaut ne se corrige pas** : aucun hook n'est placé pour voir les commandes
intégrées, l'information n'arrive jamais de ce côté. La seule réparation est que le
relevé annonce sa portée — d'où le mot « skills » plutôt que « commandes ». Ne pas
conclure d'un zéro qu'une commande n'a pas servi, ni qu'elle n'existe pas : c'est
l'erreur commise en concluant de l'absence de fichier dans `.claude/commands/` que
`/batch` n'existait pas.

### La batterie n'alimente plus ce journal

`scripts/test-hooks.sh` invoque les vrais hooks et écrivait donc dans le journal réel :
≈ 224 lignes sur 369 lui étaient imputables, et `/insights` s'y trouvait compté 23 fois
pour deux invocations. Les entrées `commande` étant indiscernables d'une invocation
réelle, le défaut avait déjà produit une fausse piste — on avait soupçonné le compteur,
qui était juste.

La batterie pose désormais `BADGEMOI_USAGE_LOG` vers un fichier jetable pour toute sa
durée. Un relevé antérieur au correctif surcompte donc les commandes, dans une
proportion qui dépend du nombre de lancements. Les lignes déjà polluées n'ont pas été
purgées : le journal meurt avec le conteneur, ce qui règle la question tout seul.
