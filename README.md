# BookMania — Backend

A full stack Library Management System built with **Spring Boot** and **React**.

---

## Description

BookMania is a RESTful API for managing a digital library. It handles books, users, loans, fines, and reservations through clear business rules and a secure JWT-based authentication system.

---

## Tech Stack

**Backend**
- Java 21
- Spring Boot 3.2
- Spring Security + JWT (JJWT 0.12.3)
- Spring Data JPA (Hibernate)
- PostgreSQL
- Maven

**Frontend** *(in progress)*
- React
- REST API integration

---

## Features

### Core Modules

- **Authentication** — Register, login and JWT with `ADMIN` and `USER` roles
- **Catalogue** — Full CRUD for books and categories including cover images
- **Loans** — Issue, extend and return books with strict business rules
- **Fines** — Automatic time-based penalty for overdue returns
- **Reservations** — Waiting queue with a maximum of 3 people per book

### Business Rules

- Loan duration: **21 days**
- Maximum **3 extensions** of 10 days each, calculated from the current due date
- Overdue penalty: **7 base days + 2 days per each day overdue**
- Penalties accumulate if multiple fines are active
- Reservation queue: maximum **3 people** per book
- Users with an active penalty cannot make reservations or borrow books
- When a book is returned, the first person in the queue is automatically notified

---

## API Endpoints

### Authentication
| Method | Endpoint | Access | Description |
|--------|----------|--------|-------------|
| POST | `/api/auth/register` | Public | Register a new user |
| POST | `/api/auth/login` | Public | Login — returns JWT token |
| PUT | `/api/users/me/password` | Authenticated | Change the current user's password |

### Books
| Method | Endpoint | Access | Description |
|--------|----------|--------|-------------|
| GET | `/api/books` | Public | List books with filters |
| GET | `/api/books/{id}` | Public | Get book details |
| POST | `/api/books` | ADMIN | Create a book |
| PUT | `/api/books/{id}` | ADMIN | Update a book |
| DELETE | `/api/books/{id}` | ADMIN | Delete a book |

### Categories
| Method | Endpoint | Access | Description |
|--------|----------|--------|-------------|
| GET | `/api/categories` | Public | List categories |
| POST | `/api/categories` | ADMIN | Create a category |

### Loans
| Method | Endpoint | Access | Description |
|--------|----------|--------|-------------|
| POST | `/api/loans` | USER | Request a loan |
| GET | `/api/loans/my` | USER | My loan history |
| PUT | `/api/loans/{id}/extend` | USER | Extend a loan |
| PUT | `/api/loans/{id}/return` | USER | Return a book |

### Fines
| Method | Endpoint | Access | Description |
|--------|----------|--------|-------------|
| GET | `/api/fines/my` | USER | My penalties |

### Reservations
| Method | Endpoint | Access | Description |
|--------|----------|--------|-------------|
| POST | `/api/reservations` | USER | Create a reservation |
| DELETE | `/api/reservations/{id}` | USER | Cancel a reservation |
| GET | `/api/reservations/my` | USER | My reservation history |

---

## Project Structure

```
src/main/java/com/bookmania/bookmania/
├── Configuration/       # SecurityConfig, JwtProperties
├── Controller/          # REST controllers
├── Dtos/                # Request and Response DTOs
├── Entity/              # JPA entities
├── Enums/               # LoanStatus, ReservationStatus, Role
├── Exception/           # GlobalExceptionHandler + custom exceptions
├── Repository/          # Spring Data JPA repositories
├── Scheduler/           # LoanScheduler — marks loans as OVERDUE daily
├── Security/            # JwtUtil, JwtAuthFilter, UserDetailsServiceImpl
└── Services/            # Business logic
```

---

## Local Setup

### Prerequisites

- Java 21+
- PostgreSQL
- Maven
- Node.js and npm (for the frontend)

### Steps

1. Clone the repository

```bash
git clone https://github.com/costanna/BookMania_Backend.git
cd BookMania_Backend
```

2. Create the PostgreSQL database

```sql
CREATE DATABASE bookmania_db;
```

3. `application.properties` reads everything from environment variables with local-friendly
   defaults (see the table below), so it works out of the box against a local Postgres on
   `localhost:5432` with user `postgres` / password `postgres`. To use different values,
   export the env vars before running, or drop a git-ignored `application-local.properties`
   next to it and run with `--spring.profiles.active=local`.

4. Run the application

```bash
mvn spring-boot:run
```

The API will be available at `http://localhost:8080`

---

## Deploying to Railway

The backend ships with a `Dockerfile` and `railway.json`, so Railway builds and runs it as a
container — no extra buildpack configuration needed.

1. Push this repo to GitHub and create a new Railway project from it (or run `railway up` from
   the CLI).
