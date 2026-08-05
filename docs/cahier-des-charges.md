# Cahier des charges — Portage Android de l'application "Trajet"

## 0. Contexte

L'application "Trajet" existe aujourd'hui sous forme d'un fichier HTML/CSS/JS autonome (une seule page, aucun backend, aucune dépendance réseau à l'exécution hormis le chargement initial des polices). Elle sert à chronométrer un trajet domicile-travail en Onewheel, jalon par jalon, à archiver les trajets, et à en tirer des moyennes.

Le POC de référence est conservé dans ce dépôt : [`docs/poc/trajet.html`](poc/trajet.html).

Ce document définit le périmètre et les choix nécessaires pour transformer cet existant en application Android installable (F-Droid en premier, Play Store à terme — voir §4.10), en conservant les décisions d'ergonomie déjà validées (usage au pouce en roulant, thème jour/nuit, iconographie monochrome) plutôt que de repartir d'une page blanche.

---

## 1. Périmètre fonctionnel existant (à conserver à l'identique)

### 1.1 Modèle de trajet
- Deux sens : **Aller** (domicile → bureau) et **Retour** (bureau → domicile), chacun composé de **5 jalons** séquentiels :

  | # | Aller | Retour |
  |---|---|---|
  | 1 | Domicile | Bureau |
  | 2 | Gare | Gare |
  | 3 | Départ (train) | Départ (train) |
  | 4 | Gare | Gare |
  | 5 | Bureau | Domicile |

- 4 tronçons nommés par sens : **Ride** (domicile↔gare, gare↔bureau), **Attente** (quai), **Train**.
- Chaque jalon peut être : posé (horodaté), ignoré (bouton maintenu), ou en attente.
- Un trajet est identifié par sa direction, son horodatage de départ, la liste des horodatages par jalon, et la liste des jalons ignorés.

### 1.2 Cycle de vie d'un trajet
1. **Accueil** : démarrage (Aller/Retour) ou reprise d'un trajet en cours.
2. **Trajet actif** : validation séquentielle des jalons, correction possible à tout moment (tap sur une ligne → heure modifiable), chronomètres en direct (temps depuis le dernier jalon, temps écoulé depuis le départ).
3. **Récapitulatif** : une fois le dernier jalon posé ou ignoré, écran de relecture avant archivage (Enregistrer / Annuler).
4. **Historique** : moyennes par tronçon et par sens, durée totale moyenne, liste des trajets récents avec code couleur (plus rapide / plus lent que la moyenne), export CSV, purge de l'historique.

### 1.3 Persistance (actuelle : `localStorage`)
| Donnée | Clé actuelle | Nature |
|---|---|---|
| Trajet en cours | `trajet-active-v2` | objet JSON unique |
| Archive des trajets | `trajet-archive-v2` | tableau JSON |
| Préférence de thème | `trajet-theme-v1` | chaîne `day`/`night` |

### 1.4 Comportements matériels
- **Écran maintenu allumé** pendant un trajet actif (Wake Lock API web → équivalent natif à spécifier §4.4).
- **Vibration** courte de confirmation à la validation d'un jalon, vibration en trois temps à la confirmation du "Passer".
- **Bouton "Passer" à appui maintenu** (650 ms, jauge de remplissage visuelle) plutôt qu'un tap, pour éviter les activations accidentelles dues aux vibrations du board.

