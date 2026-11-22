# ATOMIC TV Doctor

**Diagnostic, applications et maintenance Android TV / Google TV** — [JB Atomic Industries](https://jbatomic.com/).

[![Android TV build](https://github.com/ElecktroSkwiiZz/atomic-tv-doctor/actions/workflows/android.yml/badge.svg)](https://github.com/ElecktroSkwiiZz/atomic-tv-doctor/actions/workflows/android.yml)

## Version 1.3.0 — Fonctionnalités

### Nouveau dans la V1.3

- **Navigation TV corrigée :** les rangées Maintenance et Tableau de bord ne fixent plus des hauteurs susceptibles de masquer les boutons ; elles s'adaptent au contenu et défilent si nécessaire.
- **Langues FR / EN / ES / RU :** sélecteur dans l'en-tête TV et sélecteur indépendant dans le portail PC ; principaux onglets et contrôles traduits, traduction des libellés techniques en cours d'extension.
- **Résolution de dalle :** réglage depuis l'onglet Technique pour déclarer la résolution native réelle (4K UHD, Full HD, indéterminée). L'application conserve cette valeur et l'affiche distinctement de l'interface Android en 1080p. Un choix utilisateur est explicitement étiqueté comme tel ; il ne s'agit pas d'une mesure automatique.
- **Gestionnaire de fichiers intégré :** sélection d'un dossier autorisé, navigation par dossiers, tri par nom ou taille, ouverture des fichiers, suppression individuelle confirmée et statistiques de volume. L'inventaire du dossier autorisé est consultable en lecture seule depuis le portail PC. Aucun effacement en masse.
- **RAM et performances — ROOT requis :** proposition d'arrêter les processus que le gestionnaire Android considère comme secondaires, avec mesure de la RAM disponible avant/après. Les gains peuvent être temporaires et certaines applications seront relancées automatiquement. Ne pas présenter comme une accélération garantie.

**Limite Android TV / Android 11 :** Android exige une autorisation de dossier via son sélecteur de fichiers (Storage Access Framework). La racine du stockage, le dossier Download racine et Android/data ne peuvent pas être sélectionnés sur Android 11+. Certaines TV n'installent aucun sélecteur de dossier compatible : dans ce cas l'explorateur affichera une limitation et ne tentera pas de contourner la protection.

### Tableau de bord TV
- Interface compacte à **six cartes** : appareil, RAM, stockage, affichage, réseau, codecs.
- Jauges circulaires RAM et stockage en **octets convertis en Mo/Go**, taux d'occupation réel.
- Focus télécommande cyan visible, sections défilantes uniquement lorsque nécessaire.
- Résolution de **rendu Android** distincte des modes exposés. **La résolution native de la dalle ne peut pas toujours être déterminée** par les API Android standard. Une TV 4K peut afficher une surface d'application en 1920×1080.

### Gestionnaire d'applications intégré
- Liste des applications exposées par les lanceurs Android TV / Android (les services masqués peuvent ne pas apparaître).
- **Ouvrir** une application sans quitter l'outil pour les paramètres.
- **Désinstaller** une application utilisateur avec la confirmation exigée par Android.
- Actions **ROOT requis** (pictogramme bouclier orange) : arrêter une application utilisateur, la geler et la réactiver. Les applications système sont exclues des opérations de désactivation.
- Les applications gelées par Doctor sont mémorisées pour proposer leur réactivation.

### Entretien
- Nettoyage sûr du **cache propre de Doctor**, sans toucher aux fichiers d'autres applications.
- **ROOT requis** : purge des caches récupérables Android par `pm trim-caches 16G`, après confirmation. Le volume libéré peut être nul ; ce n'est pas un gain de performance garanti.
- Aucun effacement automatique de données utilisateur, aucun « booster RAM » trompeur.
- Vérification des droits ROOT déclenchée explicitement dans la section Maintenance. **Aucune demande de ROOT au démarrage**.

### Diagnostic technique
- Android : modèle, carte, matériel, version, correctif, ABI, CPU.
- RAM : totale, libre, seuil mémoire faible.
- Stockage : total, disponible, utilisé, cache propre.
- Réseau : Ethernet/Wi‑Fi, IP, interface et capacités de liaison annoncées (**non assimilées à un speedtest**).
- Vidéo/audio : résolution de rendu, modes d'affichage signalés, Hz, HDR déclaré, sorties audio, décodeurs multimédia.
- Partage JSON du diagnostic.

### Portail web PC, tablette et smartphone
- Tableau de bord responsive, graphiques RAM/stockage, inventaire des applications en lecture seule, rapport technique et export JSON.
- Nettoyage confirmé du cache propre de Doctor depuis le portail.
- **Port HTTP modifiable 1024–65535** (8766 par défaut), activation manuelle depuis la TV, jeton privé renouvelé à chaque activation.
- Les **actions ROOT s'exécutent uniquement sur la TV après confirmation physique**, pas sur le portail web.

## APK de test

1. Ouvrir [GitHub Actions](https://github.com/ElecktroSkwiiZz/atomic-tv-doctor/actions).
2. Sélectionner le dernier run **réussi** de `main`.
3. Sous **Artifacts**, télécharger `ATOMIC-TV-Doctor-DEBUG` et extraire `app-debug.apk`.
4. Installer via ADB ou un gestionnaire d'APK. L'identifiant DEBUG est `com.jbatomic.tvdoctor.debug`.

Le build GitHub vérifie la syntaxe JavaScript du portail et compile l'APK Android. **Une compilation réussie ne remplace pas un test sur un téléviseur réel**, notamment pour le focus D-pad et la disponibilité des commandes du constructeur.

## Mise à jour et releases signées

L'application **cherche automatiquement les mises à jour** via GitHub Releases à l'ouverture, au plus toutes les 12 heures, ainsi qu'à la demande. En cas de nouvelle release signée, elle télécharge l'APK officiel et demande l'accord d'Android pour l'installation. Pas d'installation silencieuse.

Secrets de publication GitHub Actions à configurer :

| Nom | Contenu |
| --- | --- |
| `DOCTOR_KEYSTORE_BASE64` | Keystore Android converti en base64 |
| `DOCTOR_STORE_PASSWORD` | Mot de passe du keystore |
| `DOCTOR_KEY_ALIAS` | Alias de signature |
| `DOCTOR_KEY_PASSWORD` | Mot de passe de la clé |

Ne **jamais** committer la clé. Pour une release : incrémenter `versionCode` et `versionName`, puis publier un tag Git `vX.Y.Z` identique à `versionName`. Les nouvelles APK officielles doivent conserver la même signature Android.

Les builds DEBUG et RELEASE ont des identifiants distincts ; installer une release ne met pas directement à jour un build DEBUG.

## Sécurité et limites

- Sans ROOT, Android interdit l'arrêt forcé direct d'autres applications et leur désactivation arbitraire. La désinstallation passe par la confirmation système.
- Avec ROOT, Doctor limite les commandes privilégiées à une **liste blanche** ; les noms de paquets sont validés et les applications système ne peuvent pas être gelées par Doctor.
- **Aucun accès ROOT via le portail HTTP**. Les actions root exigent une confirmation côté TV.
- Ne pas activer le portail HTTP sur un Wi-Fi public, et ne pas exposer son port sur Internet : la liaison locale n'est pas chiffrée.
- Les mesures vidéo et codec proviennent des déclarations Android ; elles ne prouvent pas les capacités exactes de chaque entrée HDMI, ni la résolution native de la dalle. Aucun FPS HDMI/ARC/eARC universel n'est promis.
- Rooter un téléviseur peut compromettre la sécurité et les garanties ; Doctor **ne root pas** l'appareil.

## Architecture

- `MainActivity.java` : interface TV et actions utilisateur.
- `DoctorSnapshot.java` : informations système et formatage Mo/Go.
- `DoctorApps.java` : inventaire des applications visibles par Android.
- `DoctorRoot.java` : commandes root strictement limitées et contrôles de privilèges.
- `DoctorMaintenance.java` : nettoyage strictement limité au cache appartenant à Doctor.
- `DoctorWebServer.java` : serveur local à jeton, API de diagnostic, inventaire et nettoyage non privilégié.
- `DoctorGauge.java` : jauges natives.
- `app/src/main/assets/` : portail web autonome.
- `.github/workflows/android.yml` : builds et publications officielles conditionnées à la signature.

© JB Atomic Industries. Version de développement.
