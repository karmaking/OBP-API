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
    echo -ne "\033]0;OBP-API Tests ${phase}...\007"  # Title
    echo -ne "\033]11;#001f3f\007"  # Dark blue background
    echo -ne "\033]10;#ffffff\007"  # White text
    # Print header bar
    printf "\033[44m\033[1;37m%-$(tput cols)s\r  OBP-API TEST RUNNER ACTIVE - ${phase}  \n%-$(tput cols)s\033[0m\n" " " " "
}

# Update title bar with progress: "Testing: DynamicEntityTest - Scenario name [5m 23s]"
update_terminal_title() {
    local phase="$1"           # Starting, Building, Testing, Complete
    local elapsed="${2:-}"     # Time elapsed (e.g. "5m 23s")
    local counts="${3:-}"      # Module counts (e.g. "obp-commons:+38 obp-api:+245")
    local suite="${4:-}"       # Current test suite name
    local scenario="${5:-}"    # Current scenario name

    local title="OBP-API ${phase}"
    [ -n "$suite" ] && title="${title}: ${suite}"
    [ -n "$scenario" ] && title="${title} - ${scenario}"
    title="${title}..."
    [ -n "$elapsed" ] && title="${title} [${elapsed}]"
    [ -n "$counts" ] && title="${title} ${counts}"

    echo -ne "\033]0;${title}\007"
}

# Restore terminal to normal (black background, default title)
restore_terminal_style() {
    echo -ne "\033]0;Terminal\007\033]11;#000000\007\033]10;#ffffff\007\033[0m"
}

# Cleanup function: stop monitor, restore terminal, remove flag files
cleanup_on_exit() {
    # Stop background monitor if running
    if [ -n "${MONITOR_PID:-}" ]; then
        kill $MONITOR_PID 2>/dev/null || true
        wait $MONITOR_PID 2>/dev/null || true
    fi

    # Remove monitor flag file
    rm -f "${LOG_DIR}/monitor.flag" 2>/dev/null || true

    # Restore terminal
    restore_terminal_style
}

# Always cleanup on exit (Ctrl+C, errors, or normal completion)
trap cleanup_on_exit EXIT INT TERM

################################################################################
# CONFIGURATION
################################################################################

LOG_DIR="test-results"
DETAIL_LOG="${LOG_DIR}/last_run.log"        # Full Maven output
SUMMARY_LOG="${LOG_DIR}/last_run_summary.log"  # Summary only

# Terminal colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

mkdir -p "${LOG_DIR}"

# Delete old log files and stale flag files from previous run
echo "Cleaning up old files..."
if [ -f "${DETAIL_LOG}" ]; then
    rm -f "${DETAIL_LOG}"
    echo "  - Removed old detail log"
fi
if [ -f "${SUMMARY_LOG}" ]; then
    rm -f "${SUMMARY_LOG}"
    echo "  - Removed old summary log"
fi
if [ -f "${LOG_DIR}/monitor.flag" ]; then
    rm -f "${LOG_DIR}/monitor.flag"
    echo "  - Removed stale monitor flag"
fi
if [ -f "${LOG_DIR}/warning_analysis.tmp" ]; then
    rm -f "${LOG_DIR}/warning_analysis.tmp"
    echo "  - Removed stale warning analysis"
fi
if [ -f "${LOG_DIR}/recent_lines.tmp" ]; then
    rm -f "${LOG_DIR}/recent_lines.tmp"
    echo "  - Removed stale temp file"
fi

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

# Analyze warnings and return top contributors
analyze_warnings() {
    local log_file="$1"
    local temp_file="${LOG_DIR}/warning_analysis.tmp"

    # Extract and categorize warnings from last 5000 lines (for performance)
    # This gives good coverage without scanning entire multi-MB log file
    tail -n 5000 "${log_file}" 2>/dev/null | grep -i "warning" | \
        # Normalize patterns to group similar warnings
        sed -E 's/line [0-9]+/line XXX/g' | \
        sed -E 's/[0-9]+ warnings?/N warnings/g' | \
        sed -E 's/\[WARNING\] .*(src|test)\/[^ ]+/[WARNING] <source-file>/g' | \
        sed -E 's/version [0-9]+\.[0-9]+(\.[0-9]+)?/version X.X/g' | \
        # Extract the core warning message
        sed -E 's/^.*\[WARNING\] *//' | \
        sort | uniq -c | sort -rn > "${temp_file}"

    # Return the temp file path for further processing
    echo "${temp_file}"
}

