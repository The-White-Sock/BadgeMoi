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

## Après toute modification d'un hook, relancer `./scripts/test-hooks.sh`

Un hook muet est indistinguable d'un hook qui n'avait rien à dire. C'est comme ça
que l'antisèche est restée morte plusieurs semaines sur un `.user_input` devenu
`.prompt`, et que `garde-fous.sh` a perdu son alerte sur les textes en dur sans que
rien ne le signale. Tout hook doit avoir des cas dans cette batterie.

**Le compte de cas n'est pas une constante.** 47 sur un arbre propre et poussé,
48 sur un arbre sale : le bloc `bilan.sh` branche sur `git status --porcelain`. Un
arbre propre mais **non poussé** fait échouer le cas « propre et à jour » — c'est le
hook qui fonctionne, pas une régression. Annoncer « N cas » sans dire l'état de
l'arbre n'est pas reproductible.

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
