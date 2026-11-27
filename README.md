# Blasa — API REST et application web

> Covoiturage étudiant en Tunisie. Ce dépôt contient l'**API REST** (Java, Jersey) et l'**application web**
> du projet Blasa (module **Services Web**, Mme Fatma Krichen).
> La base de données Redis est détaillée dans le dépôt [**Blasa-NOSQL**](https://github.com/nessryne-chouchene/Blasa-NOSQL) (module NoSQL).

**Auteurs :** Nessryne Chouchene, Omayma Hamdy — Data Engineering, Faculté des Sciences de Sfax (FSS)

## Fonctionnalités

- **Utilisateurs** : créer, lire, remplacer (`PUT`), modifier (`PATCH`), supprimer un compte.
- **Trajets** : proposer, chercher par villes ou autour d'un point, modifier, annuler.
- **Réservations** et **notes** des conducteurs, **classement** et statistiques.
- **Carte interactive** : un clic sur la carte lance une recherche dans un rayon (`GEOSEARCH`).
- **Mise à jour en direct** : une réservation met à jour les autres écrans (Redis Pub/Sub puis SSE).
- **Compte à rebours** : chaque trajet affiche le temps avant son départ, puis disparaît (TTL Redis).
- **Mode technique** : requêtes HTTP, codes de réponse, commandes Redis et contenu de Redis en direct.
- **Documentation OpenAPI / Swagger UI** : toutes les routes sont décrites et testables depuis le navigateur.
- **Pagination** (optionnelle), **authentification JWT** (optionnelle) et **HTTPS** (optionnel) : désactivés par défaut,
  le comportement historique de l'API ne change pas tant qu'on ne les active pas.

## Flux de données

```
Page web  →  API REST (Jersey)  →  Redis  →  réponse JSON  →  Page web
```

## Lancer le projet

Prérequis : **Docker Desktop** ouvert, **JDK 21** et **Maven 3.9**.

Double-cliquer sur **`start.cmd`**. Il démarre Redis, charge les données de démonstration (départs dans 20 à 50 heures),
lance l'API et ouvre `http://localhost:8080/`.

En terminal :

```powershell
powershell -ExecutionPolicy Bypass -File .\start.ps1          # démarre
powershell -ExecutionPolicy Bypass -File .\start.ps1 -Reset   # repart de données neuves
```

## Documentation de l'API (OpenAPI / Swagger)

Une fois l'API lancée : **http://localhost:8080/docs.html** (Swagger UI). La spécification est dans
`src/main/resources/web/openapi.yaml` (OpenAPI 3.0.3) et reste servie sur `http://localhost:8080/openapi.yaml`.
Le bouton *Try it out* permet d'appeler chaque route directement.

## Endpoints

Toutes les URI sont préfixées par `/api`.

| Méthode | URI | Rôle | Succès | Erreurs |
|---|---|---|---|---|
| POST | `/users` | créer un compte | 201 | 400 |
| GET | `/users` | lister les utilisateurs | 200 | |
| GET | `/users/{id}` | lire un profil | 200 | 404 |
| PUT | `/users/{id}` | remplacer un profil | 200 | 400, 404 |
| PATCH | `/users/{id}` | modifier des champs | 200 | 400, 404 |
| DELETE | `/users/{id}` | supprimer le compte | 204 | 404 |
| POST | `/rides` | proposer un trajet | 201 | 400, 404 |
| GET | `/rides` | chercher (`from`, `to`, `lat`, `lon`, `radius`) | 200 | 400 |
| GET | `/rides/{id}` | détail d'un trajet | 200 | 404 |
| PUT | `/rides/{id}` | remplacer prix, places, date | 200 | 400, 404, 409 |
| PATCH | `/rides/{id}` | modifier prix, places, date, statut | 200 | 400, 404, 409 |
| DELETE | `/rides/{id}` | annuler le trajet | 204 | 404 |
| POST | `/rides/{id}/bookings` | réserver une place | 201 | 400, 404, 409 |
| DELETE | `/rides/{id}/bookings/{userId}` | annuler une réservation | 204 | 404 |
| POST | `/users/{id}/ratings` | noter un conducteur (1 à 5) | 201 | 400, 404 |
| POST | `/auth/login` | obtenir un jeton JWT | 200 | 400, 401 |
| GET | `/stats/top-drivers` | meilleurs conducteurs | 200 | 400 |
| GET | `/stats/summary` | compteurs globaux | 200 | |
| GET | `/events` | flux en direct (Server-Sent Events) | 200 | |

### Pagination (optionnelle)

`GET /users` et `GET /rides` acceptent `page` (à partir de 1) et `size` (20 par défaut, 100 maximum) :

```
GET /api/rides?page=2&size=5
GET /api/rides?from=sfax&to=tunis&page=1&size=10      (se combine avec la recherche)
```

La réponse reste un tableau JSON ; le total est dans les en-têtes `X-Total-Count`, `X-Total-Pages`, `X-Page`, `X-Page-Size`.
**Sans `page` ni `size`, la liste complète est renvoyée comme avant** (la page web et Postman ne sont pas touchés).
Valeurs invalides (`page=0`, `size=500`…) : `400`.

### Authentification JWT (optionnelle)

Par défaut l'API est ouverte. Pour exiger un jeton sur les écritures :

```powershell
$env:AUTH_ENABLED = "true"
$env:JWT_SECRET   = "un-secret-d-au-moins-16-caracteres"     # sinon un secret aléatoire est généré à chaque démarrage
powershell -ExecutionPolicy Bypass -File .\start.ps1
```

1. **Créer un compte avec mot de passe** (8 caractères minimum, stocké haché en PBKDF2, jamais renvoyé) :
   `POST /api/users` avec `{"name": "...", "email": "...", "city": "...", "password": "motdepasse123"}`.
   Les comptes de démonstration n'ont pas de mot de passe : en définir un avec `PATCH /api/users/{id}` (`{"password": "..."}`).
2. **Se connecter** : `POST /api/auth/login` avec `{"email": "...", "password": "..."}` renvoie `{"token": "..."}` (valable 1 h, `JWT_TTL_SECONDS`).
3. **Appeler l'API** avec l'en-tête `Authorization: Bearer <jeton>`.

Règles quand `AUTH_ENABLED=true` : les `GET`, `POST /users` et `POST /auth/login` restent publics ; toute autre écriture sans jeton
valide répond `401` ; modifier un compte, un trajet ou une réservation qui n'est pas le sien répond `403`.

> La page web `http://localhost:8080/` n'envoie pas encore de jeton : avec `AUTH_ENABLED=true`, utiliser Swagger UI ou Postman.

### HTTPS (optionnel)

1. Générer un certificat auto-signé (une fois, dans le dossier du projet) :

```powershell
keytool -genkeypair -alias blasa -keyalg RSA -keysize 2048 -validity 365 -storetype PKCS12 -keystore blasa.p12 -storepass changeit -dname "CN=localhost" -ext "SAN=dns:localhost,ip:127.0.0.1"
```

2. Lancer en HTTPS :

```powershell
$env:HTTPS_ENABLED = "true"                 # KEYSTORE_PATH (défaut blasa.p12), KEYSTORE_PASSWORD (défaut changeit), PORT (défaut 8443)
powershell -ExecutionPolicy Bypass -File .\start.ps1
```

3. Ouvrir **https://localhost:8443/**. Le navigateur avertit que le certificat est auto-signé : c'est normal en local,
   il faut accepter l'exception. En production, on utiliserait un certificat d'une autorité reconnue (par exemple Let's Encrypt).
   `blasa.p12` est ignoré par Git : ne jamais publier une clé privée.

JAX-RS n'a pas d'annotation `@PATCH` : le projet en définit une (`PATCH.java`) avec `@HttpMethod("PATCH")`.

## Tester

- **Application** : `http://localhost:8080/`. Le bouton « Mode technique » ouvre le panneau des requêtes, avec un
  **scénario de démonstration** en 16 appels (POST, GET, PUT, PATCH, DELETE ; codes 201, 200, 204, 404, 409).
- **Tests automatisés (JUnit 5)** : `mvn test`. Les tests de pagination, JWT et hachage tournent sans Redis ; ceux des
  repositories (utilisateurs, trajets, réservations, recherche GEO) ont besoin de Redis (`docker compose up -d`) et
  sont ignorés s'il est éteint. Ils utilisent la **base Redis n° 15** : les données de démonstration (base 0) ne sont jamais touchées.
- **Postman** : importer `postman/blasa.postman_collection.json`, puis *Run collection* : 24 requêtes, chacune vérifie son code de réponse.

## Structure

```
pom.xml                        dépendances Maven
src/main/java/soa/blasa/       model, repository (Redis), resource (REST), security (JWT), util (pagination)
src/main/resources/web/        page web, docs.html (Swagger UI), openapi.yaml
src/test/java/soa/blasa/       tests JUnit
docker-compose.yml             Redis
start.ps1 / start.cmd          lance tout
postman/                       collection de tests
redis/01_seed.template.redis   modèle des données de démo
rapports/                      rapport LaTeX
src/main/java/soa/blasa/
  Main.java                    démarre le serveur (Grizzly)
  model/                       User, Ride
  repository/                  accès à Redis (Jedis), Pub/Sub
  resource/                    ressources REST (JAX-RS)
src/main/resources/web/        application web (index.html)
```

## Technologies

Java 21 · Maven · Jersey 2.41 (JAX-RS) · Jackson · Jersey SSE · Grizzly · Jedis · Redis 7 · Docker · Leaflet / OpenStreetMap · Postman

## Limites connues

- Pas d'authentification : le profil est choisi dans l'interface.
- Pas de pagination des résultats.
- La carte nécessite une connexion Internet (Leaflet et OpenStreetMap).
