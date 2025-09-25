#!/bin/bash
set -e

# Function to extract database URL from props file and transform for Docker
setup_docker_db_url() {
    local props_file="obp-api/src/main/resources/props/default.props"
    
    if [ -f "$props_file" ]; then
        # Extract db.url from props file (handle commented and uncommented lines)
        local db_url=$(grep -E "^[[:space:]]*db\.url=" "$props_file" | head -1 | sed 's/^[[:space:]]*db\.url=//')
        
        if [ -n "$db_url" ]; then
            # Transform localhost to host.docker.internal for Docker environment
            local docker_db_url=$(echo "$db_url" | sed 's/localhost/host.docker.internal/g')
            
            echo "Found database URL in props: $db_url"
            echo "Transformed for Docker: $docker_db_url"
            
            # Set the environment variable that OBP-API will use
            export OBP_DB_URL="$docker_db_url"
        else
            echo "Warning: No db.url found in $props_file"
            echo "Using default PostgreSQL configuration for Docker"
            export OBP_DB_URL="jdbc:postgresql://host.docker.internal:5432/obp_mapped?user=obp&password=f"
        fi
    else
        echo "Warning: Props file not found at $props_file"
        echo "Using default PostgreSQL configuration for Docker"
        export OBP_DB_URL="jdbc:postgresql://host.docker.internal:5432/obp_mapped?user=obp&password=f"
    fi
}

# Function to extract remotedata database URL and transform for Docker
setup_docker_remotedata_db_url() {
    local props_file="obp-api/src/main/resources/props/default.props"
    
    if [ -f "$props_file" ]; then
        # Extract remotedata.db.url from props file
        local remotedata_db_url=$(grep -E "^[[:space:]]*remotedata\.db\.url=" "$props_file" | head -1 | sed 's/^[[:space:]]*remotedata\.db\.url=//')
        
        if [ -n "$remotedata_db_url" ]; then
            # Transform localhost to host.docker.internal for Docker environment
            local docker_remotedata_db_url=$(echo "$remotedata_db_url" | sed 's/localhost/host.docker.internal/g')
            
            echo "Found remotedata database URL in props: $remotedata_db_url"
            echo "Transformed for Docker: $docker_remotedata_db_url"
            
            # Set the environment variable that OBP-API will use
            export OBP_REMOTEDATA_DB_URL="$docker_remotedata_db_url"
        fi
    fi
}

echo "=== OBP-API Docker Startup ==="
echo "Setting up database configuration for Docker environment..."

# Setup main database URL
setup_docker_db_url

# Setup remotedata database URL if exists
setup_docker_remotedata_db_url

echo "Database configuration complete."
echo "Starting OBP-API with Maven..."

# Set Maven options for Java 17+ compatibility
export MAVEN_OPTS="-Xss128m \
 --add-opens=java.base/java.util.jar=ALL-UNNAMED \
 --add-opens=java.base/java.lang=ALL-UNNAMED \
 --add-opens=java.base/java.lang.reflect=ALL-UNNAMED"

# Start the application
exec mvn jetty:run -pl obp-api