2. Add a **PostgreSQL** plugin to the project.
3. In the backend service's **Variables** tab, set:

   | Variable | Value |
   |---|---|
   | `SPRING_DATASOURCE_URL` | `jdbc:postgresql://${{Postgres.PGHOST}}:${{Postgres.PGPORT}}/${{Postgres.PGDATABASE}}` |
   | `SPRING_DATASOURCE_USERNAME` | `${{Postgres.PGUSER}}` |
   | `SPRING_DATASOURCE_PASSWORD` | `${{Postgres.PGPASSWORD}}` |
   | `JWT_SECRET_KEY` | a fresh, private Base64 secret (e.g. `openssl rand -base64 48`) — **do not reuse the dev default in `application.properties`** |
   | `CORS_ALLOWED_ORIGINS` | your deployed frontend URL, e.g. `https://bookmania.up.railway.app` |

   (`${{Postgres.PGHOST}}` etc. are Railway variable references to the Postgres plugin — pick
   them from the autocomplete when adding a new variable.) Railway sets `PORT` automatically;
   the app already listens on it via `server.port=${PORT:8080}`.
4. Deploy. Railway builds the `Dockerfile` and polls `/actuator/health` (configured in
   `railway.json`) to know when the container is ready.
5. Once it's up, update the frontend's `VITE_API_URL` to the Railway backend URL.

---

## Frontend Integration

The React frontend is fully integrated with this backend.

**Repository:** https://github.com/costanna/BookMania_Frontend

### Setup
```bash
git clone https://github.com/costanna/BookMania_Frontend.git
cd BookMania_Frontend
npm install
echo "VITE_API_URL=http://localhost:8080" > .env
npm run dev
```

The app will be available at `http://localhost:5173`

### How it connects

The frontend uses Axios with a JWT interceptor that automatically attaches the token to every authenticated request:
```javascript
const axiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_URL,
});

axiosInstance.interceptors.request.use((config) => {
  const token = localStorage.getItem("token");
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});
```

### Frontend Tech Stack
- React + Vite
- Tailwind CSS v3
- React Router v6
- Axios with JWT interceptors
- Context API

---

## Author

