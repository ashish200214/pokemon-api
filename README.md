# Pokemon Pokedex Search
<img width="960" height="504" alt="image" src="https://github.com/user-attachments/assets/ca154082-b772-41a5-8c97-01847a555400" />
<img width="960" height="504" alt="image" src="https://github.com/user-attachments/assets/0fa267e6-cdb8-43e0-966f-874c51dabe82" />

A local full-stack Pokedex application built for the coding challenge. The frontend lets users search Pokemon by name and renders rich details from a backend REST API. The backend fetches from PokeAPI, caches vendor responses, and serves a stable local API to the UI.

## Tech Stack

- Backend: Java 21, Spring Boot 4, Maven
- Frontend: React, Vite, CSS
- Vendor API: https://pokeapi.co/api/v2

## Features

- Search for a Pokemon by name.
- Display official artwork, type badges, abilities, base stats, moves, species description, habitat, generation, growth rate, capture rate, happiness, height, weight, and base experience.
- Backend REST API wraps PokeAPI instead of calling PokeAPI directly from the browser.
- In-memory cache with:
  - TTL expiry
  - maximum cache entries
  - access-order eviction for repeat-query performance
- Local CORS support for the Vite development server.

## Project Structure

```text
PokemonProjectAPI/
  backend/pokemon/          Spring Boot REST API
  frontend/pokemon-frontend React + Vite UI
```

## Backend API

Base URL:

```text
http://localhost:8080/api
```

Endpoints:

```text
GET /pokemon/{name}
GET /pokemon/cache
```

Example:

```text
http://localhost:8080/api/pokemon/pikachu
```

Cache configuration is in `backend/pokemon/src/main/resources/application.properties`:

```properties
pokemon.cache.max-entries=100
pokemon.cache.ttl-seconds=600
```

## Run Locally

### 1. Start Backend

```bash
cd backend/pokemon
./mvnw spring-boot:run
```

On Windows:

```bash
cd backend/pokemon
mvnw.cmd spring-boot:run
```

The backend runs on:

```text
http://localhost:8080
```

### 2. Start Frontend

Open a second terminal:

```bash
cd frontend/pokemon-frontend
npm install
npm run dev
```

The frontend runs on:

```text
http://localhost:5173
```

## Verification

Backend:

```bash
cd backend/pokemon
mvnw.cmd test
```

Frontend:

```bash
cd frontend/pokemon-frontend
npm run lint
npm run build
```

## Submission Notes

Create a public GitHub repository containing this project, then reply to the original coding challenge email without changing the subject. Include the public GitHub repository link in the email body.
