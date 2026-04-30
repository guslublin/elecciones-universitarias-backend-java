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
```

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

## Registro de listas electorales y candidatos

El sistema permite registrar listas electorales completas dentro de una elección universitaria.

Endpoint implementado:

- `POST /api/v1/elections/{id}/lists`: registra una lista con sus candidatos. Requiere rol `ADMIN`.

Regla principal:

Una lista debe incluir exactamente un candidato por cada cargo definido en la elección.

Ejemplo: si una elección tiene los cargos `Rector`, `Decano de Ingeniería` y `Decano de Ciencias`, cada lista debe registrar exactamente tres candidatos, uno asociado a cada cargo.

Validaciones implementadas:

- La elección debe existir.
- La elección debe estar en estado `DRAFT`.
- No se pueden registrar listas en elecciones `ACTIVE` o `CLOSED`.
- No se permiten listas duplicadas dentro de una misma elección.
- No pueden faltar cargos.
- No puede haber dos candidatos para el mismo cargo dentro de una lista.
- No se permiten candidatos repetidos dentro de una misma lista.
- Las entidades JPA no se exponen directamente; se usan DTOs para entrada y salida.

Ejemplo:

```json

{
  "name": "Lista A",
  "description": "Unidad Universitaria",
  "candidates": [
    {
      "positionId": "3791e5d0-ff9f-4f1e-9185-2607cb936a0a",
      "fullName": "Juan García",
      "career": "Derecho",
      "proposal": "Modernización institucional"
    },
    {
      "positionId": "421124b9-19dc-4bc9-bb00-4a23e6e1e630",
      "fullName": "Pedro López",
      "career": "Ingeniería",
      "proposal": "Fortalecimiento de laboratorios"
    },
    {
      "positionId": "6fb5859a-b7e6-48e1-97a7-2c5b8d59a3f3",
      "fullName": "Carlos Ruiz",
      "career": "Ciencias",
      "proposal": "Impulso a la investigación"
    }
  ]
}
```

--- 

## Votación segura

El sistema permite que usuarios con rol `VOTER` emitan un voto seleccionando una lista completa dentro de una elección activa.

Endpoints implementados:

- `POST /api/v1/elections/{id}/vote`: permite emitir un voto. Requiere rol `VOTER`.
- `GET /api/v1/elections/{id}/my-status`: permite consultar si el usuario autenticado ya votó en esa elección. Requiere rol `VOTER`.

Reglas implementadas:

- Solo usuarios con rol `VOTER` pueden votar.
- El voto se realiza por lista completa, no por candidato individual.
- La elección debe estar en estado `ACTIVE`.
- La lista seleccionada debe pertenecer a la elección indicada.
- Cada votante puede emitir exactamente un voto por elección.
- El sistema guarda un `voter_hash` anonimizado, nunca el email ni el ID real del usuario.
- El voto queda registrado con timestamp.
- Cada voto genera un evento de auditoría `VOTE_CAST`.

Protección concurrente:

- La tabla `votes` posee una restricción única sobre `(voter_hash, election_id)`.
- El flujo de voto se ejecuta dentro de una transacción.
- La elección se consulta con locking pesimista durante el registro del voto.
- Si dos solicitudes concurrentes intentan votar con el mismo usuario, solo una puede persistir; las demás retornan `409 Conflict`.

Ejemplo:

```json
{
  "listId": "uuid-de-la-lista"
}
```

---

## Resultados y estadísticas

El sistema permite consultar resultados electorales y estadísticas de participación para usuarios con rol `ADMIN` o `AUDITOR`.

Endpoints implementados:

- `GET /api/v1/elections/{id}/results`: devuelve votos por lista, porcentaje del total y lista ganadora.
- `GET /api/v1/elections/{id}/stats`: devuelve total de votantes habilitados, votos emitidos y porcentaje de participación.
- `GET /api/v1/elections/{id}/report`: devuelve el reporte final de resultados y participación. Solo está disponible para elecciones cerradas.

Reglas implementadas:

- Solo `ADMIN` y `AUDITOR` pueden consultar resultados.
- Usuarios `VOTER` no pueden consultar resultados mientras la elección está activa.
- Los resultados se calculan en tiempo real a partir de la tabla `votes`.
- El porcentaje se calcula sobre el total de votos emitidos.
- La lista ganadora se determina por mayor cantidad de votos.
- El resultado detalla qué candidato de la lista ganadora ocupa cada cargo.
- La participación se calcula tomando como votantes habilitados a los usuarios con rol `VOTER`.

Nota técnica:

Para evitar el problema N+1, las listas se cargan con sus candidatos y cargos mediante `@EntityGraph`.

---

## Auditoría persistente

El sistema registra acciones críticas en la tabla `audit_logs`.

Eventos auditados actualmente:

- `LOGIN_SUCCESS`
- `LOGIN_FAILED`
- `ELECTION_CREATED`
- `ELECTION_CLOSED`
- `VOTE_CAST`

Endpoint implementado:

- `GET /api/v1/audit/logs`: consulta paginada del log general de auditoría. Requiere rol `ADMIN`.

Características:

- Los logs se guardan en base de datos PostgreSQL.
- Cada evento contiene actor, acción, entidad afectada, detalle JSON y timestamp.
- Los votos se registran con actor anonimizado, por ejemplo `anonymous:a3f2b1...`.
- La tabla `audit_logs` fue diseñada como append-only mediante triggers de base de datos.

---

## Verificación de firma HMAC

El sistema permite verificar la integridad de un reporte de auditoría exportado.

Endpoint implementado:

- `POST /api/v1/audit/verify`: verifica si el payload recibido coincide con la firma HMAC enviada. Requiere rol `ADMIN` o `AUDITOR`.

Funcionamiento:

- El endpoint recibe el mismo JSON generado por `GET /api/v1/elections/{id}/audit`.
- No consulta la base de datos.
- Recalcula la firma HMAC-SHA256 sobre el campo `payload`.
- El payload se serializa de forma canónica con claves ordenadas.
- Si el JSON fue alterado, aunque sea en un solo carácter, la verificación devuelve `valid: false`.

Ejemplo de respuesta válida:

```json
{
  "valid": true,
  "message": "La firma HMAC es válida. El payload no fue alterado."
}
```

---


## Scheduler automático

Cada 60 segundos el sistema revisa elecciones ACTIVE vencidas.

Si end_date ya pasó:

- se cierran automáticamente
- se registra auditoría
- se ejecuta proceso async post cierre

## Async Thread Pool

corePoolSize: 4
maxPoolSize: 8
queueCapacity: 100

---

## Pruebas de integración

El proyecto incluye pruebas críticas de integración con PostgreSQL y Redis reales mediante Testcontainers.

Casos cubiertos:

- Rate limiting de login: el intento 6 devuelve `429 Too Many Requests`.
- Refresh token en blacklist: luego del logout, el refresh devuelve `401 Unauthorized`.
- Filtro JWT: token válido, token ausente y token inválido.
- Voto duplicado: el segundo voto del mismo usuario en la misma elección devuelve `409 Conflict`.
- Voto en elección cerrada: devuelve `409 Conflict`.
- Flujo completo: crear elección, agregar cargos, registrar lista, activar, votar y consultar resultados.
- Verificación HMAC: si se altera un carácter del payload exportado, `/api/v1/audit/verify` devuelve `valid: false`.
- Concurrencia: 500 requests simultáneos de voto del mismo usuario resultan en exactamente 1 voto válido.

Ejecutar tests:

```bash
./mvnw clean test
```

---