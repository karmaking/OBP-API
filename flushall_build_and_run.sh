#!/bin/bash

# Script to flush Redis, build the project, and run Jetty
#
# This script should be run from the OBP-API root directory:
#   cd /path/to/OBP-API
#   ./flushall_build_and_run.sh

set -e  # Exit on error

echo "=========================================="
echo "Flushing Redis cache..."
echo "=========================================="
redis-cli <<EOF
flushall
exit
EOF

if [ $? -eq 0 ]; then
    echo "Redis cache flushed successfully"
else
    echo "Warning: Failed to flush Redis cache. Continuing anyway..."
fi

echo ""
echo "=========================================="
echo "Building and running with Maven..."
echo "=========================================="
export MAVEN_OPTS="-Xss128m --add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.lang.reflect=ALL-UNNAMED --add-opens java.base/java.util=ALL-UNNAMED --add-opens java.base/java.lang.invoke=ALL-UNNAMED --add-opens java.base/sun.reflect.generics.reflectiveObjects=ALL-UNNAMED"
mvn install -pl .,obp-commons && mvn jetty:run -pl obp-api