### 1.5 Thème et iconographie
- Thème sombre par défaut, thème clair activable manuellement (persisté), tous deux définis par un jeu de tokens de couleur (fond, encre, accent ambre, accent teal, etc.).
- **24 icônes vectorielles monochromes** dessinées à la main (trait, `currentColor`, aucune image bitmap, aucun emoji couleur) : maison, gare, train, immeuble, porte, boussole, graphique, drapeau, horloge, liste, lien, tendance, téléchargement, corbeille, crayon, coche, croix, passer, lecture, synchro, avertissement, archive, répétition, marche.
- Polices : une monospace (valeurs chiffrées/heures) et une sans-serif (texte d'interface), actuellement chargées via Google Fonts.

### 1.6 Structure d'écran (à reproduire, pas à réinventer)
Chaque écran suit le même patron, déjà validé pour l'usage au pouce :
- **Zone fixe haute** : informations de statut (jamais de liste).
- **Zone scrollable unique** : uniquement les listes de données (jalons, tronçons, moyennes, trajets récents).
- **Zone fixe basse** : boutons d'action, toujours visibles, jamais noyés dans un scroll.
- Écran d'accueil : boutons ancrés en bas de l'écran (zone naturellement atteignable au pouce), pas en haut sous l'en-tête.

Le placement des éléments **interactifs** — zones d'atteinte du pouce, tailles de cibles,
position des modales et des actions destructives — est traité à part, dans
[`docs/ergonomie.md`](ergonomie.md), qui fait autorité sur le sujet. L'application est
verrouillée en **portrait**.

### 1.7 Hors périmètre actuel (à ne pas ajouter sans validation)
- Pas de compte utilisateur, pas de synchronisation cloud, pas de backend.
- Pas de notifications programmées.
- Pas de partage social.

---

## 2. Choix d'architecture technique

### 2.1 Options évaluées

| Option | Description | Avantages | Inconvénients |
|---|---|---|---|
| **A — Wrapper WebView** | Réutiliser le fichier HTML/JS quasi tel quel dans une `WebView` Android (ou via Capacitor/Cordova) | Réutilisation directe du travail déjà fait ; mise en production la plus rapide | Pas une appli "native" au sens propre ; APIs Wake Lock/Vibration/fichiers à ponter manuellement via bridge JS↔Kotlin ; rendu et gestes moins fluides qu'en natif ; poids d'appli plus élevé |
| **B — Native Kotlin + Jetpack Compose** | Réécriture de l'interface en Compose, logique métier en Kotlin, en reprenant à l'identique le modèle de données et les tokens visuels | Vraie appli Android : gestes, performances, accès direct aux APIs (Vibrator, PowerManager, SAF pour l'export), meilleure autonomie batterie, meilleure intégration Material (thème système, widgets) | Réécriture complète de l'UI (mais le travail de design/ergonomie déjà fait sert de spécification très précise, donc peu d'incertitude produit) |
| **C — Flutter / React Native** | Framework cross-platform | Portable vers iOS plus tard | Overhead d'apprentissage d'un framework supplémentaire sans bénéfice ici puisque seul Android est visé ; ne s'appuie sur aucun choix déjà fait dans le projet |

### 2.2 Recommandation

**Option B (Kotlin + Jetpack Compose)** — retenue, pour trois raisons concrètes à ce projet précis :
1. Les APIs matérielles utilisées (écran maintenu allumé pendant la conduite, vibration, sauvegarde de fichier CSV) sont plus fiables et plus simples en natif qu'à travers un pont WebView.
2. Le design est déjà entièrement spécifié (tokens de couleur, 24 icônes vectorielles, structure fixe/scroll/fixe, tailles de zones tactiles) — il n'y a pas de travail de conception UI à refaire, seulement une traduction technique.
3. Application mono-plateforme, mono-utilisateur, sans besoin de portabilité iOS : le bénéfice cross-platform de Flutter/RN ne s'applique pas.
4. Un widget d'écran d'accueil est requis dès le premier lot (§9) : la voie native Compose + Glance est la plus directe pour partager l'état entre l'appli et le widget.

---

## 3. Spécifications fonctionnelles détaillées

### 3.1 Écran d'accueil
- Cas "pas de trajet en cours" : deux boutons pleine largeur (Aller / Retour), chacun affichant la frise d'icônes du parcours et un aperçu, ancrés en bas de l'écran.
- Cas "trajet en cours" : **l'accueil s'efface**. L'application ouvre directement l'écran
  des jalons, et la reprise se décide dans une fenêtre posée par-dessus (§3.2). L'écran de
  reprise d'origine — bannière de statut, "Reprendre", "Abandonner" — n'affichait qu'une
  ligne d'information pour un écran entier ; l'écart est consigné au §9 (entrée 10).
- Bascule de thème et onglets de navigation (Trajet / Historique) dans un bandeau haut compact, une seule ligne.

### 3.2 Écran "Trajet actif"
- Frise de progression façon plan de ligne (jalons reliés par un trait, jalon courant mis en évidence, jalons posés distingués visuellement).
- Bandeau Départ / Écoulé : heure de départ figée, durée écoulée en direct tant que le dernier jalon n'est pas posé.
- Mini-indicateur "temps depuis le dernier jalon", discret, mis à jour chaque seconde.
- Liste des jalons, **ancrée en bas** de sa zone, au contact de la barre d'action : c'est là
  qu'est le pouce. Chaque ligne est une rangée de tableau — libellé, puis deux colonnes de
  largeur fixe, **heure de passage** et **temps écoulé depuis le jalon précédent**.
  Correction possible par tap sur une ligne tranchée.
- La liste **ne montre pas les cinq jalons** : les tranchés en petit, le courant en emphase,
  et un seul jalon à venir, grisé. La frise porte la vue d'ensemble. Deux écarts au
  périmètre d'origine sont consignés au §9 : l'heure absolue (entrée 9) et l'affichage
  partiel (entrée 11).
- Barre d'action fixe en bas : bouton "Passer" (appui maintenu 650 ms) à gauche, bouton "Valider" (tap simple, retour haptique, flash de confirmation, verrou anti-double-tap de 400 ms) à droite. Cet ordre a été retenu au test sur appareil.
- **Fenêtre de reprise** quand on retrouve un trajet déjà entamé — jamais quand on vient de
  le démarrer. Elle rappelle la direction, l'heure de départ, le temps écoulé qui avance et
  le prochain jalon, et propose "Reprendre" ou "Abandonner". L'écarter revient à reprendre ;
  l'abandon demande confirmation. C'est ici que vit l'abandon d'un trajet en cours, l'accueil
  ne le proposant plus.

### 3.3 Écran "Récapitulatif"
- Bandeau Départ/Arrivée. La cellule de droite bascule sur la **durée mesurée** quand le
  dernier jalon a été ignoré et qu'il n'y a donc pas d'heure d'arrivée (`departArrivalFlap`
  du POC).
  La cellule Départ est **cliquable** : elle ouvre la correction du jalon de départ, seul
  jalon qu'aucun tronçon ne désigne.
- Liste des tronçons nommés (Ride/Attente/Train/Ride) avec durée. **Seule liste de l'écran** :
  un tronçon est une durée, et c'est une durée qu'on vient relire. La liste des jalons reste
  à l'écran actif, où elle est une liste d'actions (§9, entrée 9).
- Les lignes de tronçon sont **cliquables** : chacune ouvre la correction de son jalon
  d'**arrivée**, celui qui ferme le tronçon et explique sa durée. Les quatre tronçons
  couvrent les jalons 1 à 4, le bandeau le jalon 0 : les cinq restent corrigeables sur
  place, sans quitter l'écran.
- Boutons **"Abandonner"** / **"Enregistrer"** fixes en bas. « Abandonner » jette le trajet
  sans l'archiver — c'est le `sumDiscard` → `discardTrip` du POC, et non un retour en
  arrière. L'action étant irréversible sur un écran atteint automatiquement, elle demande
  confirmation, comme l'abandon depuis l'accueil.

### 3.4 Écran "Historique"
- Sélecteur Aller/Retour.
- Bloc "Trajet complet" : durée moyenne + nombre de trajets archivés.
- Moyennes par tronçon.
- Liste des trajets récents (10 derniers) : date et **heure de départ**, durée colorée selon
  l'écart à la moyenne (plus rapide/plus lent). L'heure distingue deux trajets d'un même
  jour, que la seule date confondait.
- **Ouverture d'un trajet archivé** : hors mode sélection, toucher un trajet récent rouvre
  le récapitulatif (§3.3) sur ce trajet. Les corrections y sont écrites aussitôt dans
  l'archive — même principe que pour un trajet en cours — et les moyennes suivent. L'action
  destructive s'y nomme « Supprimer », le bouton bas « Terminé » : il n'y a rien à
  enregistrer, tout l'est déjà.
- Export CSV : **toute** l'archive, les deux sens confondus, contrairement aux statistiques.
- **Suppression par sélection.** Le bouton « Supprimer » siège au niveau du titre « Trajets
  récents », qui est ce sur quoi il agit. Un appui bascule la liste en mode sélection :
  chaque ligne se coche, le bouton se dédouble en « Annuler » et « Supprimer », et un
  « Tout » coche l'ensemble des trajets du sens — y compris ceux au-delà des dix affichés.
  C'est le seul chemin de suppression, du trajet isolé à la purge complète d'un sens.
- Sortir du mode ou changer de sens vide la sélection : une sélection oubliée ne doit pas
  détruire au prochain appui des trajets qu'on ne regarde plus.

### 3.5 Correction d'un jalon
- Overlay de saisie d'heure (sélecteur d'heure natif Android, équivalent du `<input type="time">` actuel).
- Actions : Enregistrer, Ignorer ce jalon, Effacer, Annuler.

### 3.6 Widget d'écran d'accueil
- Widget minimal (Glance) permettant de lancer directement un trajet Aller ou Retour sans ouvrir l'application.
- Si un trajet est déjà en cours, le widget affiche l'état "en cours" (direction, heure de départ) et propose de rouvrir l'application sur l'écran "Trajet actif" plutôt que de permettre d'en démarrer un nouveau.
- L'état affiché doit rester cohérent avec l'application (même source de vérité, cf. §4.2) — pas de duplication d'état entre widget et appli.

---

## 4. Spécifications techniques

### 4.1 Stack recommandée
- **Langage** : Kotlin.
- **UI** : Jetpack Compose + Material 3 (thème personnalisé, pas le thème Material par défaut — les tokens de couleur existants doivent être repris tels quels).
- **Widget** : Glance (Jetpack Compose pour App Widgets).
- **Architecture** : MVVM simple (ViewModel + StateFlow), pas besoin de couche réseau.
- **Min SDK** : **API 29 (Android 10)**.

### 4.2 Persistance des données
Remplacement de `localStorage` par :
- **Room (SQLite)** pour l'archive des trajets (une table `trips`, une table `milestones` ou un simple champ JSON par trajet — à trancher en phase de conception technique selon le besoin de requêtage).
- **DataStore (Preferences)** pour le trajet en cours (objet unique) et la préférence de thème.
- Le trajet en cours doit être lisible aussi bien par l'application que par le widget (§3.6) : DataStore convient nativement à ce partage inter-process.
- Pas de migration de l'historique web existant : l'application démarre avec un historique vierge (voir §6).

### 4.3 Vibration
- `Vibrator`/`VibratorManager` (API 31+) avec repli sur `Vibrator` classique.
- Permission `android.permission.VIBRATE`.
- Reprendre les deux patterns existants : impulsion courte (validation), triple impulsion (confirmation de "Passer").

### 4.4 Écran maintenu allumé
- Équivalent natif du Wake Lock web : `window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)` sur l'écran actif uniquement, retiré à l'arrivée ou à l'abandon du trajet (pas de permission `WAKE_LOCK` nécessaire avec cette approche, contrairement à un `PowerManager.WakeLock` explicite).

### 4.5 Export CSV
- Remplacement du téléchargement navigateur par le **Storage Access Framework** (`Intent.ACTION_CREATE_DOCUMENT`) ou une **feuille de partage** (`Intent.ACTION_SEND`, type `text/csv`) permettant d'envoyer le fichier vers Drive, e-mail, etc.
- Le fichier est écrit en **ASCII pur** (voir §9, écart 13) : il se lit à l'identique quel que soit l'encodage supposé par le lecteur.

### 4.6 Thème jour/nuit
- Conserver la bascule manuelle existante (persistée), **et** proposer un mode "suivre le thème système" en option — cohérent avec les conventions Android, absent de la version web faute d'API fiable côté navigateur mobile.

### 4.7 Icônes et polices
- Les 24 icônes (tracés SVG simples, trait, sans remplissage complexe) se convertissent directement en **Vector Drawables XML**, `tint` piloté par les tokens de couleur du thème — aucune perte de fidélité attendue.
- Les polices doivent être **embarquées dans l'APK** (fichiers `.ttf`/`.otf` en ressources) plutôt que chargées à distance : supprime toute dépendance réseau, cohérent avec une appli 100 % hors-ligne, et évite de demander la permission `INTERNET`.

### 4.8 Permissions Android nécessaires
| Permission | Usage | Obligatoire |
|---|---|---|
| `VIBRATE` | Retours haptiques | Oui |
| `INTERNET` | Aucun usage prévu si polices embarquées | Non — à ne pas déclarer |

### 4.9 Contraintes non fonctionnelles
- Application **100 % hors-ligne**, aucun appel réseau à l'exécution.
- Cible principale : usage à une main, pouce, écran allumé en extérieur (voir tailles de cibles tactiles et contrastes déjà spécifiés côté web, §1.5-1.6, à reprendre sans les réduire).
- Pas de dégradation de la structure fixe/scroll/fixe même sur petit écran (voir filet de sécurité déjà spécifié côté web : en cas de contenu trop grand, scroll de secours plutôt que perte d'accès à un bouton).

### 4.10 Distribution
Deux canaux visés, **F-Droid en premier** :

1. **F-Droid** : le projet coche déjà les critères d'inclusion (licence libre GPLv3,
   100 % hors-ligne, aucune dépendance propriétaire type Google Play Services/Firebase,
   aucun tracking). Publication via une recette de métadonnées soumise au dépôt
   `fdroiddata`, build reproductible fait par l'infrastructure F-Droid (pas besoin de
   gérer nous-mêmes une clé de signature de release pour ce canal). Voir
   [`docs/publication.md`](publication.md) pour la mécanique détaillée (versioning,
   tags, releases GitHub qui serviront de source pour la recette F-Droid).
2. **Play Store**, à terme, y compris pour une diffusion privée/interne le cas échéant
   (piste "publication interne" ou liste de diffusion restreinte du Play Console).
   Implique : compte développeur Google Play, signature via **App Bundle (.aab)** signé
   par Play App Signing, fiche store minimale, et conformité aux politiques de
   contenu/permissions Play (déclarer explicitement l'absence de collecte de données).

En attendant, les APK de test sont distribués via les **GitHub Releases** du dépôt
(voir [`docs/publication.md`](publication.md)).

---

## 5. Design system à reprendre tel quel

| Token | Nuit | Jour |
|---|---|---|
| Fond | quasi noir navy | blanc |
| Accent principal (ambre) | vif | assombri pour contraste sur blanc |
| Accent secondaire (teal) | vif | assombri pour contraste sur blanc |
| Texte | blanc cassé | quasi noir |

Règle de contraste à respecter dans la traduction Compose : tout texte/icône sur fond coloré doit utiliser une couleur de contenu dédiée (équivalent de l'actuel `--cta-ink`), jamais une couleur codée en dur — c'est la classe de bug rencontrée plusieurs fois côté web et qu'il faut éviter dès la conception des composants Compose (utiliser `MaterialTheme.colorScheme` de bout en bout, aucune couleur littérale dans les composables d'écran).

---

## 6. Migration des données existantes

**Décision : pas de migration.** L'application Android démarre avec un historique vierge ; aucun export/import depuis le `localStorage` de la version web n'est développé.

---

## 7. Découpage en lots

| Lot | Contenu | Sortie |
|---|---|---|
| 1 | Modèle de données Kotlin (trajets, jalons, tronçons), persistance Room/DataStore | Logique testable sans UI |
| 2 | Écran d'accueil + navigation + thème | APK avec démarrage/reprise de trajet |
| 3 | Écran "Trajet actif" (validation, correction, haptique, écran allumé) | Parcours de saisie complet |
| 4 | Écran Récapitulatif + archivage | Cycle complet trajet → historique |
| 5 | Écran Historique (moyennes, export CSV, purge) | Fonctionnalité complète |
| 6 | Widget d'écran d'accueil (Glance) : lancement rapide + affichage de l'état en cours | Widget installable, cohérent avec l'état de l'appli |
| 7 | Polish : icônes vectorielles finales, thème système, tests sur petit écran, préparation recette F-Droid puis fiche Play Store | Prêt pour publication |

---

## 8. Critères d'acceptation

- Un trajet complet (Aller ou Retour) peut être saisi entièrement au pouce, une main, sans jamais faire défiler la page pour atteindre un bouton d'action.
- L'écran reste allumé pendant toute la durée d'un trajet actif et s'éteint normalement une fois le trajet terminé ou abandonné.
- Les moyennes de l'historique restent correctes après purge partielle ou export.
- Le thème jour offre un contraste suffisant sur chaque écran, y compris dans les zones à fond coloré (boutons pleins, badges d'état).
- Aucune requête réseau n'est émise par l'application (vérifiable via un pare-feu/proxy de test).
- Le widget d'écran d'accueil permet de démarrer un trajet sans ouvrir l'application, et reflète correctement un trajet déjà en cours.

---

## 9. Points ouverts — décisions

| # | Point | Décision |
|---|---|---|
| 1 | Version minimale d'Android | **API 29 (Android 10)** |
| 2 | Distribution | **F-Droid en premier**, **Play Store** à terme (voir §4.10 et `docs/publication.md`) |
| 3 | Récupération de l'historique web existant | **Non** — l'application démarre avec un historique vierge |
| 4 | Widget d'écran d'accueil | **Oui**, intégré dès la conception (lot 6, §3.6) — pas une évolution différée |
| 5 | Grain de l'ondulation d'appui en thème nuit | **Accepté** — on garde l'ondulation Material telle quelle, sans code de remplacement (voir ci-dessous) |
| 6 | Correction d'un jalon | **Jalons tranchés seulement** (posés ou ignorés) — restreint le « à tout moment » du §1.2 |
| 7 | Chrono « depuis le dernier jalon » | **Sur la ligne du jalon courant**, avec emphase — et non le mini-indicateur discret du §3.2 |
| 8 | Second bouton du récapitulatif | **« Abandonner »** — comportement du POC rétabli, voir #87 |
| 9 | Jalons et tronçons | **Deux vues, deux rôles** — jalons à l'écran actif (avec leur heure), tronçons seuls au récapitulatif, voir #90 |
| 10 | Reprise d'un trajet | **Fenêtre sur l'écran des jalons** — plus d'écran d'accueil de reprise, voir #93 |
| 11 | Liste des jalons | **Affichage partiel et ancré en bas** — tranchés, courant, un seul à venir, voir #101 |
| 12 | Défilement de l'Historique | **Trois blocs indépendants** au lieu d'une zone défilante unique, voir #107 |
| 13 | Encodage de l'export CSV | **Fichier entièrement ASCII**, sans BOM UTF-8 — les accents sont repliés (« Départ » → « Depart ») |

### Écarts assumés au périmètre d'origine (6 à 13)

Ces huit points **contredisent la lettre** des sections citées. Ils sont le fruit d'un
test sur appareil et ont été validés en connaissance de cause ; ils sont consignés ici
parce que le script `scripts/check-docs-coherence.sh` ne vérifie que des invariants
mécaniques — versions, chemins, liens — et ne verrait pas une contradiction de prose.

**6. Correction restreinte.** Le §1.2 annonce une correction « à tout moment ». Elle est
en fait réservée aux jalons **tranchés** : corriger un jalon qu'on n'a pas encore atteint
reviendrait à inventer un passage, et ferait de l'overlay un second moyen de valider, en
doublon de la barre d'action.

**7. Chrono déplacé.** Le §3.2 décrit un « mini-indicateur discret ». Il vit désormais sur
la **ligne du jalon courant**, avec une emphase propre : deux chronomètres côte à côte
dans le bandeau se lisaient mal, et celui qui mesure le tronçon en cours appartient au
jalon qu'il mesure.

**8. « Abandonner » au lieu d'« Annuler ».** Le §3.3 nommait le bouton « Annuler », mot
qui se lit comme un abandon du trajet sur un écran dont l'autre bouton archive
définitivement. #87 a montré que c'en est bien un : le POC y appelle `discardTrip`, et rend
par ailleurs les jalons du récapitulatif corrigeables **sur place**. Le §3.3 dit désormais
les deux, et l'écart n'en est plus un — le libellé explicite simplement ce que le mot
« Annuler » cachait.

**9. Deux vues, deux rôles.** Le §3.2 réservait l'heure absolue au bandeau, et le §3.3
faisait afficher au récapitulatif une liste de jalons **en plus** de la liste des tronçons.
Les deux règles avaient le même défaut, hérité du POC.

Un jalon est un **instant**, un tronçon est une **durée**. Notre liste de jalons était
étiquetée par un instant mais **valorisée par une durée** : elle empruntait sa valeur au
tronçon qui la précédait. D'où deux symptômes d'une seule cause — la ligne du départ
n'affichait qu'un tiret, faute de tronçon avant elle (le POC le savait : son `jalonsCard`
force `durTxt = '—'` pour `i === 0`), et les quatre durées inter-jalons du récapitulatif
étaient **les quatre durées des tronçons**, affichées deux fois.

La réponse sépare les rôles plutôt que d'arbitrer un affichage. L'écran actif garde une
liste de **jalons**, parce que c'en est une liste d'actions : on valide un jalon à la fois,
et l'heure y comble la première ligne. Le récapitulatif ne montre que des **tronçons** :
c'est une lecture, et ce sont les durées qu'on vient y chercher.

Deux conséquences assumées. La correction, sur le récapitulatif, passe désormais par les
tronçons — chacun ouvre le jalon qui le ferme — et par la cellule « Départ » du bandeau
pour le jalon 0 ; la couverture reste complète et aucun clic n'est ambigu. Et la durée
**cumulée** qu'une ligne de jalon donnait en enjambant un jalon ignoré disparaît de cet
écran : un tronçon non mesuré l'est honnêtement, cette durée n'étant attribuable ni à l'un
ni à l'autre de ses deux voisins. Elle reste visible sur l'écran actif, et le tronçon
« Non mesuré » est précisément l'endroit où l'on rebouche le trou.

Le modèle de domaine n'a pas bougé : `Trip.times` reste un horodatage par jalon. C'est un
changement de rendu.

**10. Plus d'écran de reprise.** Le §3.1 décrivait, pour un trajet en cours, une bannière de
statut surmontant « Reprendre » et « Abandonner ». Sur appareil, cela donnait deux lignes
d'information puis un écran entier de vide : l'écran ne faisait que demander l'autorisation
d'aller là où l'on veut aller, sans rien dire de l'état du trajet.

L'accueil s'efface donc devant le trajet en cours, et la décision se prend dans une fenêtre
posée sur l'écran des jalons. Le gain n'est pas que de la place : la fenêtre s'ouvre sur
l'écran qui porte déjà la frise, les compteurs et les jalons pointés, si bien que
l'information manquante est là sans être dupliquée. L'accueil redevient l'écran où l'on
**démarre** un trajet, et l'abandon suit la fenêtre.

Deux conséquences de navigation. L'accueil est retiré de la pile en redirigeant, sans quoi
le retour y reviendrait pour être aussitôt redirigé — une boucle ; quitter l'écran des
jalons quitte donc l'application. Et la route du trajet actif porte désormais un drapeau
`resuming`, seul moyen de distinguer un trajet retrouvé d'un trajet qu'on vient de démarrer.

**11. Liste des jalons partielle, et ramenée sous le pouce.** Le §3.2 décrit une liste des
cinq jalons, occupant la zone défilante médiane. Le test sur appareil a montré deux
défauts : on **tape** ces lignes pour corriger un jalon, en roulant, et elles étaient dans
le haut de l'écran, hors de la zone d'atteinte du pouce ; et les cinq lignes se
ressemblaient trop pour dire d'un coup d'œil où l'on en est.

La liste est donc ancrée **en bas**, au contact de la barre d'action, et réduite à ce qui
sert : les jalons tranchés en petit, le jalon courant en emphase, et **un seul** jalon à
venir, grisé et inerte. Les suivants ne disent rien qu'on ait besoin de lire en roulant, et
la frise de progression porte déjà la vue d'ensemble — c'est son rôle depuis le §3.2.

Le traitement discret des jalons tranchés est **typographique** : leur cible tactile garde
ses 48 dp et ses 8 dp d'écart, puisqu'ils restent le point d'entrée de la correction.

**12. Trois défilements sur l'Historique.** Le §1.6 impose une « zone scrollable unique ».
L'Historique en a désormais deux — moyennes par tronçon et trajets récents — sous un bloc
« Trajet complet » fixe.

La règle visait la saisie en roulant : un contenu qui grandit ne doit pas repousser un
bouton d'action hors de l'écran. L'enjeu de l'Historique est inverse et se joue à l'arrêt —
garder les moyennes sous les yeux **pendant** qu'on parcourt les trajets qu'elles résument.
Avec un défilement unique, atteindre les trajets récents chassait de l'écran ce à quoi on
voulait justement les comparer.

Les zones fixes haute et basse restent intactes : le motif de la règle, lui, est préservé.
La contrepartie est que deux zones défilantes deviennent étroites à fort grossissement de
police — à regarder au lot 7, avec les tests sur petit écran.

**13. L'export CSV est en ASCII, sans BOM.** Le POC écrivait une marque d'ordre des octets
UTF-8 en tête de fichier, seul moyen qu'Excel n'affiche pas « Départ » en mojibake. Le
portage l'avait reprise telle quelle.

À l'usage, Google Sheets **ignore** cette marque : il décode le fichier en Latin-1, affiche
donc la marque elle-même dans la première cellule (« ï»¿ ») et manque l'accent malgré tout
(« DÃ©part »). Un fichier, deux symptômes, et aucun levier côté application — c'est le
lecteur qui choisit son décodage.

Le constat qui tranche est que l'accent de « Départ » était le **seul** octet non ASCII de
tout l'export : en-tête technique, sens, dates ISO et heures le sont déjà. Un détecteur
d'encodage n'avait donc presque rien pour trancher, et il a tranché de travers.

On le retire à la source plutôt que de parier sur un encodage. Un fichier ASCII se lit à
l'identique en UTF-8, en Latin-1 et en Windows-1252 : la question de l'encodage ne se pose
plus, chez aucun lecteur. La perte — « Depart » sans accent — ne porte que sur le fichier,
l'écran gardant son libellé ; et l'export a de toute façon déjà son propre vocabulaire,
son en-tête étant en minuscules techniques.

### Grain de l'ondulation d'appui

Depuis Android 12, `android.graphics.drawable.RippleDrawable` superpose à l'onde un
scintillement — `FORCE_PATTERNED_STYLE = true` dans AOSP, donc sur **toute** ondulation,
quel que soit l'appareil. Sa couleur est `DEFAULT_EFFECT_COLOR = 0x8dffffff`, du blanc à
55 %, ce qui se voit sur le fond quasi noir du thème nuit (`#0F1115`).

Le levier existe côté plateforme — `setEffectColor`, et le shader multiplie le
scintillement par l'alpha de cette couleur — mais Compose n'y donne aucun accès : il crée
son `UnprojectedRipple` en interne, la pose en fond d'une `RippleHostView` privée, et
`RippleConfiguration` n'expose que la couleur et les alphas d'état. Baisser l'alpha de
l'ondulation ne fait qu'aggraver le contraste, celui du scintillement en étant
indépendant.

Le supprimer demanderait donc de réimplémenter l'ondulation. Cela a été fait, mesuré à
environ 220 lignes, puis **écarté** : le coût de maintenance d'un composant d'animation
maison dépasse la gêne visuelle. À rouvrir si Compose expose un jour `effectColor`.

