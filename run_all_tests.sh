#!/bin/bash

################################################################################
# OBP-API Test Runner Script
#
# What it does:
#   1. Changes terminal to blue background with "Tests Running" in title
#   2. Runs: mvn clean test
#   3. Shows all test output in real-time
#   4. Updates title bar with: phase, time elapsed, pass/fail counts
#   5. Saves detailed log and summary to test-results/
#   6. Restores terminal to normal when done
#
# Usage: ./run_all_tests.sh
################################################################################

set -e

################################################################################
# TERMINAL STYLING FUNCTIONS
################################################################################

# Set terminal to "test mode" - blue background, special title
set_terminal_style() {
    local phase="${1:-Running}"
    echo -ne "\033]0;🧪 OBP-API Tests ${phase}...\007"  # Title
    echo -ne "\033]11;#001f3f\007"  # Dark blue background
    echo -ne "\033]10;#ffffff\007"  # White text
    # Print header bar
    printf "\033[44m\033[1;37m%-$(tput cols)s\r  🧪 OBP-API TEST RUNNER ACTIVE - ${phase}  \n%-$(tput cols)s\033[0m\n" " " " "
}

# Update title bar with progress: "Testing... [5m 23s] ✓42 ✗0"
update_terminal_title() {
    local phase="$1"           # Starting, Building, Testing, Complete
    local elapsed="${2:-}"     # Time elapsed (e.g. "5m 23s")
    local passed="${3:-}"      # Number of tests passed
    local failed="${4:-}"      # Number of tests failed

    local title="🧪 OBP-API Tests ${phase}..."
    [ -n "$elapsed" ] && title="${title} [${elapsed}]"
    [ -n "$passed" ] && title="${title} ✓${passed}"
    [ -n "$failed" ] && [ "$failed" != "0" ] && title="${title} ✗${failed}"

    echo -ne "\033]0;${title}\007"
}

# Restore terminal to normal (black background, default title)
restore_terminal_style() {
    echo -ne "\033]0;Terminal\007\033]11;#000000\007\033]10;#ffffff\007\033[0m"
}

# Always restore terminal on exit (Ctrl+C, errors, or normal completion)
trap restore_terminal_style EXIT INT TERM

################################################################################
# CONFIGURATION
################################################################################

TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
LOG_DIR="test-results"
DETAIL_LOG="${LOG_DIR}/test_run_${TIMESTAMP}.log"       # Full Maven output
SUMMARY_LOG="${LOG_DIR}/test_summary_${TIMESTAMP}.log"  # Summary only
LATEST_SUMMARY="${LOG_DIR}/latest_test_summary.log"     # Link to latest

# Terminal colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

mkdir -p "${LOG_DIR}"

################################################################################
# HELPER FUNCTIONS
################################################################################

# Log message to terminal and summary file
log_message() {
    echo -e "$1"
    echo "[$(date +"%Y-%m-%d %H:%M:%S")] $1" | sed 's/\x1b\[[0-9;]*m//g' >> "${SUMMARY_LOG}"
}

# Print section header
print_header() {
    echo ""
    echo "================================================================================"
    echo "$1"
    echo "================================================================================"
    echo ""
}

################################################################################
# START TEST RUN
################################################################################

set_terminal_style "Starting"

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

# Ensure test properties file exists
PROPS_FILE="obp-api/src/main/resources/props/test.default.props"
PROPS_TEMPLATE="${PROPS_FILE}.template"

if [ -f "${PROPS_FILE}" ]; then
    log_message "${GREEN}✓ Found test.default.props${NC}"
else
    log_message "${YELLOW}⚠ WARNING: test.default.props not found - creating from template${NC}"
    if [ -f "${PROPS_TEMPLATE}" ]; then
        cp "${PROPS_TEMPLATE}" "${PROPS_FILE}"
        log_message "${GREEN}✓ Created test.default.props${NC}"
    else
        log_message "${RED}ERROR: ${PROPS_TEMPLATE} not found!${NC}"
        exit 1
    fi
fi

################################################################################
# RUN TESTS
################################################################################

print_header "Running Tests"
update_terminal_title "Building"
log_message "${BLUE}Executing: mvn clean test${NC}"
echo ""

START_TIME=$(date +%s)

