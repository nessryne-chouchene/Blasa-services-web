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
| GET | `/stats/top-drivers` | meilleurs conducteurs | 200 | 400 |
| GET | `/stats/summary` | compteurs globaux | 200 | |
| GET | `/events` | flux en direct (Server-Sent Events) | 200 | |

JAX-RS n'a pas d'annotation `@PATCH` : le projet en définit une (`PATCH.java`) avec `@HttpMethod("PATCH")`.

## Tester

- **Application** : `http://localhost:8080/`. Le bouton « Mode technique » ouvre le panneau des requêtes, avec un
  **scénario de démonstration** en 16 appels (POST, GET, PUT, PATCH, DELETE ; codes 201, 200, 204, 404, 409).
- **Postman** : importer `postman/blasa.postman_collection.json`, puis *Run collection* : 24 requêtes, chacune vérifie son code de réponse.

## Structure

```
pom.xml                        dépendances Maven
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
