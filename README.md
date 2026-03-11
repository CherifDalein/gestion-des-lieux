# GestionDesLieux - API de gestion et partage de lieux

Application web Spring Boot permettant de gérer des lieux personnels, des collections, des imports/exports géographiques, des images et le partage sécurisé via JWT et tokens d'accès.

## Membres du groupe

- DIALLO Mamadou Cherif
- LAMINOU AMADOU Abdoul Bassit
- KANE Abdou Latif
- IDRISS Abakar Nokour
- DIALLO Mohamed Moussa

## Prérequis

- **Java 17+** (obligatoire)
- **Gradle** optionnel si vous utilisez le **Gradle Wrapper** fourni (`./gradlew` ou `gradlew.bat`)

## Installation et lancement

### Étape 1 : Vérifier Java

```bash
# Vérifier la version de Java
java -version

# Doit afficher Java 17 ou supérieur
```

### Étape 2 : Vérifier Gradle

Option recommandée : utilisez le Gradle Wrapper déjà présent dans le projet.

```bash
# Linux / Mac
./gradlew -v

# Windows
gradlew.bat -v
```

Si vous n'avez pas le wrapper, vous pouvez installer Gradle système.

Pour Windows, consultez [gradle.org/install](https://gradle.org/install/).

Pour Linux / Mac :

```bash
# macOS
brew install gradle

# SDKMAN (Linux/Mac)
sdk install gradle
```

### Étape 3 : Lancer l'application

#### Linux / Mac

```bash
chmod +x run.sh
./run.sh run
```

#### Windows

```bat
run.bat run
```

**Note** : les scripts détectent automatiquement `gradlew`/`gradlew.bat` ou `gradle` système.

### Commandes disponibles

| Commande | Description |
| -------- | ----------- |
| `compile` | Compile le code source |
| `test` | Lance les tests |
| `package` | Crée le JAR exécutable Spring Boot |
| `run` | Lance l'application (défaut) |
| `clean` | Nettoie le projet |
| `all` | Build complet (compile + tests + package) |

Exemples :

```bash
# Linux / Mac
./run.sh compile
./run.sh test
./run.sh package

# Windows
run.bat compile
run.bat test
run.bat package
```

## Accès à l'application

Une fois lancée, l'application est accessible sur :

- **API REST** : [http://localhost:8080](http://localhost:8080)
- **Documentation OpenAPI JSON** : [http://localhost:8080/api-docs](http://localhost:8080/api-docs)
- **Console H2** : [http://localhost:8080/h2-console](http://localhost:8080/h2-console)

Configuration H2 par défaut :

- **JDBC URL** : `jdbc:h2:file:./data/lieux_db;MODE=MySQL;DB_CLOSE_DELAY=-1`
- **Username** : `sa`
- **Password** : (vide)

## Comptes de test

Des données de démonstration sont créées automatiquement au premier démarrage si la base est vide.

### Utilisateurs

- `alice@test.com` / `password123`
- `bob@test.com` / `password123`

### Administrateur

- `admin@test.com` / `admin123`

## Fonctionnalités principales

- Authentification JWT avec access token et refresh token
- Gestion CRUD des lieux personnels
- Recherche par texte, tag et rayon géographique
- Recherche de lieux à proximité (Haversine)
- Gestion de collections de lieux
- Export de collections en `GeoJSON`, `GPX` et `KML`
- Import de lieux depuis fichiers ou contenu brut `GeoJSON`, `GPX` et `KML`
- Upload et suppression d'images associées aux lieux
- Partage sécurisé via tokens d'accès sur lieux et collections
- Partage temporaire de la position courante
- Statistiques et mise à jour du profil utilisateur
- API documentée via OpenAPI / Springdoc

## Endpoints principaux

- `POST /api/auth/register` : création de compte
- `POST /api/auth/login` : connexion
- `POST /api/auth/refresh` : renouvellement des tokens
- `GET /api/places` : liste des lieux
- `GET /api/places/search` : recherche de lieux
- `GET /api/places/nearby` : lieux proches
- `POST /api/collections/{id}/share` : partage d'une collection
- `GET /api/collections/{id}/export` : export GeoJSON / GPX / KML
- `POST /api/import` : import de fichier géographique
- `POST /api/location/share` : partage de position courante
- `GET /api/tokens/discover?token=...` : découverte d'une ressource partagée

## Architecture

- **Framework** : Spring Boot 4.0.1
- **Langage** : Java 17
- **Build** : Gradle
- **Base de données** : H2
- **Sécurité** : Spring Security + JWT
- **Documentation API** : Springdoc OpenAPI
- **Déploiement** : Docker + Render

## Déploiement

Le projet contient :

- `Dockerfile` pour construire l'application
- `render.yaml` pour un déploiement automatique sur Render

## Dépannage

### Erreur : `gradlew` ou `gradlew.bat` introuvable

Le projet contient déjà le wrapper Gradle. Vérifiez que vous êtes dans la racine du dépôt.

### Erreur : ni wrapper ni Gradle système disponibles

Installez Gradle ou restaurez les fichiers `gradlew`, `gradlew.bat` et `gradle/wrapper/`.

### Port 8080 déjà utilisé

Modifiez le port dans [application.properties](src/main/resources/application.properties) :

```properties
server.port=8081
```

### Base H2 verrouillée

Nettoyez les fichiers de build puis relancez :

```bash
# Linux / Mac
./run.sh clean

# Windows
run.bat clean
```

### Modifier la configuration par défaut

La configuration locale est directement définie dans [application.properties](src/main/resources/application.properties). Vous pouvez y ajuster le port, la base H2, le secret JWT ou le dossier d'upload si nécessaire.

## Structure du projet

```text
gestion-des-lieux/
├── src/main/java/org/example/gestiondeslieux/
│   ├── controller/api/   # Endpoints REST
│   ├── service/          # Logique métier
│   ├── repository/       # Accès aux données
│   ├── model/            # Entités JPA
│   ├── dto/              # Objets d'échange
│   ├── request/          # Payloads d'entrée
│   ├── response/         # Réponses standardisées
│   ├── security/         # JWT et filtres de sécurité
│   ├── config/           # Configuration Spring
│   └── data/             # Initialisation des données de démo
├── src/main/resources/
│   └── application.properties
├── src/test/java/        # Tests d'intégration et de service
├── run.sh                # Script Linux / Mac
├── run.bat               # Script Windows
├── build.gradle          # Configuration Gradle
├── gradlew               # Gradle Wrapper Unix
├── gradlew.bat           # Gradle Wrapper Windows
├── Dockerfile            # Image Docker
└── render.yaml           # Déploiement Render
```
