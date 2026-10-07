# parqueando-saas (backend)

API REST multiempresa para la gestion de parqueaderos: entradas y salidas de vehiculos, tarifas,
mensualidades, reportes, licencias anuales y sincronizacion con las instalaciones de escritorio.

- Java 17, Spring Boot 3.4, Spring Security (JWT), JPA/Hibernate, PostgreSQL
- Arquitectura hexagonal: `domain` (modelo y puertos), `application` (servicios y DTO),
  `infrastructure` (REST, persistencia, configuracion)
- Frontend: repositorio `parqueando-saas-frontend` (Angular)

## Ejecutar en local

Requiere Java 17 y un PostgreSQL accesible.

```bash
export APP_JWT_SECRET="<minimo 32 caracteres>"
export LICENSE_HMAC_SECRET="$(openssl rand -base64 32)"
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/parqueadero
export SPRING_DATASOURCE_USERNAME=postgres
export SPRING_DATASOURCE_PASSWORD=postgres
./mvnw spring-boot:run
```

La aplicacion **no arranca** si faltan `APP_JWT_SECRET` o `LICENSE_HMAC_SECRET`, o si usan valores
inseguros (el secreto de licencias por defecto que tuvo el repositorio esta rechazado a proposito).

## Variables de entorno

| Variable | Obligatoria | Descripcion |
|---|---|---|
| `APP_JWT_SECRET` | si | Secreto de firma del JWT (>= 32 caracteres) |
| `LICENSE_HMAC_SECRET` | si | Secreto HMAC de los codigos de licencia, en Base64 (>= 32 bytes). Cambiarlo invalida las licencias pendientes |
| `SPRING_DATASOURCE_URL` / `_USERNAME` / `_PASSWORD` | si | Conexion a PostgreSQL |
| `APP_CORS_ALLOWED_ORIGINS` | recomendada | Origenes permitidos, separados por coma. Admite patrones (`https://*.vercel.app`). Por defecto: localhost y `*.vercel.app` |
| `APP_JWT_EXPIRATION_MILLIS` | no | Vida del access token (por defecto 900000 = 15 min) |
| `SWAGGER_ENABLED` | no | `true` para exponer Swagger UI y api-docs (apagado por defecto) |
| `APP_SYNC_MAX_PAYLOAD_BYTES` | no | Tamano maximo del cuerpo de `/api/v1/sync` (por defecto 10 MB) |
| `APP_SYNC_BASE_URL`, `APP_SYNC_RETENCION_DIAS`, `APP_SYNC_INTERVALO_SEGUNDOS` | no | Sincronizacion con escritorio |
| `SPRING_JPA_HIBERNATE_DDL_AUTO` | no | `update` por defecto; ver "Pendientes" |

## Seguridad

- Autenticacion por JWT (access) + refresh token opaco rotativo con deteccion de reuso.
- Respuestas 401 (sesion invalida) y 403 (sin permiso) en JSON; reglas por URL y validacion de rol en los servicios.
- El JWT solo es valido si el usuario sigue existiendo.
- Limite de peticiones por IP en login, refresh, licencias y sync (429), y bloqueo de un usuario
  durante 15 min tras 5 intentos fallidos. El estado esta en memoria: con mas de una instancia
  hay que moverlo a un almacen compartido (por ejemplo Redis).
- Entradas y salidas de vehiculos toman un bloqueo de fila sobre la sede para no desajustar el cupo.

## Pruebas

```bash
./mvnw test
```

`ParqueaderoApplicationTests` levanta el contexto completo y necesita PostgreSQL y las variables
anteriores. En CI se usa un servicio PostgreSQL (`.github/workflows/ci.yml`).

## Pendientes conocidos

- Migraciones con Flyway y `ddl-auto=validate` (hoy el esquema lo crea Hibernate con `update`),
  junto con un indice unico parcial de "una placa activa por sede".
- Paginacion de los reportes y exportaciones.
