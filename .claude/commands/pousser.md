---
description: Qualité, commit, push, PR et auto-merge — le rituel complet de livraison
---

Livre le travail en cours en suivant la séquence ci-dessous **dans l'ordre**, sans en
sauter une étape. Chacune de ces étapes a déjà été oubliée au moins une fois ; c'est
la raison d'être de cette commande.

Argument éventuel : `$ARGUMENTS` — les numéros d'issues fermées par ce travail.

## 1. Qualité

Lancer `/qualite` — les quatre tâches Gradle, la cohérence des docs si elles ont bougé,
et la lecture des échecs courants.

Tout doit être vert avant de continuer. La CI reste l'arbitre final, mais un aller-retour
évité est une heure gagnée.

## 1 bis. Les deux relectures, qui ne se remplacent pas

Elles répondent à deux questions différentes, et aucune ne couvre l'autre :

- **`/revue`** — le diff contredit-il une **règle écrite** (ergonomie, cahier) ?
  À lancer si le diff touche l'UI ou s'écarte peut-être du cahier.
- **`/code-review`** — le code est-il **juste** : bugs, régressions, failles ?
  À lancer dès que le diff touche `app/src/main/**`. Sauté pour un diff de
  documentation ou de harnais seul, qui ne paie pas une relecture de code.

Deux précautions sur `/code-review`, tirées d'un échec déjà commis :

- **Fixer la plage sur `origin/main...HEAD`**, jamais `main...HEAD`. Le `main` local peut
  être en retard de plusieurs dizaines de commits, et la relecture couvre alors du travail
  déjà fusionné. C'est arrivé : 16 commits relus pour rien.
- Ses trouvailles sont des **hypothèses**, pas des verdicts. Les classer comme celles de
  `/revue` — à corriger, à assumer, écarté — et vérifier chacune avant d'y toucher.

## 2. État de la branche

La branche de travail est celle imposée par la session. Vérifier l'état de sa
**PR précédente** :

- **Fusionnée** → repartir de zéro, en gardant le même nom :
  `git fetch origin main && git checkout -B <branche> origin/main`.
  Une PR fusionnée est finie : elle ne peut pas porter de nouveau travail.
- **Ouverte** → continuer dessus, sans réinitialiser.
- **Ref distante supprimée** (auto-delete après fusion) → `git remote prune origin`
  avant de pousser, sinon le push est rejeté en « stale info ».

Si la branche portait des commits non fusionnés, les rebaser plutôt que les perdre.

## 3. Commit

Message **gitmoji**, prose **française** :
`<emoji>(<scope>) : <description au présent>`.

Emojis : `✨` fonctionnalité · `🐛` correctif · `♻️` refactor · `✅` tests ·
`📝` docs · `🔧` outillage · `💄` visuel · `🚀` perf · `💥` rupture.

L'emoji détermine le versioning automatique (`docs/publication.md`) : le choisir
pour ce qu'il déclenche, pas pour son allure.

Le corps explique **pourquoi**, pas quoi — le diff dit déjà quoi.

## 4. Push

```bash
git push -u origin <branche>
```

En cas d'échec réseau, réessayer avec un retrait exponentiel (2s, 4s, 8s, 16s).

## 5. Pull request

Corps en **français**, sauf les mots-clés de fermeture.

> **`Closes #N`, `Fixes #N`, `Resolves #N` n'existent qu'en anglais.**
> « Ferme #N » est une simple mention et ne ferme rien. C'est l'erreur qui a laissé
> une série d'issues ouvertes après leur livraison.

Structure : ce qui change, pourquoi, ce que ça coûte, comment c'est vérifié. Si le
travail contredit le cahier, le renvoi vers l'écart §9 va dans le corps.

## 6. Auto-merge — immédiatement

Armer l'auto-merge **en squash** dans la foulée de la création, sans attendre la CI.
C'est une convention du dépôt (`CLAUDE.md`), et l'oublier laisse une PR verte non
fusionnée que personne ne regarde.

Exceptions : PR en brouillon, auto-merge désactivé sur le dépôt, ou conflit ouvert.

## 7. Suivi

Planifier un point de contrôle à environ une heure pour revérifier l'état, la CI et
la fusionnabilité — les webhooks ne livrent ni les succès de CI, ni les conflits de
fusion apparus après coup.
