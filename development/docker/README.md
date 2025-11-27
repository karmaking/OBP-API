# OBP-API Docker Development Setup

This Docker Compose setup provides a complete development environment for OBP-API with Redis caching support.

## Services

### 🏦 **obp-api-app** 
- Main OBP-API application
- Built with Maven 3 + OpenJDK 17
- Runs on Jetty 9.4
- Port: `8080`

### 🔴 **obp-api-redis**
- Redis cache server
- Version: Redis 7 Alpine
- Internal port: `6379`
- External port: `6380` (configurable)
- Persistent storage with AOF

## Quick Start

1. **Prerequisites**
   - Docker and Docker Compose installed
   - Local PostgreSQL database running
   - Props file configured at `obp-api/src/main/resources/props/default.props`

2. **Start services**
   ```bash
   cd development/docker
   docker-compose up --build
   ```

3. **Access application**
   - OBP-API: http://localhost:8080
   - Redis: `localhost:6380`

## Configuration

### Database Connection

Your `default.props` should use `host.docker.internal` to connect to your local database:

```properties
db.driver=org.postgresql.Driver
db.url=jdbc:postgresql://host.docker.internal:5432/obp_mapped?user=obp&password=yourpassword
```

**Note**: The Docker setup automatically overrides the database URL via environment variable, so you can also configure it without modifying props files.

### Redis Configuration

Redis is configured automatically using OBP-API's environment variable override system:

```yaml
# Automatically set by docker-compose.yml:
OBP_CACHE_REDIS_URL=redis      # Connect to redis service
OBP_CACHE_REDIS_PORT=6379      # Internal Docker port
OBP_DB_URL=jdbc:postgresql://host.docker.internal:5432/obp_mapped?user=obp&password=f
```

### Custom Redis Port

To customize configuration, edit `.env`:

```bash
# .env file
OBP_CACHE_REDIS_PORT=6381
OBP_DB_URL=jdbc:postgresql://host.docker.internal:5432/mydb?user=myuser&password=mypass
```

Or set environment variables:

```bash
export OBP_CACHE_REDIS_PORT=6381
export OBP_DB_URL="jdbc:postgresql://host.docker.internal:5432/mydb?user=myuser&password=mypass"
docker-compose up --build
```

## Container Names

All containers use consistent `obp-api-*` naming:

- `obp-api-app` - Main application
- `obp-api-redis` - Redis cache server
- `obp-api-network` - Docker network
- `obp-api-redis-data` - Redis data volume

## Development Features

### Props File Override

The setup mounts your local props directory:
```yaml
volumes:
  - ../../obp-api/src/main/resources/props:/app/props
```

Environment variables take precedence over props files using OBP's built-in system:
- `cache.redis.url` → `OBP_CACHE_REDIS_URL`
- `cache.redis.port` → `OBP_CACHE_REDIS_PORT`
- `db.url` → `OBP_DB_URL`

### Live Development

For code changes without rebuilds:
```yaml
# docker-compose.override.yml provides:
volumes:
  - ../../obp-api:/app/obp-api
  - ../../obp-commons:/app/obp-commons
```

## Useful Commands

### Service Management
```bash
# Start services
docker-compose up -d

# View logs
docker-compose logs obp-api-app
docker-compose logs obp-api-redis

# Stop services  
docker-compose down

# Rebuild and restart
docker-compose up --build
```

### Redis Operations
```bash
# Connect to Redis CLI
docker exec -it obp-api-redis redis-cli

# Check Redis keys
docker exec obp-api-redis redis-cli KEYS "*"

# Monitor Redis commands
docker exec obp-api-redis redis-cli MONITOR
```

### Container Inspection
```bash
# List containers
docker-compose ps

# Execute commands in containers
docker exec -it obp-api-app bash
docker exec -it obp-api-redis sh
```

## Troubleshooting

### Redis Connection Issues
- Check if `OBP_CACHE_REDIS_URL=redis` is set correctly
- Verify Redis container is running: `docker-compose ps`
- Test Redis connection: `docker exec obp-api-redis redis-cli ping`

### Database Connection Issues  
- Ensure local PostgreSQL is running
- Verify `host.docker.internal` resolves: `docker exec obp-api-app ping host.docker.internal`
- Check props file is mounted: `docker exec obp-api-app ls /app/props/`

### Props Loading Issues
- Check external props are detected: `docker-compose logs obp-api-app | grep "external props"`
- Verify environment variables: `docker exec obp-api-app env | grep OBP_`

## Environment Variables

The setup uses OBP-API's built-in environment override system:

| Props File Property | Environment Variable | Default | Description |
|---------------------|---------------------|---------|-------------|
| `cache.redis.url` | `OBP_CACHE_REDIS_URL` | `redis` | Redis hostname |
| `cache.redis.port` | `OBP_CACHE_REDIS_PORT` | `6379` | Redis port |
| `cache.redis.password` | `OBP_CACHE_REDIS_PASSWORD` | - | Redis password |
| `db.url` | `OBP_DB_URL` | `jdbc:postgresql://host.docker.internal:5432/obp_mapped?user=obp&password=f` | Database connection URL |

## Network Architecture

```
Host Machine
├── PostgreSQL :5432
└── Docker Network (obp-api-network)
    ├── obp-api-app :8080 → :8080
    └── obp-api-redis :6379 → :6380
```

- OBP-API connects to Redis via internal Docker network (`redis:6379`)
- OBP-API connects to PostgreSQL via `host.docker.internal:5432`
- Redis is accessible from host via `localhost:6380`

## Notes

- Container builds use multi-stage Dockerfile for optimized images
- Redis data persists in `obp-api-redis-data` volume
- Props files are mounted from host for easy development
- Environment variables override props file values automatically
- All containers restart automatically unless stopped manually

---

🚀 **Ready to develop!** Run `docker-compose up --build` and start coding.