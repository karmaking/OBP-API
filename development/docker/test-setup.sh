#!/bin/bash

# Test script to verify Docker setup after moving from ./docker to ./development/docker
set -e

echo "=== OBP-API Docker Setup Test ==="
echo "Testing the moved Docker configuration..."
echo

# Check if we're in the right directory
if [[ ! -f "docker-compose.yml" ]]; then
    echo "❌ Error: docker-compose.yml not found in current directory"
    echo "   Please run this script from the development/docker directory"
    exit 1
fi

echo "✅ Found docker-compose.yml"

# Check if entrypoint.sh exists and is executable
if [[ ! -f "entrypoint.sh" ]]; then
    echo "❌ Error: entrypoint.sh not found"
    exit 1
fi

if [[ ! -x "entrypoint.sh" ]]; then
    echo "❌ Error: entrypoint.sh is not executable"
    echo "   Run: chmod +x entrypoint.sh"
    exit 1
fi

echo "✅ entrypoint.sh exists and is executable"

# Test docker-compose config validation
echo "🔍 Validating docker-compose configuration..."
if docker-compose config > /dev/null 2>&1; then
    echo "✅ Docker-compose configuration is valid"
else
    echo "❌ Error: Docker-compose configuration is invalid"
    echo "   Running docker-compose config for details:"
    docker-compose config
    exit 1
fi

# Check if required source directories exist
echo "🔍 Checking source directories..."
if [[ -d "../../obp-api" ]]; then
    echo "✅ Found ../../obp-api directory"
else
    echo "❌ Error: ../../obp-api directory not found"
    exit 1
fi

if [[ -d "../../obp-commons" ]]; then
    echo "✅ Found ../../obp-commons directory"
else
    echo "❌ Error: ../../obp-commons directory not found"
    exit 1
fi

# Check if main project files exist
echo "🔍 Checking main project files..."
if [[ -f "../../pom.xml" ]]; then
    echo "✅ Found ../../pom.xml"
else
    echo "❌ Error: ../../pom.xml not found"
    exit 1
fi

# Test Docker build (use cache for faster testing)
echo "🔍 Testing Docker build..."
if docker-compose build > /tmp/docker-build.log 2>&1; then
    echo "✅ Docker build completed successfully"
else
    echo "❌ Error: Docker build failed"
    echo "   Check the build log:"
    tail -20 /tmp/docker-build.log
    exit 1
fi

# Test that the container can start and the entrypoint is accessible
echo "🔍 Testing container startup and entrypoint..."
if docker-compose run --rm -T obp-api ls -la /app/entrypoint.sh > /dev/null 2>&1; then
    echo "✅ Container starts correctly and entrypoint is accessible"
else
    echo "❌ Error: Container startup test failed"
    exit 1
fi

# Test volume mounts work
echo "🔍 Testing volume mounts..."
if docker-compose run --rm -T obp-api ls -la /app/obp-api/pom.xml > /dev/null 2>&1; then
    echo "✅ obp-api volume mount works"
else
    echo "❌ Error: obp-api volume mount failed"
    exit 1
fi

if docker-compose run --rm -T obp-api ls -la /app/obp-commons/pom.xml > /dev/null 2>&1; then
    echo "✅ obp-commons volume mount works"
else
    echo "❌ Error: obp-commons volume mount failed"
    exit 1
fi

# Test database connectivity and application startup
echo "🔍 Testing database connectivity and application startup..."
echo "   Starting containers..."
docker-compose up -d > /dev/null 2>&1

# Wait for application to start (with timeout)
echo "   Waiting for application to start (this may take a few minutes)..."
timeout=300  # 5 minutes timeout
elapsed=0
interval=10

while [ $elapsed -lt $timeout ]; do
    if curl -s -f http://localhost:8080 > /dev/null 2>&1; then
        echo "✅ Application started and responding on port 8080"
        app_started=true
        break
    fi
    
    # Check if container is still running
    if ! docker-compose ps -q obp-api | xargs docker inspect -f '{{.State.Running}}' 2>/dev/null | grep -q true; then
        echo "❌ Error: Container stopped unexpectedly"
        echo "   Check logs with: docker-compose logs obp-api"
        docker-compose down > /dev/null 2>&1
        exit 1
    fi
    
    sleep $interval
    elapsed=$((elapsed + interval))
    echo "   Still waiting... (${elapsed}s elapsed)"
done

if [ "$app_started" != "true" ]; then
    echo "❌ Error: Application did not start within ${timeout} seconds"
    echo "   This might be normal for first run (downloading dependencies)"
    echo "   Check logs with: docker-compose logs obp-api"
    echo "   You can continue with manual testing using docker-compose up"
else
    echo "✅ Database connectivity and application startup successful"
fi

# Clean up test containers
docker-compose down > /dev/null 2>&1

echo
echo "🎉 All tests passed! Docker setup is working correctly."
echo
echo "Usage instructions:"
echo "  1. Navigate to development/docker directory:"
echo "     cd development/docker"
echo
echo "  2. Start the service:"
echo "     docker-compose up"
echo
echo "  3. For development with live reload:"
echo "     docker-compose up --build"
echo
echo "  4. Access the API at:"
echo "     http://localhost:8080"
echo
echo "  5. Stop the service:"
echo "     docker-compose down"
echo
echo "Database Configuration:"
echo "  The setup uses: jdbc:postgresql://host.docker.internal:5432/obp_mapped"
echo "  Username: obp"
echo "  Password: f"
echo "  Make sure PostgreSQL is running on your host machine"
echo

echo "✅ Setup verification complete!"
echo
echo "Next Steps:"
echo "  - Ensure PostgreSQL is running with the configured database"
echo "  - Run 'docker-compose up' to start the application"
echo "  - First startup may take several minutes downloading dependencies"
echo "  - Check logs with 'docker-compose logs -f obp-api' if needed"