**Anna Costa**
[LinkedIn](https://www.linkedin.com/in/annahico/)
[GitHub](https://github.com/costanna)

---
---

# BookMania — Backend

Sistema de Gestión de Bibliotecas full stack desarrollado con **Spring Boot** y **React**.

---

## Descripción

BookMania es una API RESTful para la gestión de una biblioteca digital. Permite gestionar libros, usuarios, préstamos, multas y reservas mediante reglas de negocio claras y un sistema de autenticación seguro basado en JWT.

---

## Stack tecnológico

**Backend**
- Java 21
- Spring Boot 3.2
- Spring Security + JWT (JJWT 0.12.3)
- Spring Data JPA (Hibernate)
- PostgreSQL
- Maven

**Frontend** *(en desarrollo)*
- React
- Integración con API REST

---

## Características

### Módulos principales

- **Autenticación** — Registro, login y JWT con roles `ADMIN` y `USER`
- **Catálogo** — CRUD completo de libros y categorías con imágenes de portada
- **Préstamos** — Emisión, prórroga y devolución con reglas de negocio estrictas
- **Multas** — Penalización temporal automática por retrasos
- **Reservas** — Cola de espera con máximo 3 personas por libro

### Reglas de negocio

- Duración del préstamo: **21 días**
- Máximo **3 prórrogas** de 10 días cada una calculadas desde la fecha de vencimiento
- Penalización por retraso: **7 días base + 2 días por cada día de retraso**
- Las penalizaciones se acumulan si hay varias multas activas
- Cola de reservas con máximo **3 personas** por libro
- Los usuarios con penalización activa no pueden hacer reservas ni préstamos
- Al devolver un libro, se notifica automáticamente al primero de la cola

---

## Endpoints de la API

### Autenticación
| Método | Endpoint | Acceso | Descripción |
|--------|----------|--------|-------------|
| POST | `/api/auth/register` | Público | Registro de usuario |
| POST | `/api/auth/login` | Público | Login — devuelve JWT |
| PUT | `/api/users/me/password` | Autenticado | Cambiar la contraseña del usuario actual |

### Libros
| Método | Endpoint | Acceso | Descripción |
|--------|----------|--------|-------------|
| GET | `/api/books` | Público | Listar libros con filtros |
| GET | `/api/books/{id}` | Público | Detalle de un libro |
| POST | `/api/books` | ADMIN | Crear libro |
| PUT | `/api/books/{id}` | ADMIN | Actualizar libro |
| DELETE | `/api/books/{id}` | ADMIN | Eliminar libro |

### Categorías
| Método | Endpoint | Acceso | Descripción |
|--------|----------|--------|-------------|
| GET | `/api/categories` | Público | Listar categorías |
| POST | `/api/categories` | ADMIN | Crear categoría |

### Préstamos
| Método | Endpoint | Acceso | Descripción |
|--------|----------|--------|-------------|
| POST | `/api/loans` | USER | Solicitar préstamo |
| GET | `/api/loans/my` | USER | Mis préstamos |
| PUT | `/api/loans/{id}/extend` | USER | Prorrogar préstamo |
| PUT | `/api/loans/{id}/return` | USER | Devolver libro |

### Multas
| Método | Endpoint | Acceso | Descripción |
|--------|----------|--------|-------------|
| GET | `/api/fines/my` | USER | Mis penalizaciones |

### Reservas
| Método | Endpoint | Acceso | Descripción |
|--------|----------|--------|-------------|
| POST | `/api/reservations` | USER | Crear reserva |
| DELETE | `/api/reservations/{id}` | USER | Cancelar reserva |
| GET | `/api/reservations/my` | USER | Mis reservas |

---

## Arquitectura

```
src/main/java/com/bookmania/bookmania/
├── Configuration/       # SecurityConfig, JwtProperties
├── Controller/          # Controladores REST
├── Dtos/                # Request y Response DTOs
├── Entity/              # Entidades JPA
├── Enums/               # LoanStatus, ReservationStatus, Role
├── Exception/           # GlobalExceptionHandler + excepciones custom
├── Repository/          # Repositorios Spring Data JPA
├── Scheduler/           # LoanScheduler — marca préstamos OVERDUE diariamente
├── Security/            # JwtUtil, JwtAuthFilter, UserDetailsServiceImpl
└── Services/            # Lógica de negocio
```

---

## Instalación local

### Prerrequisitos

- Java 21+
- PostgreSQL
- Maven
- Node.js y npm (para el frontend)

### Pasos

1. Clona el repositorio

```bash
git clone https://github.com/costanna/BookMania_Backend.git
cd BookMania_Backend
```

2. Crea la base de datos en PostgreSQL

```sql
CREATE DATABASE bookmania_db;
```

3. `application.properties` lee todo desde variables de entorno con valores por defecto
   pensados para desarrollo local (ver tabla más abajo), así que funciona tal cual contra un
   Postgres local en `localhost:5432` con usuario `postgres` / contraseña `postgres`. Para usar
   otros valores, exporta las variables antes de arrancar, o crea un `application-local.properties`
   (ignorado por git) y arranca con `--spring.profiles.active=local`.

4. Arranca la aplicación

```bash
mvn spring-boot:run
```

La API estará disponible en `http://localhost:8080`

---

## Despliegue en Railway

El backend incluye un `Dockerfile` y un `railway.json`, así que Railway lo construye y ejecuta
como contenedor sin configuración adicional de buildpack.

1. Sube este repo a GitHub y crea un proyecto nuevo en Railway a partir de él (o usa
   `railway up` desde la CLI).
2. Añade un plugin de **PostgreSQL** al proyecto.
3. En la pestaña **Variables** del servicio del backend, configura:

   | Variable | Valor |
   |---|---|
   | `SPRING_DATASOURCE_URL` | `jdbc:postgresql://${{Postgres.PGHOST}}:${{Postgres.PGPORT}}/${{Postgres.PGDATABASE}}` |
   | `SPRING_DATASOURCE_USERNAME` | `${{Postgres.PGUSER}}` |
   | `SPRING_DATASOURCE_PASSWORD` | `${{Postgres.PGPASSWORD}}` |
   | `JWT_SECRET_KEY` | un secreto Base64 nuevo y privado (p. ej. `openssl rand -base64 48`) — **no reutilices el valor por defecto de desarrollo de `application.properties`** |
   | `CORS_ALLOWED_ORIGINS` | la URL del frontend desplegado, p. ej. `https://bookmania.up.railway.app` |

   (`${{Postgres.PGHOST}}`, etc. son referencias a variables del plugin de Postgres —
   aparecen en el autocompletado al crear una variable nueva.) Railway define `PORT`
   automáticamente; la app ya escucha en ese puerto vía `server.port=${PORT:8080}`.
4. Despliega. Railway construye el `Dockerfile` y consulta `/actuator/health` (configurado en
   `railway.json`) para saber cuándo el contenedor está listo.
5. Una vez arriba, actualiza `VITE_API_URL` en el frontend con la URL del backend en Railway.

---

## Integración con el Frontend

El frontend en React está completamente integrado con este backend.

**Repositorio:** https://github.com/costanna/BookMania_Frontend

### Instalación
```bash
git clone https://github.com/costanna/BookMania_Frontend.git
cd BookMania_Frontend
npm install
echo "VITE_API_URL=http://localhost:8080" > .env
npm run dev
```

La app estará disponible en `http://localhost:5173`

### Cómo se conecta

El frontend usa Axios con un interceptor JWT que adjunta automáticamente el token en cada petición autenticada:
```javascript
const axiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_URL,
});

axiosInstance.interceptors.request.use((config) => {
  const token = localStorage.getItem("token");
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});
```

### Stack del Frontend
- React + Vite
- Tailwind CSS v3
- React Router v6
- Axios con interceptores JWT
- Context API

---

## Autora

**Anna Costa**
[LinkedIn](https://www.linkedin.com/in/annahico/)
[GitHub](https://github.com/costanna)