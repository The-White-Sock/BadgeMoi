---
paths:
  - ".claude/hooks/*"
  - ".claude/rules/*"
  - "scripts/*"
  - ".claude/settings.json"
---

# Hooks et scripts du harnais

`CLAUDE.md` § « Outillage de session » reste la source sur le rôle de chaque hook.
Ce digest ne porte que ce qui casse **en silence** dans cette zone.

## Deux batteries, à relancer selon la zone touchée

Un contrôle muet est indistinguable d'un contrôle qui n'avait rien à dire. C'est
comme ça que l'antisèche est restée morte plusieurs semaines sur un `.user_input`
devenu `.prompt`, et que `garde-fous.sh` a perdu son alerte sur les textes en dur
sans que rien ne le signale.

- **`./scripts/test-hooks.sh`** — les hooks, et les interrogations de `/point`.
  Tout hook doit y avoir des cas.
- **`./scripts/test-docs-coherence.sh`** — `check-docs-coherence.sh`, un cas par
  contrôle. Les cas de budget y figurent **avec leur revers** : les mêmes lignes
  ajoutées en prose puis en commentaire HTML, la même règle scopée puis non scopée.
  C'est la paire qui prouve quelque chose, pas le cas seul.

Les deux tournent en CI sur chaque PR et sur les push vers `main`
(`.github/workflows/harnais.yml`), qui reste l'arbitre. Les lancer localement n'achète
qu'un aller-retour évité.

**Le compte de cas n'est pas une constante.** `test-hooks.sh` : 48 sur un arbre propre
et poussé, 49 sur un arbre sale — le bloc `bilan.sh` branche sur `git status
--porcelain`. Un arbre propre mais **non poussé** fait échouer le cas « propre et à
jour » : c'est le hook qui fonctionne, pas une régression. Annoncer « N cas » sans dire
l'état de l'arbre n'est pas reproductible.

## Une batterie prouve qu'un hook *peut* se déclencher, pas qu'il *se déclenche*

C'est le reliquat que les deux batteries ne couvraient pas, et par lequel les deux
pannes historiques sont passées : elles étaient vertes pendant que les hooks étaient
inutiles en séance réelle.

`journal-usage.sh` est une **bibliothèque sourcée**, pas un hook — aucun événement ne
la lance. Elle est sourcée par `antiseche.sh`, `garde-fous.sh` et `avant-livraison.sh`,
qui journalisent leur issue dans `$(git rev-parse --git-dir)/badgemoi-usage.log` :

- `alerte` — trouvé quelque chose, et l'a dit ;
- `muet` — a examiné sa cible sans rien trouver. Le silence est un **résultat** ;
- `hors-perimetre` — n'avait rien à examiner. Le silence est **normal** ;
- `commande` — une commande slash a été invoquée (mesure d'usage, pas un contrôle).

**Trois issues et non deux, parce que c'est la distinction `muet` / `hors-perimetre`
qui porte tout.** Les deux produisent le même silence sur stdout. Mesuré : la coupe
`*.kt` de `garde-fous.sh` neutralisée, quatre fichiers Kotlin édités donnent quatre
`hors-perimetre` au lieu de quatre `muet` — la signature exacte du défaut resté
invisible. Un compteur qui les confondrait rouvrirait le trou qu'il prétend fermer.

Corollaire pour les cas de test : ils vont **par paires**, comme ceux du budget dans
`test-docs-coherence.sh`. Un cas `muet` seul ne prouve rien sans son `hors-perimetre`.

La bibliothèque ne doit jamais faire échouer le hook qui la source : erreurs avalées,
retour toujours 0, rien sur stdout — un octet de trop y corromprait le JSON rendu au
harnais. Si elle manque, chaque hook redéfinit `journaliser_usage` en fonction vide.

## Une batterie verte au premier lancement n'a encore rien prouvé

Le vert d'un test qu'on vient d'écrire ne distingue pas « le contrôle marche » de
« le test ne mord sur rien ». Le geste qui tranche est de rejouer la batterie contre
un contrôle **cassé exprès**, hors dépôt, et de vérifier que ce sont bien les cas
attendus qui rougissent — les deux batteries ont été éprouvées ainsi, dans les deux
sens (contrôle muet, puis contrôle bavard).

## Une règle scopée ne se charge qu'une fois par séance

La compaction ne remet pas ce compteur à zéro : passé un `/compact`, la règle n'est
plus en contexte et rouvrir un fichier de sa zone **ne la rappelle pas**. Le seul
geste qui marche est de lire `.claude/rules/<nom>.md`. Corollaire pour le diagnostic :
un journal muet après avoir ouvert des fichiers est le cas **normal**, pas le signe
d'un glob cassé — c'est `check-docs-coherence.sh` qui tranche là-dessus. Le relevé qui
l'établit est dans `/point`.

## Une règle créée en cours de séance ne se charge pas dans cette séance

Mesuré : `.claude/rules/` est parcouru au lancement. Ouvrir un fichier de la zone
d'une règle **ajoutée depuis** ne déclenche rien, et le journal reste muet — alors
que le même geste charge bien une règle présente au démarrage. Ne pas en conclure
que le glob est faux : le vérifier à la séance suivante, ou par le contrôle de
motif mort de `check-docs-coherence.sh`, qui lui lit le disque.

Les répertoires visés ici sont **plats** : `*` suffit et `**` serait une supposition
sur une arborescence qui n'existe pas.

## `grep -q` ne termine jamais un tube

Il s'arrête au premier match, SIGPIPE l'amont, et sous `set -o pipefail` le tube vaut
141 : l'alerte est perdue. D'autant plus sûrement que le fichier est gros — donc
exactement quand le hook sert. Un here-string (`grep -q motif <<< "${x}"`) n'est pas
un tube : c'est la forme à reprendre.

## Un hook ne suppose rien de son environnement

Forcer `LC_ALL` et `LANG` dans le hook lui-même plutôt que de les hériter : `iconv
//TRANSLIT` rend « ? » au lieu de « e » quand `LANG` est vide, ce qui est l'état par
défaut du conteneur. Résoudre le dépôt par `git rev-parse`, jamais par `.git/` en
dur : dans un worktree lié, `.git` est un fichier pointeur.

## Des pointeurs, jamais du contenu

Un hook qui déverse du texte à chaque tour consomme le budget de contexte qu'il est
censé préserver. Sortie JSON `hookSpecificOutput.additionalContext`, quelques lignes
au plus, silence complet quand rien ne correspond.

## Consultatif, sauf un

Tous les hooks rendent la main. `avant-livraison.sh` est la seule exception, et son
périmètre est une décision, pas un détail : trois erreurs déjà commises et mal
réparables. Il n'arrête d'ailleurs que ce qu'il a su lire — un message passé par
`-F` ou par heredoc passe sans contrôle.

## Vérifier ce qu'une mesure compte, avant de corriger son seuil

Motif récurrent ici : un pipeline qui affirme plus que ses données ne portent. Le
`.user_input` mort, le cumul du journal confondu avec la fenêtre courante, le
`wc -l` d'un fichier dont un tiers n'est jamais injecté. Le réflexe utile n'est pas
« vérifier le glob », c'est « vérifier ce que la mesure prétend mesurer ».
