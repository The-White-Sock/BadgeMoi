# Ergonomie — usage au pouce

BadgeMoi se manipule **d'une main, en roulant**. Ce document fait autorité sur le
placement des éléments interactifs. Le cahier des charges décrit *ce que* les écrans
affichent (§3), ce document dit *où* les choses tactiles ont le droit de se trouver.

Toute règle de placement se décide ici, pas dans le cahier ni dans `CLAUDE.md`.

---

## 1. Le principe

L'ergonomie à une main se pilote par la **zone d'atteinte du pouce**, pas par la
hiérarchie visuelle. Deux données structurantes :

- environ **49 %** des usages se font à une main, **36 %** en berceau (une main tient, un
  pouce agit) : le pouce assure à peu près **trois interactions sur quatre** (Hoober,
  2013) ;
- la zone d'atteinte n'est pas un rectangle mais un **arc**, dont le pivot est la base du
  pouce, en bas de l'écran, du côté de la main qui tient l'appareil. Rayon fonctionnel
  moyen : **60 à 75 mm**.

Conséquence directe : sur un écran de 6,5 pouces (~150 mm de haut), **plus de la moitié
supérieure est hors zone confortable**.

## 2. Cartographie verticale

| Zone | Hauteur depuis le bas | Statut | Contenu adapté |
|---|---|---|---|
| Système | 0–48 dp | **Interdite** | Barre de gestes — aucune cible tactile |
| Naturelle | 48 dp → 30 % | Optimale | Action primaire, actions répétées |
| Extension | 30 → 55 % | Acceptable | Actions secondaires, listes interactives |
| Difficile | 55 → 80 % | À éviter | Contenu défilant non tactile |
| Hors d'atteinte | 80 → 100 % | Proscrite | Titre, statut, indicateurs non tactiles |

L'axe horizontal compte autant : le coin haut-gauche est le pire point pour un droitier,
et l'inverse pour un gaucher. La main de tenue varie chez un même utilisateur, donc
**le bas-centre est la seule position robuste** pour les actions critiques. Un bouton
pleine largeur ancré en bas neutralise la question de la latéralité — c'est le patron des
barres d'action de l'application.

## 3. Règles de placement

- **Action primaire** : bouton pleine largeur ancré en bas.
- **Confirmation ou saisie** : en bas, y compris dans les modales — donc des *bottom
  sheets* plutôt que des fenêtres centrées.
- **Actions destructives** : hors de la zone naturelle, volontairement. On accepte
  qu'elles soient moins commodes pour qu'elles soient moins souvent déclenchées par
  erreur, à condition qu'elles restent découvrables.
- **Navigation** : au bas de l'écran **si elle est fréquente**. Voir la décision 2 §5 :
  ce n'est pas le cas ici.
- **Gestes** : le retour par balayage depuis le bord rachète une partie de l'inatteignable,
  mais ne remplace jamais un contrôle visible.
- **Mode une main du système** (Reachability, mode une main Android) : ne jamais compter
  dessus, son taux d'usage est marginal.

## 4. Contraintes chiffrées

| Contrainte | Valeur | Où elle s'applique |
|---|---|---|
| Cible tactile minimale | **48 × 48 dp** | Toute ligne, tout bouton, tout onglet |
| Espacement entre cibles | **≥ 8 dp** | Entre deux lignes cliquables distinctes |
| Zone d'exclusion des gestes | ~20 dp sur les bords latéraux | Marge des barres d'action |
| Encarts système | `WindowInsets.safeDrawing` | Appliqué une fois, dans `BadgeMoiApp` |
| Clavier ouvert | ampute 35 à 45 % de la hauteur | Sans objet : aucune saisie au clavier |

La dernière ligne mérite une note. Aucun écran de BadgeMoi n'ouvre de clavier : la
correction d'un jalon passe par le cadran d'un `TimePicker`, pas par une saisie clavier. Si un
champ texte apparaissait un jour — un champ de recherche dans l'historique, par exemple —
sa validation devrait remonter au-dessus du clavier.

## 5. Décisions propres à BadgeMoi

**1. Portrait exclusif.** Le paysage repousserait la barre d'action hors de portée et
n'apporte rien : aucun écran n'a de contenu large. Verrouillé au manifeste.

Le verrou tient sur téléphone. Au-delà de **600 dp** de largeur minimale, Android l'ignore
depuis l'API 36 pour les applications qui la ciblent, et l'échappatoire par propriété de
manifeste **a disparu à l'API 37** — celle que nous visons. Une tablette ou un pliable
ouvert affichera donc l'application en paysage. Ce n'est pas la cible, mais aucun écran ne
doit s'y casser : c'est à vérifier au lot 7 avec les tests sur petit écran.

**2. Les onglets restent en haut**, malgré la règle qui veut la navigation en bas.

Ce n'est pas une entorse mais une application de la règle telle qu'elle est écrite : la
zone difficile est disqualifiée pour ce qui est **à la fois fréquent et tactile**. Or on
bascule vers l'Historique au repos, quelques fois par semaine, tandis qu'on valide un
jalon plusieurs fois par trajet, en roulant. Descendre les onglets occuperait la meilleure
place de l'écran avec ce qu'on utilise le moins, et les mettrait en concurrence directe
avec « Valider » et « Passer », qui, eux, doivent y être.

La contrainte de **48 dp** s'applique en revanche pleinement aux onglets : ils sont rares,
pas facultatifs.

**3. La zone haute reste une zone de statut**, conformément au §1.6 du cahier — à une
exception près, assumée : « Abandonner » y figure sur le récapitulatif, précisément parce
que la règle des actions destructives veut qu'il soit malcommode.

## 6. Anti-patrons à ne pas réintroduire

1. Tiroir hamburger en haut à gauche comme navigation principale.
2. « Enregistrer » en haut à droite comme unique point de validation.
3. Fenêtre centrée dont les boutons tombent dans la moitié haute.
4. Contrôles collés au bord bas sans respect des encarts système.
5. Une action destructive côte à côte avec l'action primaire, à quelques millimètres.

## 7. Méthode de validation

Tester sur le **plus grand appareil de la cible**, à une main, main dominante **et** non
dominante, en marchant.

Le critère est mécanique : tout élément **à la fois fréquent et situé au-dessus de 55 %**
de la hauteur est un défaut de conception, pas un choix esthétique.

## 8. Hors périmètre

L'adaptation aux **tablettes et pliables** — bascule vers un rail latéral au-delà de
600 dp, disposition en volets, ancrage des contrôles aux deux coins bas en paysage — est
un arbitrage structurellement différent, et la cible de BadgeMoi est le téléphone. Le
sujet ne se rouvrira que si l'application y est un jour distribuée.
