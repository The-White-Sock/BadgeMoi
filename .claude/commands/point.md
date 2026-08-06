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

## Forme

Rendre la note **dans la réponse**, en markdown, prête à être collée dans le prompt
suivant. Ne pas créer de fichier : une passation périmée traînant dans le dépôt est pire
que pas de passation.

Concis mais complet — ce qui n'est pas écrit ici sera reconstruit à partir du code, et
la reconstruction se trompe.
