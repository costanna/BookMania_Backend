# Despliegue del backend: Render (Blueprint) + Neon

| Pieza | Dónde | Plan |
|---|---|---|
| API Spring Boot (Docker) | Render, servicio web `bookmania-backend` | Free: se duerme tras 15 min sin peticiones |
| PostgreSQL | Neon, proyecto `bookmania` | Free: 0.25 CU fijo, scale to zero a los 5 min |
| Frontend | Vercel (ver `DEPLOY.md` del repo del frontend) | Hobby |

Toda la configuración del servicio de Render está en [`render.yaml`](render.yaml) (el
**Blueprint**). Render lo lee al crear el Blueprint y, desde entonces, aplica cada cambio de ese
fichero cuando se hace push a `main`. Los secretos están marcados con `sync: false`: se rellenan
en el panel una vez y Render no los pisa nunca.

La base de datos **no** está en el Blueprint a propósito: el Postgres gratuito de Render caduca a
los 30 días. Neon escala a cero y no caduca.

Orden: **1. Neon → 2. datos (opcional) → 3. Render → 4. Vercel → 5. CORS → 6. primer admin → 7. apagar Railway.**

---

## 1. Crear la base de datos en Neon

1. En [console.neon.tech](https://console.neon.tech) → **New project**:
   - Nombre: `bookmania`
   - Postgres: 17
   - Región: **AWS Europe Central 1 (Frankfurt)**, la misma que el servicio de Render.
2. **Compute fijo en 0.25 CU**: *Branches → main → Computes → Edit* → mínimo **0.25** y máximo
   **0.25**. Deja **Scale to zero** activado con el tiempo por defecto (5 min).
3. Cadena de conexión: botón **Connect** → desactiva **Connection pooling** (conexión directa,
   host sin `-pooler`) → copia la cadena:

   ```text
   postgresql://neondb_owner:XXXX@ep-xxxx-xxxx.eu-central-1.aws.neon.tech/neondb?sslmode=require
   ```

   Si termina en `&channel_binding=require`, quítalo: el driver JDBC no lo necesita.
   Esta cadena es el valor de `DATABASE_URL`. Guárdala en tu gestor de contraseñas, no en el repo.

> ¿Por qué la conexión directa? La app tiene un único proceso con su propio pool (HikariCP); el
> pooler de Neon (PgBouncer en modo transacción) no aporta nada aquí.

## 2. Datos (opcional)

**Empezar con la base vacía** es lo más sencillo: no hay que hacer nada. Hibernate crea las tablas
en Neon en el primer arranque (`JPA_DDL_AUTO=update`). Después hay que crear el primer
administrador (paso 6) y dar de alta categorías y libros desde el panel de administración.

**Copiar datos de una base existente** (p. ej. el antiguo Postgres de Railway) solo es posible si
esa base está encendida y accesible desde fuera (en Railway: *TCP Proxy* activado, que crea la
variable `DATABASE_PUBLIC_URL`). En ese caso, cópialos **antes** del primer arranque en Render,
para que Hibernate encuentre las tablas ya creadas.

Ejecuta esto en PowerShell **desde una carpeta fuera de los repos** (el volcado contiene datos
de usuarios y no debe acabar en git). Usa Docker para no tener que instalar el cliente de Postgres:

```powershell
# URL pública del Postgres de Railway: servicio Postgres → Variables → DATABASE_PUBLIC_URL
$RAILWAY_URL = "postgresql://postgres:XXXX@xxxx.proxy.rlwy.net:12345/railway"
# Cadena de Neon del paso 1
$NEON_URL = "postgresql://neondb_owner:XXXX@ep-xxxx.eu-central-1.aws.neon.tech/neondb?sslmode=require"

# 1) Volcado de Railway (formato custom)
docker run --rm -v "${PWD}:/backup" postgres:17 pg_dump --no-owner --no-acl -Fc -d $RAILWAY_URL -f /backup/bookmania.dump

# 2) Restaurar en Neon
docker run --rm -v "${PWD}:/backup" postgres:17 pg_restore --no-owner --no-acl -d $NEON_URL /backup/bookmania.dump

# 3) Comprobar
docker run --rm postgres:17 psql $NEON_URL -c "SELECT count(*) FROM books;" -c "SELECT count(*) FROM users;"
```

- Si `pg_dump` se queja de la versión del servidor, usa una imagen igual o más nueva que el
  Postgres de Railway (`postgres:18`, por ejemplo).
- Si el Postgres de Railway está parado, arráncalo antes desde su panel (*Restart*).
- Si ya habías arrancado el backend contra Neon y Hibernate creó las tablas vacías, repite el
  paso 2 añadiendo `--clean --if-exists` a `pg_restore`.
- Guarda `bookmania.dump` como copia de seguridad hasta que todo funcione en Render.

## 3. Crear el servicio en Render con el Blueprint

`render.yaml` tiene que estar en la rama `main` del repo antes de este paso.

1. [dashboard.render.com](https://dashboard.render.com) → **New → Blueprint**.
2. Conecta GitHub y elige el repo **BookMania_Backend**. Rama: `main`. Nombre del Blueprint:
   `BookMania`.
3. Render muestra el servicio `bookmania-backend` (Docker, Free, Frankfurt) y pide los valores
   de los secretos:

   | Variable | Valor |
   |---|---|
   | `DATABASE_URL` | la cadena de Neon del paso 1 |
   | `JWT_SECRET_KEY` | una clave nueva: `openssl rand -base64 48` (en Git Bash). No reutilices la de desarrollo de `application.properties` |
   | `CORS_ALLOWED_ORIGINS` | la URL del frontend en Vercel, p. ej. `https://bookmania.vercel.app`. Si aún no la tienes, pon esa y la corriges en el paso 5 |

   `JWT_EXPIRATION` (24 h) y `JPA_DDL_AUTO` (`update`) ya vienen en el Blueprint. `PORT` lo
   pone Render y la app lo lee con `server.port=${PORT:8080}`.
4. **Deploy Blueprint**. El primer build (Maven dentro de Docker) tarda unos minutos.
5. La URL pública aparece en el servicio, normalmente `https://bookmania-backend.onrender.com`
   (si el nombre está cogido, Render añade un sufijo).

Comprobación (PowerShell):

```powershell
curl.exe https://bookmania-backend.onrender.com/actuator/health
# {"status":"UP"}
curl.exe "https://bookmania-backend.onrender.com/api/books?page=0&size=1"
# JSON (con "content": [] si la base está vacía) → la conexión con Neon funciona
```

### Cosas que conviene saber

- **Arranque en frío:** con el plan Free el servicio se duerme tras 15 min sin peticiones y la
  primera petición tarda hasta ~1 min. El frontend ya muestra un aviso mientras tanto. Es
  intencionado: **no** añadas pings, workflows de GitHub Actions, UptimeRobot ni nada que lo
  mantenga despierto.
- **El health check no toca la BD** (`management.health.db.enabled=false`). Render lo llama cada
  pocos segundos mientras el servicio está despierto; si consultara la BD, Neon no se dormiría.
- **Pool sin pings:** `minimum-idle=0`, `idle-timeout=60s` y `keepalive-time=0` (HikariCP 6
  hace keepalive cada 2 min por defecto). Así Neon se suspende a los 5 min sin consultas.
- **`LoanScheduler`** (diario a las 00:05 UTC) solo se ejecuta si el servicio está despierto a
  esa hora; con el plan Free normalmente no lo estará.
- **Cambios:** lo que está en `render.yaml` se cambia en el fichero y se hace push a `main`. Los
  secretos se cambian en *Environment* del servicio (Render redespliega al guardar).
- **Rollback:** servicio → *Events/Deploys* → elige un deploy anterior → *Rollback*.

## 4. Frontend en Vercel

Sigue el `DEPLOY.md` del repo **BookMania_Frontend**: importar el repo en Vercel y configurar
`VITE_API_URL` con la URL de Render del paso 3.

## 5. Ajustar CORS

Cuando tengas la URL definitiva de Vercel, en Render → `bookmania-backend` → **Environment** →
`CORS_ALLOWED_ORIGINS` = esa URL exacta, con `https://` y sin barra final. Para varios orígenes,
sepáralos con comas sin espacios:

```text
https://bookmania.vercel.app
```

Guardar redespliega el servicio. Las URLs de preview de Vercel no están permitidas (y no hace
falta).

## 6. Crear el primer administrador

El registro público siempre crea usuarios con rol `USER`. Para tener un admin:

1. Regístrate desde la web de Vercel con el email que quieras usar como administradora.
2. En Neon → proyecto `bookmania` → **SQL Editor**:

   ```sql
   UPDATE users SET role = 'ADMIN' WHERE email = 'tu-email@ejemplo.com';
   ```

3. Cierra sesión y vuelve a entrar (el rol va dentro del token JWT).

## 7. Apagar Railway

Solo cuando el frontend de Vercel cargue los datos desde Render y el login funcione:

1. En Railway, proyecto **BookMania**: elimina los servicios `BookMania_Backend`,
   `BookMania_Frontend` y `Postgres` (o el proyecto entero). Mientras sigan conectados a GitHub,
   cada push a `main` los vuelve a construir y gasta crédito.
2. En este repo, borra la carpeta `.railway/` (IaC de Railway, ya no se usa) en una rama nueva.
3. Borra o guarda en un sitio seguro `bookmania.dump`.

---

## Solución de problemas

| Síntoma | Causa probable | Qué hacer |
|---|---|---|
| El deploy falla en *health check* | La app no arranca: suele ser `DATABASE_URL` mal copiada | Mira *Logs* en Render. Comprueba que la cadena empieza por `postgresql://` y lleva `?sslmode=require` |
| `password authentication failed` | Contraseña de Neon regenerada o mal pegada | Copia otra vez la cadena desde *Connect* en Neon y actualiza `DATABASE_URL` |
| El navegador dice `blocked by CORS policy` | `CORS_ALLOWED_ORIGINS` no coincide exactamente con la URL del frontend | Mismo esquema (`https://`), sin barra final, sin espacios |
| Login devuelve 500 / `WeakKeyException` | `JWT_SECRET_KEY` no es Base64 o es demasiado corta | Genera otra con `openssl rand -base64 48` |
| Catálogo vacío | Base nueva, o no se restauraron los datos | Normal si empezaste vacía: añade libros como admin. Si copiaste datos, repite el paso 2 con `--clean --if-exists` |
| Neon no se duerme nunca | Algo consulta la BD periódicamente | En Neon mira si el compute pasa a *Idle*. Busca pingers externos y comprueba que el health check sigue sin tocar la BD |
