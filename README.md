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
- **Documentation OpenAPI / Swagger UI** : toutes les routes sont décrites et testables depuis le navigateur (lien « 📖 API » dans la barre du haut).
- **Liste des trajets paginée** (6 par page) et **connexion par email et mot de passe** dans la page web, avec le choix **passager** (je cherche un covoiturage) ou **conducteur** (j'ai une voiture).
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
powershell -ExecutionPolicy Bypass -File .\start.ps1          # démarre (connexion obligatoire)
powershell -ExecutionPolicy Bypass -File .\start.ps1 -Reset   # repart de données neuves
powershell -ExecutionPolicy Bypass -File .\start.ps1 -Open    # API ouverte, sans connexion (pour Postman)
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

### Connexion (JWT), rôles passager / conducteur

**Par défaut, `start.ps1` active la connexion** (`AUTH_ENABLED=true`) : la page web demande un email et un mot de passe,
et l'API exige un jeton JWT pour tout, sauf `POST /users` (inscription), `POST /auth/login` et `GET /events` (flux en direct).

**Comptes de démonstration** (mot de passe : `blasa2026`) :

| Email | Nom | Rôle |
|---|---|---|
| `sarra@mail.tn`, `ahmed@mail.tn`, `meriem@mail.tn` | Sarra, Ahmed, Meriem | conducteur |
| `yassine@mail.tn`, `nour@mail.tn` | Yassine, Nour | passager |

**Inscription** : le mot de passe (8 caractères minimum, stocké haché en PBKDF2, jamais renvoyé) et le **rôle** sont obligatoires.

- `PASSENGER` : « je cherche un covoiturage ». Peut réserver et noter ; ne peut pas proposer de trajet.
- `DRIVER` : « j'ai une voiture et je cherche des passagers ». Peut proposer et gérer des trajets ; ne réserve pas.

L'email doit être unique (`409` sinon). Les règles sont vérifiées **par l'API** (`403`), pas seulement par la page.

```powershell
POST /api/users        {"name":"Lina","email":"lina@mail.tn","city":"Sfax","password":"motdepasse123","role":"PASSENGER"}
POST /api/auth/login   {"email":"lina@mail.tn","password":"motdepasse123"}   ->  {"token":"...","userId":6,"role":"PASSENGER",...}
# puis, sur chaque appel :   Authorization: Bearer <jeton>
```

Règles de l'API avec connexion : jeton absent ou invalide `401` ; modifier un compte, un trajet ou une réservation qui n'est pas le sien,
ou agir avec le mauvais rôle `403` ; les emails des autres utilisateurs ne sont plus renvoyés. Le jeton dure 1 h (`JWT_TTL_SECONDS`) ;
`JWT_SECRET` (16 caractères minimum) fixe la clé de signature, sinon une clé aléatoire est créée à chaque démarrage.
Un compte créé avant l'ajout des rôles (sans rôle) garde tous les droits.

**API ouverte (sans connexion)** : `.\start.ps1 -Open`. C'est le mode à utiliser avec la **collection Postman** : le mot de passe et le
rôle y sont facultatifs et aucun jeton n'est exigé, comme dans la première version de l'API.

> Après la mise à jour, lancer **`.\start.ps1 -Reset`** une fois : il recharge les données de démonstration avec les mots de passe et les rôles.
> (`start.ps1` le fait aussi tout seul si les comptes de démo n'ont pas de mot de passe.)

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
- **Postman** : lancer l'API avec `.\start.ps1 -Open`, importer `postman/blasa.postman_collection.json`, puis *Run collection* : 24 requêtes, chacune vérifie son code de réponse.

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
