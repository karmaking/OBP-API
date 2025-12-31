#!/bin/bash

################################################################################
# Run Specific Tests Script
#
# Simple script to run specific test classes for fast iteration.
# Edit SPECIFIC_TESTS array below with the test class names you want to run.
#
# Usage:
#   ./run_specific_tests.sh
#
# Configuration:
#   Update SPECIFIC_TESTS array with FULL PACKAGE PATH (required for ScalaTest)
#
# IMPORTANT: ScalaTest requires full package path!
#   - Must include: code.api.vX_X_X.TestClassName
#   - Do NOT use just "TestClassName"
#   - Do NOT include .scala extension
#
# Examples:
#   SPECIFIC_TESTS=("code.api.v6_0_0.RateLimitsTest")
#   SPECIFIC_TESTS=("code.api.v6_0_0.RateLimitsTest" "code.api.v6_0_0.ConsumerTest")
#
# How to find package path:
#   1. Find test file: obp-api/src/test/scala/code/api/v6_0_0/RateLimitsTest.scala
#   2. Package path: code.api.v6_0_0.RateLimitsTest
#
# Output:
#   - test-results/last_specific_run.log
#   - test-results/last_specific_run_summary.log
#
# Technical Note:
#   Uses Maven -Dsuites parameter (NOT -Dtest) because we use scalatest-maven-plugin
#   The -Dtest parameter is for surefire plugin and doesn't work with ScalaTest
################################################################################

set -e

################################################################################
# CONFIGURATION - Edit this!
################################################################################

# Test class names - MUST include full package path for ScalaTest!
# Format: "code.api.vX_X_X.TestClassName"
# Example: "code.api.v6_0_0.RateLimitsTest"
SPECIFIC_TESTS=(
  "code.api.v6_0_0.RateLimitsTest"
)

################################################################################
# Script Logic
################################################################################

LOG_DIR="test-results"
DETAIL_LOG="${LOG_DIR}/last_specific_run.log"
SUMMARY_LOG="${LOG_DIR}/last_specific_run_summary.log"

mkdir -p "${LOG_DIR}"

# Check if tests are configured
if [ ${#SPECIFIC_TESTS[@]} -eq 0 ]; then
  echo "ERROR: No tests configured!"
  echo "Edit this script and add test names to SPECIFIC_TESTS array"
  exit 1
fi

echo "=========================================="
echo "Running Specific Tests"
echo "=========================================="
echo ""
echo "Tests to run:"
for test in "${SPECIFIC_TESTS[@]}"; do
  echo "  - $test"
done
echo ""
echo "Logs: ${DETAIL_LOG}"
echo ""

# Set Maven options
export MAVEN_OPTS="-Xss128m -Xms3G -Xmx6G -XX:MaxMetaspaceSize=2G --add-opens java.base/java.lang.invoke=ALL-UNNAMED --add-opens java.base/java.lang=ALL-UNNAMED"

# Build test list (space-separated for ScalaTest -Dsuites)
TEST_ARG="${SPECIFIC_TESTS[*]}"

# Start time
START_TIME=$(date +%s)

# Run tests
# NOTE: We use -Dsuites (NOT -Dtest) because obp-api uses scalatest-maven-plugin
# The -Dtest parameter only works with maven-surefire-plugin (JUnit tests)
# ScalaTest requires the -Dsuites parameter with full package paths
echo "Executing: mvn -pl obp-api test -Dsuites=\"$TEST_ARG\""
echo ""

if mvn -pl obp-api test -Dsuites="$TEST_ARG" 2>&1 | tee "${DETAIL_LOG}"; then
  TEST_RESULT="SUCCESS"
else
  TEST_RESULT="FAILURE"
fi

# End time
END_TIME=$(date +%s)
DURATION=$((END_TIME - START_TIME))
DURATION_MIN=$((DURATION / 60))
DURATION_SEC=$((DURATION % 60))

# Write summary
{
  echo "=========================================="
  echo "Test Run Summary"
  echo "=========================================="
  echo "Result:   ${TEST_RESULT}"
  echo "Duration: ${DURATION_MIN}m ${DURATION_SEC}s"
  echo ""
  echo "Tests Run:"
  for test in "${SPECIFIC_TESTS[@]}"; do
    echo "  - $test"
  done
  echo ""
  echo "Logs:"
  echo "  ${DETAIL_LOG}"
  echo "  ${SUMMARY_LOG}"
} | tee "${SUMMARY_LOG}"

echo ""
echo "=========================================="
echo "Done!"
echo "=========================================="

# Exit with test result
if [ "$TEST_RESULT" = "FAILURE" ]; then
  exit 1
fi
