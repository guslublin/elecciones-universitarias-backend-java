# elecciones-universitarias-backend-java

Prueba técnica desarrollada en Java 21 con Spring Boot 3.x para la gestión de elecciones universitarias.

## Tecnologías principales

- Java 21
- Spring Boot 3.x
- Spring Security
- Spring Data JPA
- PostgreSQL
- Redis
- Flyway
- Docker Compose
- OpenAPI / Swagger
- JUnit 5

## Modelo de datos inicial

El sistema utiliza PostgreSQL como base de datos relacional principal y Flyway para gestionar migraciones versionadas.

Tablas principales:

- `users`: almacena usuarios registrados con email único y contraseña hasheada.
- `user_roles`: almacena los roles asociados a cada usuario (`ADMIN`, `VOTER`, `AUDITOR`).
- `elections`: representa elecciones universitarias con estado `DRAFT`, `ACTIVE` o `CLOSED`.
- `positions`: representa los cargos definidos dentro de una elección.
- `election_lists`: representa las listas electorales que compiten en una elección.
- `candidates`: representa los candidatos de cada lista, asociados a un cargo específico.
- `votes`: registra votos anónimos mediante `voter_hash`, con restricción única por elección.
- `audit_logs`: registra eventos críticos del sistema de forma append-only.

Restricciones relevantes:

- Una lista no puede tener más de un candidato para el mismo cargo.
- Un votante anonimizado no puede votar más de una vez por elección.
- El cierre concurrente de elecciones se protege mediante campo `version` para locking optimista.
- Los logs de auditoría tienen triggers para impedir actualización o eliminación.

## Autenticación JWT

El sistema implementa autenticación **stateless** utilizando **JSON Web Tokens (JWT)** con Spring Security, permitiendo proteger endpoints sin mantener sesiones en servidor.

### Endpoints disponibles

- `POST /api/v1/auth/register`  
  Registra nuevos usuarios con email único y contraseña protegida mediante **BCrypt**.

- `POST /api/v1/auth/login`  
  Autentica credenciales válidas y retorna:

  - Access Token
  - Refresh Token

- `POST /api/v1/auth/refresh`  
  Permite renovar el access token utilizando un refresh token válido.

- `POST /api/v1/auth/logout`  
  Endpoint preparado para invalidar refresh tokens mediante blacklist en Redis.

---

### Características implementadas

- Autenticación totalmente **stateless**
- Access Token con duración de **15 minutos**
- Refresh Token con duración de **7 días**
- Contraseñas hasheadas con **BCrypt**
- Roles soportados:

  - `ADMIN`
  - `VOTER`
  - `AUDITOR`

- Protección de rutas mediante filtro JWT explícito de Spring Security
- Validación automática de expiración de tokens
- Configuración CORS centralizada
- Respuestas diferenciadas:

  - `401 Unauthorized` → usuario no autenticado
  - `403 Forbidden` → usuario sin permisos

- Manejo consistente de errores usando `ProblemDetail`

---