# Background process: Monitor log file and update title bar with progress
(
    sleep 5
    phase="Building"
    in_testing=false

    while true; do
        passed=""
        failed=""

        if [ -f "${DETAIL_LOG}" ]; then
            # Switch to "Testing" phase when tests start
            if ! $in_testing && grep -q "Run starting" "${DETAIL_LOG}" 2>/dev/null; then
                phase="Testing"
                in_testing=true
            fi

            # Extract test counts: "Tests: succeeded 21, failed 0"
            if $in_testing; then
                test_line=$(grep -E "Tests:.*succeeded.*failed" "${DETAIL_LOG}" 2>/dev/null | tail -1)
                if [ -n "$test_line" ]; then
                    passed=$(echo "$test_line" | grep -oP "succeeded \K\d+" | tail -1)
                    failed=$(echo "$test_line" | grep -oP "failed \K\d+" | tail -1)
                fi
            fi
        fi

        # Calculate elapsed time
        duration=$(($(date +%s) - START_TIME))
        minutes=$((duration / 60))
        seconds=$((duration % 60))
        elapsed=$(printf "%dm %ds" $minutes $seconds)

        # Update title: "Testing... [5m 23s] ✓42 ✗0"
        update_terminal_title "$phase" "$elapsed" "$passed" "$failed"

        sleep 5
    done
) &
MONITOR_PID=$!

# Run Maven (all output goes to terminal AND log file)
if mvn clean test 2>&1 | tee "${DETAIL_LOG}"; then
    TEST_RESULT="SUCCESS"
    RESULT_COLOR="${GREEN}"
else
    TEST_RESULT="FAILURE"
    RESULT_COLOR="${RED}"
fi

# Stop background monitor
kill $MONITOR_PID 2>/dev/null
wait $MONITOR_PID 2>/dev/null

END_TIME=$(date +%s)
DURATION=$((END_TIME - START_TIME))
DURATION_MIN=$((DURATION / 60))
DURATION_SEC=$((DURATION % 60))

# Update title with final results
FINAL_ELAPSED=$(printf "%dm %ds" $DURATION_MIN $DURATION_SEC)
FINAL_PASSED=$(grep -E "Tests:.*succeeded.*failed" "${DETAIL_LOG}" 2>/dev/null | tail -1 | grep -oP "succeeded \K\d+" | tail -1)
FINAL_FAILED=$(grep -E "Tests:.*succeeded.*failed" "${DETAIL_LOG}" 2>/dev/null | tail -1 | grep -oP "failed \K\d+" | tail -1)
update_terminal_title "Complete" "$FINAL_ELAPSED" "$FINAL_PASSED" "$FINAL_FAILED"

################################################################################
# GENERATE SUMMARY
################################################################################

print_header "Test Results Summary"

# Extract test statistics
TOTAL_TESTS=$(grep -E "Total number of tests run:|Tests run:" "${DETAIL_LOG}" | tail -1 | grep -oP '\d+' | head -1 || echo "0")
SUCCEEDED=$(grep -oP "succeeded \K\d+" "${DETAIL_LOG}" | tail -1 || echo "0")
FAILED=$(grep -oP "failed \K\d+" "${DETAIL_LOG}" | tail -1 || echo "0")
ERRORS=$(grep -oP "errors \K\d+" "${DETAIL_LOG}" | tail -1 || echo "0")
SKIPPED=$(grep -oP "(skipped|ignored) \K\d+" "${DETAIL_LOG}" | tail -1 || echo "0")

# Determine build status
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

# Print summary
log_message "Test Run Summary"
log_message "================"
log_message "Timestamp:     $(date)"
log_message "Duration:      ${DURATION_MIN}m ${DURATION_SEC}s"
log_message "Build Status:  ${BUILD_COLOR}${BUILD_STATUS}${NC}"
log_message ""
log_message "Test Statistics:"
log_message "  Total:       ${TOTAL_TESTS}"
log_message "  ${GREEN}Succeeded:   ${SUCCEEDED}${NC}"
log_message "  ${RED}Failed:      ${FAILED}${NC}"
log_message "  ${RED}Errors:      ${ERRORS}${NC}"
log_message "  ${YELLOW}Skipped:     ${SKIPPED}${NC}"
log_message ""

# Show failed tests if any
if [ "${FAILED}" != "0" ] || [ "${ERRORS}" != "0" ]; then
    log_message "${RED}Failed Tests:${NC}"
    grep -A 5 "FAILED\|ERROR" "${DETAIL_LOG}" | head -50 >> "${SUMMARY_LOG}"
    log_message ""
fi

cp "${SUMMARY_LOG}" "${LATEST_SUMMARY}"

################################################################################
# FINAL RESULT
################################################################################

print_header "Test Run Complete"

if [ "${BUILD_STATUS}" = "SUCCESS" ] && [ "${FAILED}" = "0" ] && [ "${ERRORS}" = "0" ]; then
    log_message "${GREEN}✓ All tests passed!${NC}"
    EXIT_CODE=0
else
    log_message "${RED}✗ Tests failed${NC}"
    EXIT_CODE=1
fi

log_message ""
log_message "Logs:"
log_message "  Detailed: ${DETAIL_LOG}"
log_message "  Summary:  ${SUMMARY_LOG}"
log_message "  Latest:   ${LATEST_SUMMARY}"
echo ""

exit ${EXIT_CODE}