# Format and display top warning factors
display_warning_factors() {
    local analysis_file="$1"
    local max_display="${2:-10}"

    if [ ! -f "${analysis_file}" ] || [ ! -s "${analysis_file}" ]; then
        log_message "  ${YELLOW}No detailed warning analysis available${NC}"
        return
    fi

    local total_warning_types=$(wc -l < "${analysis_file}")
    local displayed=0

    log_message "${YELLOW}Top Warning Factors:${NC}"
    log_message "-------------------"

    while IFS= read -r line && [ $displayed -lt $max_display ]; do
        # Extract count and message
        local count=$(echo "$line" | awk '{print $1}')
        local message=$(echo "$line" | sed -E 's/^[[:space:]]*[0-9]+[[:space:]]*//')

        # Truncate long messages
        if [ ${#message} -gt 80 ]; then
            message="${message:0:77}..."
        fi

        # Format with count prominence
        printf "  ${YELLOW}%4d x${NC} %s\n" "$count" "$message" | tee -a "${SUMMARY_LOG}" > /dev/tty

        displayed=$((displayed + 1))
    done < "${analysis_file}"

    if [ $total_warning_types -gt $max_display ]; then
        local remaining=$((total_warning_types - max_display))
        log_message "  ... and ${remaining} more warning type(s)"
    fi

    # Clean up temp file
    rm -f "${analysis_file}"
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
# The --add-opens flags tell Java 17 to allow Kryo serialization library to access
# the internal java.lang.invoke and java.lang modules, which fixes the InaccessibleObjectException
export MAVEN_OPTS="-Xss128m -Xms3G -Xmx6G -XX:MaxMetaspaceSize=2G --add-opens java.base/java.lang.invoke=ALL-UNNAMED --add-opens java.base/java.lang=ALL-UNNAMED"
log_message "${BLUE}Maven Options: ${MAVEN_OPTS}${NC}"
echo ""

# Ensure test properties file exists
PROPS_FILE="obp-api/src/main/resources/props/test.default.props"
PROPS_TEMPLATE="${PROPS_FILE}.template"

if [ -f "${PROPS_FILE}" ]; then
    log_message "${GREEN}[OK] Found test.default.props${NC}"
else
    log_message "${YELLOW}[WARNING] test.default.props not found - creating from template${NC}"
    if [ -f "${PROPS_TEMPLATE}" ]; then
        cp "${PROPS_TEMPLATE}" "${PROPS_FILE}"
        log_message "${GREEN}[OK] Created test.default.props${NC}"
    else
        log_message "${RED}ERROR: ${PROPS_TEMPLATE} not found!${NC}"
        exit 1
    fi
fi

################################################################################
# CLEAN METRICS DATABASE
################################################################################

print_header "Cleaning Metrics Database"
log_message "${YELLOW}Checking for test database files...${NC}"

# Only delete specific test database files to prevent accidental data loss
# The test configuration uses test_only_lift_proto.db as the database filename
TEST_DB_PATTERNS=(
    "./test_only_lift_proto.db"
    "./test_only_lift_proto.db.mv.db"
    "./test_only_lift_proto.db.trace.db"
    "./obp-api/test_only_lift_proto.db"
    "./obp-api/test_only_lift_proto.db.mv.db"
    "./obp-api/test_only_lift_proto.db.trace.db"
)

FOUND_FILES=false
for dbfile in "${TEST_DB_PATTERNS[@]}"; do
    if [ -f "$dbfile" ]; then
        FOUND_FILES=true
        rm -f "$dbfile"
        log_message "  ${GREEN}[OK]${NC} Deleted: $dbfile"
    fi
done

if [ "$FOUND_FILES" = false ]; then
    log_message "${GREEN}No old test database files found${NC}"
fi

log_message ""

################################################################################
# RUN TESTS
################################################################################

print_header "Running Tests"
update_terminal_title "Building"
log_message "${BLUE}Executing: mvn clean test${NC}"
echo ""

START_TIME=$(date +%s)
export START_TIME

# Create flag file to signal background process to stop
MONITOR_FLAG="${LOG_DIR}/monitor.flag"
touch "${MONITOR_FLAG}"

# Background process: Monitor log file and update title bar with progress
(
    # Wait for log file to be created and have Maven output
    while [ ! -f "${DETAIL_LOG}" ] || [ ! -s "${DETAIL_LOG}" ]; do
        sleep 1
    done

    phase="Building"
    in_testing=false

    # Keep monitoring until flag file is removed
    while [ -f "${MONITOR_FLAG}" ]; do
        # Use tail to look at recent lines only (last 500 lines for performance)
        # This ensures O(1) performance regardless of log file size
        recent_lines=$(tail -n 500 "${DETAIL_LOG}" 2>/dev/null)

        # Switch to "Testing" phase when tests start
        if ! $in_testing && echo "$recent_lines" | grep -q "Run starting" 2>/dev/null; then
            phase="Testing"
            in_testing=true
        fi

        # Extract current running test suite and scenario from recent lines
        suite=""
        scenario=""
        if $in_testing; then
            # Find the most recent test suite name (pattern like "SomeTest:")
            # Pipe directly to avoid temp file I/O
            suite=$(echo "$recent_lines" | grep -E "Test:" | tail -1 | sed 's/\x1b\[[0-9;]*m//g' | sed 's/:$//' | tr -d '\n\r')

            # Find the most recent scenario name (pattern like "  Scenario: ..." or "- Scenario: ...")
            scenario=$(echo "$recent_lines" | grep -i "scenario:" | tail -1 | sed 's/\x1b\[[0-9;]*m//g' | sed 's/^[[:space:]]*-*[[:space:]]*//' | sed -E 's/^[Ss]cenario:[[:space:]]*//' | tr -d '\n\r')

            # Truncate scenario if too long (max 50 chars)
            if [ -n "$scenario" ] && [ ${#scenario} -gt 50 ]; then
                scenario="${scenario:0:47}..."
            fi
        fi

        # Calculate elapsed time
        duration=$(($(date +%s) - START_TIME))
        minutes=$((duration / 60))
        seconds=$((duration % 60))
        elapsed=$(printf "%dm %ds" $minutes $seconds)

        # Update title: "Testing: DynamicEntityTest - Scenario name [5m 23s]"
        update_terminal_title "$phase" "$elapsed" "" "$suite" "$scenario"

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

# Stop background monitor by removing flag file
rm -f "${MONITOR_FLAG}"
sleep 1
kill $MONITOR_PID 2>/dev/null || true
wait $MONITOR_PID 2>/dev/null || true

END_TIME=$(date +%s)
DURATION=$((END_TIME - START_TIME))
DURATION_MIN=$((DURATION / 60))
DURATION_SEC=$((DURATION % 60))

# Update title with final results (no suite/scenario name for Complete phase)
FINAL_ELAPSED=$(printf "%dm %ds" $DURATION_MIN $DURATION_SEC)
# Build final counts with module context
FINAL_COMMONS=$(sed -n '/Building Open Bank Project Commons/,/Building Open Bank Project API/{/Tests: succeeded/p;}' "${DETAIL_LOG}" 2>/dev/null | grep -oP "succeeded \K\d+" | head -1)
FINAL_API=$(sed -n '/Building Open Bank Project API/,/OBP Http4s Runner/{/Tests: succeeded/p;}' "${DETAIL_LOG}" 2>/dev/null | grep -oP "succeeded \K\d+" | tail -1)
FINAL_COUNTS=""
[ -n "$FINAL_COMMONS" ] && FINAL_COUNTS="commons:+${FINAL_COMMONS}"
[ -n "$FINAL_API" ] && FINAL_COUNTS="${FINAL_COUNTS:+${FINAL_COUNTS} }api:+${FINAL_API}"
update_terminal_title "Complete" "$FINAL_ELAPSED" "$FINAL_COUNTS" "" ""

################################################################################
# GENERATE SUMMARY
################################################################################

print_header "Test Results Summary"

# Extract test statistics (with UNKNOWN fallback if extraction fails)
TOTAL_TESTS=$(grep -E "Total number of tests run:|Tests run:" "${DETAIL_LOG}" | tail -1 | grep -oP '\d+' | head -1 || echo "UNKNOWN")
SUCCEEDED=$(grep -oP "succeeded \K\d+" "${DETAIL_LOG}" | tail -1 || echo "UNKNOWN")
FAILED=$(grep -oP "failed \K\d+" "${DETAIL_LOG}" | tail -1 || echo "UNKNOWN")
ERRORS=$(grep -oP "errors \K\d+" "${DETAIL_LOG}" | tail -1 || echo "UNKNOWN")
SKIPPED=$(grep -oP "(skipped|ignored) \K\d+" "${DETAIL_LOG}" | tail -1 || echo "UNKNOWN")
WARNINGS=$(grep -c "WARNING" "${DETAIL_LOG}" || echo "UNKNOWN")

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
log_message "  ${YELLOW}Warnings:    ${WARNINGS}${NC}"
log_message ""

# Analyze and display warning factors if warnings exist
if [ "${WARNINGS}" != "0" ] && [ "${WARNINGS}" != "UNKNOWN" ]; then
    warning_analysis=$(analyze_warnings "${DETAIL_LOG}")
    display_warning_factors "${warning_analysis}" 10
    log_message ""
fi

# Show failed tests if any
if [ "${FAILED}" != "0" ] || [ "${ERRORS}" != "0" ]; then
    log_message "${RED}Failed Tests:${NC}"
    grep -A 5 "FAILED\|ERROR" "${DETAIL_LOG}" | head -50 >> "${SUMMARY_LOG}"
    log_message ""
fi

################################################################################
# FINAL RESULT
################################################################################

print_header "Test Run Complete"

if [ "${BUILD_STATUS}" = "SUCCESS" ] && [ "${FAILED}" = "0" ] && [ "${ERRORS}" = "0" ]; then
    log_message "${GREEN}[PASS] All tests passed!${NC}"
    EXIT_CODE=0
else
    log_message "${RED}[FAIL] Tests failed${NC}"
    EXIT_CODE=1
fi

log_message ""
log_message "Logs saved to:"
log_message "  ${DETAIL_LOG}"
log_message "  ${SUMMARY_LOG}"
echo ""

exit ${EXIT_CODE}
