## OBP API – Docker & Docker Compose Setup

This project uses Docker and Docker Compose to run the **OBP API** service with Maven and Jetty.

- Java 17 with reflection workaround  
- **Automatic database URL transformation** for seamless Docker development
- Connects to your local Postgres using `host.docker.internal`  
- Supports separate dev & prod setups

---

## How to use

> **Make sure you have Docker and Docker Compose installed.**
> **Navigate to the `development/docker` directory before running commands.**

```bash
cd development/docker
```

### Automatic Database Configuration 🚀

The Docker setup now **automatically transforms your database configuration** for Docker environments!

**What this means:**
- Set your database URL in props file with `localhost` (for local development)
- Docker automatically transforms `localhost` to `host.docker.internal` 
- **No need to change props files when switching between local and Docker!**

**Example:**
```properties
# In your obp-api/src/main/resources/props/default.props
db.url=jdbc:postgresql://localhost:5432/obp_mapped?user=obp&password=f
```

When you run with Docker, this automatically becomes:
```
jdbc:postgresql://host.docker.internal:5432/obp_mapped?user=obp&password=f
```

**Password changes are automatically reflected!** 
If you change your password in the props file, Docker will use the new password automatically.

---

### Build & run

Build the Docker image and run the container:

```bash
docker-compose up --build
```

The service will be available at [http://localhost:8080](http://localhost:8080).

---

## Development tips

For live code updates without rebuilding:

* Use the provided `docker-compose.override.yml` which mounts only:

  ```yaml
  volumes:
    - ../../obp-api:/app/obp-api
    - ../../obp-commons:/app/obp-commons
  ```
* This keeps other built files (like `entrypoint.sh`) intact.
* Avoid mounting the full `../../:/app` because it overwrites the built image.

---

## How the automatic transformation works

1. **Local Development**: Use your props file with `localhost`:
   ```properties
   db.url=jdbc:postgresql://localhost:5432/obp_mapped?user=obp&password=newpass
   ```

2. **Docker Startup**: The `entrypoint.sh` script:
   - Reads your current `db.url` from `default.props`
   - Automatically transforms `localhost` → `host.docker.internal`
   - Sets the `OBP_DB_URL` environment variable
   - Starts the application

3. **Result**: OBP-API uses the transformed URL in Docker, original URL locally.

---

## Useful commands

Rebuild the image and restart:

```bash
docker-compose up --build
```

Stop the container:

```bash
docker-compose down
```

View startup logs to see the database transformation:

```bash
docker-compose logs obp-api
```

---

## Before first run

### 1. Database Setup
Ensure PostgreSQL is running on your host machine with a database accessible via:
```
jdbc:postgresql://localhost:5432/YOUR_DB_NAME?user=YOUR_USER&password=YOUR_PASSWORD
```

### 2. Props Configuration
Configure your database in `obp-api/src/main/resources/props/default.props`:
```properties
db.driver=org.postgresql.Driver
db.url=jdbc:postgresql://localhost:5432/obp_mapped?user=obp&password=f
```

### 3. Make entrypoint executable
Make sure your entrypoint script is executable:

```bash
chmod +x development/docker/entrypoint.sh
```

---

## Manual Configuration (if needed)

If you need to override the automatic transformation, you can set the environment variable manually:

```yaml
# In docker-compose.yml
environment:
  - OBP_DB_URL=jdbc:postgresql://your-custom-host:5432/your_db?user=user&password=pass
```

---

## Notes

* The container uses `MAVEN_OPTS` to pass JVM `--add-opens` flags needed by Lift.
* Database connection automatically uses `host.docker.internal` in Docker environments.
* Both main database (`db.url`) and remotedata database (`remotedata.db.url`) are transformed.
* In production, consider using external database services instead of host.docker.internal.

## Troubleshooting

**Database Connection Issues:**
- Ensure PostgreSQL is running on the host machine
- Check database credentials in your props file
- Verify firewall allows connections to PostgreSQL port 5432
- Check startup logs: `docker-compose logs obp-api`

**Permission Issues:**
- Make sure entrypoint.sh is executable: `chmod +x entrypoint.sh`

**Configuration Issues:**
- Startup logs will show the detected and transformed database URLs
- Verify your `default.props` file has the correct `db.url` setting

---

That's it — now you can run from the `development/docker` directory:

```bash
cd development/docker
docker-compose up --build
```

Your database configuration will automatically work in both local development and Docker! 🎉