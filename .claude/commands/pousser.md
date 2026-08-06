---
description: Qualité, commit, push, PR et auto-merge — le rituel complet de livraison
---

Livre le travail en cours en suivant la séquence ci-dessous **dans l'ordre**, sans en
sauter une étape. Chacune de ces étapes a déjà été oubliée au moins une fois ; c'est
la raison d'être de cette commande.

Argument éventuel : `$ARGUMENTS` — les numéros d'issues fermées par ce travail.

## 1. Qualité

```bash
./gradlew ktlintCheck detekt testDebugUnitTest assembleDebug
```

Tout doit être vert avant de continuer. En cas d'échec de style seul, `ktlintFormat`
puis relancer. La CI reste l'arbitre final, mais un aller-retour évité est une heure
gagnée.

Si des fichiers de documentation ont changé, lancer aussi :

```bash
./scripts/check-docs-coherence.sh
```

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
