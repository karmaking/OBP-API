#!/bin/bash

################################################################################
# OBP-API Test Runner Script
#
# This script runs all tests for the OBP-API project and generates:
# - A detailed log file with all test output
# - A summary file with test results
#
# Usage: ./run_all_tests.sh
################################################################################

set -e

# Configuration
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
LOG_DIR="test-results"
DETAIL_LOG="${LOG_DIR}/test_run_${TIMESTAMP}.log"
SUMMARY_LOG="${LOG_DIR}/test_summary_${TIMESTAMP}.log"
LATEST_SUMMARY="${LOG_DIR}/latest_test_summary.log"

# Colors for terminal output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Create log directory if it doesn't exist
mkdir -p "${LOG_DIR}"

# Function to print with timestamp
log_message() {
    local message="$1"
    echo -e "${message}"
    echo "[$(date +"%Y-%m-%d %H:%M:%S")] ${message}" | sed 's/\x1b\[[0-9;]*m//g' >> "${SUMMARY_LOG}"
}

# Function to print section header
print_header() {
    local message="$1"
    echo ""
    echo "================================================================================"
    echo "${message}"
    echo "================================================================================"
    echo ""
}

# Start the test run
print_header "OBP-API Test Suite"
log_message "${BLUE}Starting test run at $(date)${NC}"
log_message "Detail log: ${DETAIL_LOG}"
log_message "Summary log: ${SUMMARY_LOG}"
echo ""

# Set Maven options for tests
export MAVEN_OPTS="-Xss128m -Xms3G -Xmx6G -XX:MaxMetaspaceSize=2G"
log_message "${BLUE}Maven Options: ${MAVEN_OPTS}${NC}"
echo ""

# Check if test.default.props exists, if not create it from template
PROPS_FILE="obp-api/src/main/resources/props/test.default.props"
PROPS_TEMPLATE="obp-api/src/main/resources/props/test.default.props.template"

if [ -f "${PROPS_FILE}" ]; then
    log_message "${GREEN}✓ Found test.default.props${NC}"
else
    log_message "${YELLOW}⚠ WARNING: test.default.props not found${NC}"
    if [ -f "${PROPS_TEMPLATE}" ]; then
        log_message "${YELLOW}Creating test.default.props from template...${NC}"
        cp "${PROPS_TEMPLATE}" "${PROPS_FILE}"
        log_message "${GREEN}test.default.props created successfully${NC}"
        log_message "${YELLOW}⚠ Please review and customize test.default.props if needed${NC}"
    else
        log_message "${RED}ERROR: test.default.props.template not found!${NC}"
        exit 1
    fi
fi

# Run the tests
print_header "Running Tests"
log_message "${BLUE}Executing: mvn clean test${NC}"
echo ""

START_TIME=$(date +%s)

# Run Maven tests and capture output
if mvn clean test 2>&1 | tee "${DETAIL_LOG}"; then
    TEST_RESULT="SUCCESS"
    RESULT_COLOR="${GREEN}"
else
    TEST_RESULT="FAILURE"
    RESULT_COLOR="${RED}"
fi

END_TIME=$(date +%s)
DURATION=$((END_TIME - START_TIME))
DURATION_MIN=$((DURATION / 60))
DURATION_SEC=$((DURATION % 60))

# Extract test statistics from the detail log
print_header "Test Results Summary"

# Parse Maven output for test results
TOTAL_TESTS=$(grep -E "Total number of tests run:|Tests run:" "${DETAIL_LOG}" | tail -1 | grep -oP '\d+' | head -1 || echo "0")
SUCCEEDED=$(grep -oP "succeeded \K\d+" "${DETAIL_LOG}" | tail -1 || echo "0")
FAILED=$(grep -oP "failed \K\d+" "${DETAIL_LOG}" | tail -1 || echo "0")
ERRORS=$(grep -oP "errors \K\d+" "${DETAIL_LOG}" | tail -1 || echo "0")
SKIPPED=$(grep -oP "(skipped|ignored) \K\d+" "${DETAIL_LOG}" | tail -1 || echo "0")

# Build status from Maven
if grep -q "BUILD SUCCESS" "${DETAIL_LOG}"; then
    BUILD_STATUS="SUCCESS"
    BUILD_COLOR="${GREEN}"
elif grep -q "BUILD FAILURE" "${DETAIL_LOG}"; then
    BUILD_STATUS="FAILURE"
    BUILD_COLOR="${RED}"
else
    BUILD_STATUS="UNKNOWN"
    BUILD_COLOR="${YELLOW}"
fi

# Write summary
log_message "Test Run Summary"
log_message "================"
log_message "Timestamp:        $(date)"
log_message "Duration:         ${DURATION_MIN}m ${DURATION_SEC}s"
log_message "Build Status:     ${BUILD_COLOR}${BUILD_STATUS}${NC}"
log_message ""
log_message "Test Statistics:"
log_message "  Total Tests:    ${TOTAL_TESTS}"
log_message "  ${GREEN}Succeeded:      ${SUCCEEDED}${NC}"
log_message "  ${RED}Failed:         ${FAILED}${NC}"
log_message "  ${RED}Errors:         ${ERRORS}${NC}"
log_message "  ${YELLOW}Skipped:        ${SKIPPED}${NC}"
log_message ""

# Extract module test results
log_message "Module Results:"
log_message "---------------"
grep -E "Building Open Bank Project|Tests run:|BUILD SUCCESS|BUILD FAILURE" "${DETAIL_LOG}" | while read -r line; do
    if echo "${line}" | grep -q "Building Open Bank Project"; then
        MODULE=$(echo "${line}" | grep -oP "Building \K.*")
        echo "  ${BLUE}${MODULE}${NC}"
        echo "  ${MODULE}" >> "${SUMMARY_LOG}"
    elif echo "${line}" | grep -q "Tests run:"; then
        echo "    ${line}"
        echo "    ${line}" >> "${SUMMARY_LOG}"
    fi
done

# Check for compilation errors
COMPILE_ERRORS=$(grep -c "COMPILATION ERROR" "${DETAIL_LOG}" || echo "0")
if [ "${COMPILE_ERRORS}" -gt 0 ]; then
    log_message ""
    log_message "${RED}⚠ Found ${COMPILE_ERRORS} compilation error(s)${NC}"
fi

# Extract failed tests details if any
if [ "${FAILED}" != "0" ] || [ "${ERRORS}" != "0" ]; then
    log_message ""
    log_message "${RED}Failed Tests Details:${NC}"
    log_message "---------------------"
    grep -A 5 "FAILED\|ERROR" "${DETAIL_LOG}" | head -50 >> "${SUMMARY_LOG}"
fi

# Copy summary to latest
cp "${SUMMARY_LOG}" "${LATEST_SUMMARY}"

# Final result
echo ""
print_header "Test Run Complete"

if [ "${BUILD_STATUS}" = "SUCCESS" ] && [ "${FAILED}" = "0" ] && [ "${ERRORS}" = "0" ]; then
    log_message "${GREEN}✓ All tests passed successfully!${NC}"
    EXIT_CODE=0
else
    log_message "${RED}✗ Some tests failed or errors occurred${NC}"
    EXIT_CODE=1
fi

log_message ""
log_message "Detailed log:  ${DETAIL_LOG}"
log_message "Summary log:   ${SUMMARY_LOG}"
log_message "Latest summary: ${LATEST_SUMMARY}"
echo ""

exit ${EXIT_CODE}
