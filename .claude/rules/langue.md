# Langue du dépôt

Cette règle n'a **pas** de `paths:`. C'est délibéré : elle se charge au lancement de
chaque session et survit à la compaction, comme `CLAUDE.md`. Elle porte sur ce qu'on
écrit à la toute fin d'une séance — un message de commit, un corps de PR — au moment
où plus aucun fichier de `docs/` n'a de raison d'être ouvert.

Toute la prose du dépôt est en **français** : commentaires, KDoc, messages de commit,
corps de PR, documentation. Les identifiants restent anglais/techniques.

Une exception, et une seule : les mots-clés que GitHub interprète. `Closes #N`,
`Fixes #N`, `Resolves #N` n'existent qu'en anglais — « Ferme #N » est une simple
mention et ne ferme aucune issue. `avant-livraison.sh` refuse la forme française,
parce qu'elle échoue en silence : la PR fusionne, l'issue reste ouverte.
