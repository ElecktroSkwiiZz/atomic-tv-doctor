# ATOMIC TV Doctor

Application de diagnostic **Android TV / Google TV** — **JB Atomic Industries** ([site officiel](https://jbatomic.com/)).

## Fonctions v1.0.0

- Interface TV utilisable à la télécommande : modèle, version d’Android, cœurs CPU, RAM, stockage, réseau, décodeurs H.264, HEVC, AV1.
- Portail de diagnostic **web local pour PC, tablette et smartphone**, avec export JSON et rafraîchissement.
- Serveur HTTP **désactivé par défaut**, activable dans l'application TV, **port modifiable entre 1024 et 65535** (8766 par défaut).
- URL d’accès privée avec jeton aléatoire renouvelé à chaque activation. Aucune commande système n'est exposée au web.
- Vérification des mises à jour **GitHub Releases** à l'ouverture (maximum une fois toutes les 12 heures), et à la demande. Téléchargement d'une nouvelle APK officielle, puis **confirmation obligatoire par Android** pour l'installation.

## Tester l'application

Ouvrir [Actions](https://github.com/ElecktroSkwiiZz/atomic-tv-doctor/actions), sélectionner le dernier workflow réussi, puis télécharger son artefact `ATOMIC-TV-Doctor-DEBUG`. Extraire l’APK et l’installer sur la TV.

Une fois lancée, renseigner le port et activer le portail web. Sur le PC connecté au même réseau, ouvrir **l'URL complète** affichée à l’écran TV, contenant le jeton d’accès. Le jeton est privé. La TV et le PC doivent communiquer sur le réseau local.

La version **DEBUG** est `com.jbatomic.tvdoctor.debug` ; une APK release officielle utilisera `com.jbatomic.tvdoctor`.

## Publier des versions signées et permettre les mises à jour

Dans GitHub → Settings → Secrets and variables → Actions, configurer :

| Secret | Valeur |
| --- | --- |
| `DOCTOR_KEYSTORE_BASE64` | Keystore Android encodé en base64 |
| `DOCTOR_STORE_PASSWORD` | Mot de passe du keystore |
| `DOCTOR_KEY_ALIAS` | Alias de signature |
| `DOCTOR_KEY_PASSWORD` | Mot de passe de la clé |

Protéger soigneusement la clé : **toutes les mises à jour officielles doivent conserver la même signature**. Après augmentation de `versionCode` et `versionName` dans `app/build.gradle`, pousser un tag `vX.Y.Z` correspondant à la version. Le workflow compile et publie `ATOMIC-TV-Doctor-AndroidTV.apk` dans GitHub Releases. Sans secrets de signature, le workflow refuse volontairement la publication.

Les mises à jour sont **détectées automatiquement**, mais l'installation ne peut pas être silencieuse pour une application Android TV standard. L'utilisateur doit l'autoriser.

## Sécurité et limites

Le serveur local utilise **HTTP non chiffré** : ne l'activer que sur un réseau privé de confiance. Il est arrêté lorsque l'application est fermée ou le portail désactivé. Les diagnostics restent sur la TV jusqu'à consultation volontaire via le réseau local.

Les API Android ne fournissent pas toutes les mesures : aucune promesse de FPS HDMI, de température universelle ou d'état eARC. La présence d'un décodeur ne garantit pas chaque profil ou résolution.

## Code

- `MainActivity.java` — interface Android TV, paramètres du portail, recherche de nouvelles versions.
- `DoctorSnapshot.java` — informations matériel/système.
- `DoctorWebServer.java` — serveur HTTP local en lecture seule.
- `InstallerProvider.java` — fichier APK destiné à l'installateur Android.
- `app/src/main/assets/` — interface web HTML/CSS/JavaScript autonome.
- `.github/workflows/android.yml` — build et publication GitHub Releases.

© JB Atomic Industries. Code en cours de développement.
