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

## Redis: rate limiting de login

El endpoint `POST /api/v1/auth/login` implementa rate limiting utilizando Redis.

Regla aplicada:

- Máximo 5 intentos de login por IP por minuto.
- El intento número 6 dentro de la misma ventana retorna `429 Too Many Requests`.
- El contador se almacena en Redis con TTL de 1 minuto.

Ejemplo de comportamiento:

```text
Intento 1 → permitido
Intento 2 → permitido
Intento 3 → permitido
Intento 4 → permitido
Intento 5 → permitido
Intento 6 → bloqueado con 429

---

## Redis: Protección de autenticación

El sistema utiliza Redis para dos funciones críticas:

### 1. Blacklist de refresh tokens

Al cerrar sesión (`logout`), el refresh token queda invalidado en Redis con TTL igual a su tiempo restante.

### 2. Rate limiting de login

El endpoint:

POST /api/v1/auth/login

permite máximo:

- 5 intentos por IP por minuto

El intento número 6 retorna:

429 Too Many Requests

Luego de 60 segundos el contador expira automáticamente.

### Disponibilidad

Si Redis no está disponible:

- el sistema sigue funcionando
- login continúa normalmente (fail-open)
- se prioriza disponibilidad sobre restricción temporal



---

## Gestión de elecciones

El módulo de elecciones permite a usuarios con rol `ADMIN` crear elecciones universitarias y a usuarios autenticados consultar elecciones existentes.

Endpoints implementados:

- `POST /api/v1/elections`: crea una nueva elección. Requiere rol `ADMIN`.
- `GET /api/v1/elections`: lista elecciones registradas. Requiere autenticación.
- `GET /api/v1/elections/{id}`: obtiene el detalle básico de una elección. Requiere autenticación.

Reglas implementadas:

- Las entidades JPA no se exponen directamente en las respuestas.
- Se utilizan DTOs para entrada y salida de datos.
- La fecha de inicio debe ser futura.
- La fecha de cierre debe ser posterior a la fecha de inicio.
- Toda elección nueva se crea inicialmente en estado `DRAFT`.

Ejemplo de creación:

```json
{
  "title": "Autoridades Universitarias 2026",
  "description": "Elección de autoridades estudiantiles de la universidad.",
  "startDate": "2026-05-10T08:00:00",
  "endDate": "2026-05-10T18:00:00"
}

---

## Cargos de elección

El sistema permite definir los cargos que serán cubiertos dentro de una elección universitaria.

Endpoint implementado:

- `POST /api/v1/elections/{id}/positions`: agrega un cargo a una elección existente. Requiere rol `ADMIN`.

Reglas implementadas:

- Solo usuarios con rol `ADMIN` pueden agregar cargos.
- La elección debe existir.
- Solo se pueden agregar cargos mientras la elección esté en estado `DRAFT`.
- No se pueden agregar cargos si la elección está `ACTIVE` o `CLOSED`.
- No se permiten cargos duplicados dentro de una misma elección.
- Los datos se reciben y retornan mediante DTOs, sin exponer entidades JPA.

Ejemplo:

```json
{
  "name": "Rector"
}

