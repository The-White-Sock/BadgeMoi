# Cahier des charges — Portage Android de l'application "Trajet"

## 0. Contexte

L'application "Trajet" existe aujourd'hui sous forme d'un fichier HTML/CSS/JS autonome (une seule page, aucun backend, aucune dépendance réseau à l'exécution hormis le chargement initial des polices). Elle sert à chronométrer un trajet domicile-travail en Onewheel, jalon par jalon, à archiver les trajets, et à en tirer des moyennes.

Le POC de référence est conservé dans ce dépôt : [`docs/poc/trajet.html`](poc/trajet.html).

Ce document définit le périmètre et les choix nécessaires pour transformer cet existant en application Android installable (Play Store), en conservant les décisions d'ergonomie déjà validées (usage au pouce en roulant, thème jour/nuit, iconographie monochrome) plutôt que de repartir d'une page blanche.

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
- Cas "trajet en cours" : bannière de statut (direction, heure de départ), bouton "Reprendre" pleine largeur, bouton "Abandonner" secondaire.
- Bascule de thème et onglets de navigation (Trajet / Historique) dans un bandeau haut compact, une seule ligne.

### 3.2 Écran "Trajet actif"
- Frise de progression façon plan de ligne (jalons reliés par un trait, jalon courant mis en évidence, jalons posés distingués visuellement).
- Bandeau Départ / Écoulé : heure de départ figée, durée écoulée en direct tant que le dernier jalon n'est pas posé.
- Mini-indicateur "temps depuis le dernier jalon", discret, mis à jour chaque seconde.
- Liste des jalons : icône + libellé court + **temps écoulé depuis le jalon précédent** (pas l'heure absolue, réservée au bandeau). Correction possible par tap sur une ligne.
- Barre d'action fixe en bas : bouton "Valider" (tap simple, retour haptique, flash de confirmation, verrou anti-double-tap de 400 ms) et bouton "Passer" (appui maintenu 650 ms).

### 3.3 Écran "Récapitulatif"
- Bandeau Départ/Arrivée (les deux heures sont connues à ce stade).
- Liste des tronçons nommés (Ride/Attente/Train/Ride) avec durée.
- Liste des jalons avec durée inter-jalons (même composant que l'écran actif).
- Boutons "Annuler" / "Enregistrer" fixes en bas.

### 3.4 Écran "Historique"
- Sélecteur Aller/Retour.
- Bloc "Trajet complet" : durée moyenne + nombre de trajets archivés.
- Moyennes par tronçon.
- Liste des trajets récents (10 derniers), durée colorée selon l'écart à la moyenne (plus rapide/plus lent).
- Export CSV, purge de l'historique (confirmation à double appui).

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
