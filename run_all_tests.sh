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
# Usage:
#   ./run_all_tests.sh              - Run full test suite
#   ./run_all_tests.sh --summary-only - Regenerate summary from existing log
################################################################################

set -e

################################################################################
# PARSE COMMAND LINE ARGUMENTS
################################################################################

SUMMARY_ONLY=false
if [ "$1" = "--summary-only" ]; then
    SUMMARY_ONLY=true
fi

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

mkdir -p "${LOG_DIR}"

# If summary-only mode, skip to summary generation
if [ "$SUMMARY_ONLY" = true ]; then
    if [ ! -f "${DETAIL_LOG}" ]; then
        echo "ERROR: No log file found at ${DETAIL_LOG}"
        echo "Please run tests first without --summary-only flag"
        exit 1
    fi
    echo "Regenerating summary from existing log: ${DETAIL_LOG}"
    # Skip cleanup and jump to summary generation
    START_TIME=0
    END_TIME=0
    DURATION=0
    DURATION_MIN=0
    DURATION_SEC=0
else
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
fi  # End of if [ "$SUMMARY_ONLY" = true ]

################################################################################
# HELPER FUNCTIONS
################################################################################

# Log message to terminal and summary file
log_message() {
    echo "$1"
    echo "[$(date +"%Y-%m-%d %H:%M:%S")] $1" >> "${SUMMARY_LOG}"
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
        log_message "  No detailed warning analysis available"
        return
    fi

    local total_warning_types=$(wc -l < "${analysis_file}")
    local displayed=0

    log_message "Top Warning Factors:"
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
        printf "  %4d x %s\n" "$count" "$message" | tee -a "${SUMMARY_LOG}" > /dev/tty

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
# GENERATE SUMMARY FUNCTION (DRY)
################################################################################

generate_summary() {
    local detail_log="$1"
    local summary_log="$2"
    local start_time="${3:-0}"
    local end_time="${4:-0}"

    # Calculate duration
    local duration=$((end_time - start_time))
    local duration_min=$((duration / 60))
    local duration_sec=$((duration % 60))

    # If no timing info (summary-only mode), extract from log
    if [ $duration -eq 0 ] && grep -q "Total time:" "$detail_log"; then
        local time_str=$(grep "Total time:" "$detail_log" | tail -1)
        duration_min=$(echo "$time_str" | grep -oP '\d+(?= min)' || echo "0")
        duration_sec=$(echo "$time_str" | grep -oP '\d+(?=\.\d+ s)' || echo "0")
    fi

    print_header "Test Results Summary"

    # Extract test statistics from ScalaTest output (with UNKNOWN fallback if extraction fails)
    # ScalaTest outputs across multiple lines:
    #   Run completed in X seconds.
    #   Total number of tests run: N
    #   Suites: completed M, aborted 0
    #   Tests: succeeded N, failed 0, canceled 0, ignored 0, pending 0
    #   All tests passed.
    # We need to extract the stats from the last test run (in case there are multiple modules)
    SCALATEST_SECTION=$(grep -A 4 "Run completed" "${detail_log}" | tail -5)
    if [ -n "$SCALATEST_SECTION" ]; then
        TOTAL_TESTS=$(echo "$SCALATEST_SECTION" | grep -oP "Total number of tests run: \K\d+" || echo "UNKNOWN")
        SUCCEEDED=$(echo "$SCALATEST_SECTION" | grep -oP "succeeded \K\d+" || echo "UNKNOWN")
        FAILED=$(echo "$SCALATEST_SECTION" | grep -oP "failed \K\d+" || echo "UNKNOWN")
        ERRORS=$(echo "$SCALATEST_SECTION" | grep -oP "errors \K\d+" || echo "0")
        SKIPPED=$(echo "$SCALATEST_SECTION" | grep -oP "ignored \K\d+" || echo "UNKNOWN")
    else
        TOTAL_TESTS="UNKNOWN"
        SUCCEEDED="UNKNOWN"
        FAILED="UNKNOWN"
        ERRORS="0"
        SKIPPED="UNKNOWN"
    fi
    WARNINGS=$(grep -c "WARNING" "${detail_log}" || echo "UNKNOWN")

    # Determine build status
    if grep -q "BUILD SUCCESS" "${detail_log}"; then
        BUILD_STATUS="SUCCESS"
        BUILD_COLOR=""
    elif grep -q "BUILD FAILURE" "${detail_log}"; then
        BUILD_STATUS="FAILURE"
        BUILD_COLOR=""
    else
        BUILD_STATUS="UNKNOWN"
        BUILD_COLOR=""
    fi

    # Print summary
    log_message "Test Run Summary"
    log_message "================"
    log_message "Timestamp:     $(date)"
    log_message "Duration:      ${duration_min}m ${duration_sec}s"
    log_message "Build Status:  ${BUILD_STATUS}"
    log_message ""
    log_message "Test Statistics:"
    log_message "  Total:       ${TOTAL_TESTS}"
    log_message "  Succeeded:   ${SUCCEEDED}"
    log_message "  Failed:      ${FAILED}"
    log_message "  Errors:      ${ERRORS}"
    log_message "  Skipped:     ${SKIPPED}"
    log_message "  Warnings:    ${WARNINGS}"
    log_message ""

    # Analyze and display warning factors if warnings exist
    if [ "${WARNINGS}" != "0" ] && [ "${WARNINGS}" != "UNKNOWN" ]; then
        warning_analysis=$(analyze_warnings "${detail_log}")
        display_warning_factors "${warning_analysis}" 10
        log_message ""
    fi

    # Show failed tests if any (only actual test failures, not application ERROR logs)
    if [ "${FAILED}" != "0" ] && [ "${FAILED}" != "UNKNOWN" ]; then
        log_message "Failed Tests:"
        # Look for ScalaTest failure markers, not application ERROR logs
        grep -E "\*\*\* FAILED \*\*\*|\*\*\* RUN ABORTED \*\*\*" "${detail_log}" | head -50 >> "${summary_log}"
        log_message ""
    elif [ "${ERRORS}" != "0" ] && [ "${ERRORS}" != "UNKNOWN" ]; then
        log_message "Test Errors:"
        grep -E "\*\*\* FAILED \*\*\*|\*\*\* RUN ABORTED \*\*\*" "${detail_log}" | head -50 >> "${summary_log}"
        log_message ""
    fi

    # Final result
    print_header "Test Run Complete"

    if [ "${BUILD_STATUS}" = "SUCCESS" ] && [ "${FAILED}" = "0" ] && [ "${ERRORS}" = "0" ]; then
        log_message "[PASS] All tests passed!"
        return 0
    else
        log_message "[FAIL] Tests failed"
        return 1
    fi
}

################################################################################
# SUMMARY-ONLY MODE
################################################################################

if [ "$SUMMARY_ONLY" = true ]; then
    # Just regenerate the summary and exit
    rm -f "${SUMMARY_LOG}"
    if generate_summary "${DETAIL_LOG}" "${SUMMARY_LOG}" 0 0; then
        log_message ""
        log_message "Summary regenerated:"
        log_message "  ${SUMMARY_LOG}"
        exit 0
    else
        exit 1
    fi
fi

################################################################################
# START TEST RUN
################################################################################

set_terminal_style "Starting"

# Start the test run
print_header "OBP-API Test Suite"
log_message "Starting test run at $(date)"
log_message "Detail log: ${DETAIL_LOG}"
log_message "Summary log: ${SUMMARY_LOG}"
echo ""

# Set Maven options for tests
# The --add-opens flags tell Java 17 to allow Kryo serialization library to access
# the internal java.lang.invoke and java.lang modules, which fixes the InaccessibleObjectException
export MAVEN_OPTS="-Xss128m -Xms3G -Xmx6G -XX:MaxMetaspaceSize=2G --add-opens java.base/java.lang.invoke=ALL-UNNAMED --add-opens java.base/java.lang=ALL-UNNAMED"
log_message "Maven Options: ${MAVEN_OPTS}"
echo ""

# Ensure test properties file exists
PROPS_FILE="obp-api/src/main/resources/props/test.default.props"
PROPS_TEMPLATE="${PROPS_FILE}.template"

if [ -f "${PROPS_FILE}" ]; then
    log_message "[OK] Found test.default.props"
else
    log_message "[WARNING] test.default.props not found - creating from template"
    if [ -f "${PROPS_TEMPLATE}" ]; then
        cp "${PROPS_TEMPLATE}" "${PROPS_FILE}"
        log_message "[OK] Created test.default.props"
    else
        log_message "ERROR: ${PROPS_TEMPLATE} not found!"
        exit 1
    fi
fi

################################################################################
# CHECK AND CLEANUP TEST SERVER PORTS
# Port 8018 is used by the embedded Jetty test server (configured in test.default.props)
################################################################################

print_header "Checking Test Server Ports"
log_message "Checking if test server port 8018 is available..."

# Check if port 8018 is in use
if lsof -i :8018 >/dev/null 2>&1; then
    log_message "[WARNING] Port 8018 is in use - attempting to kill process"
    # Try to kill the process using the port
    PORT_PID=$(lsof -t -i :8018 2>/dev/null)
    if [ -n "$PORT_PID" ]; then
        kill -9 $PORT_PID 2>/dev/null || true
        sleep 2
        log_message "[OK] Killed process $PORT_PID using port 8018"
    fi
else
    log_message "[OK] Port 8018 is available"
fi

# Also check for any stale Java test processes
STALE_TEST_PROCS=$(ps aux | grep -E "TestServer|ScalaTest.*obp-api" | grep -v grep | awk '{print $2}' || true)
if [ -n "$STALE_TEST_PROCS" ]; then
    log_message "[WARNING] Found stale test processes - cleaning up"
    echo "$STALE_TEST_PROCS" | xargs kill -9 2>/dev/null || true
    sleep 2
    log_message "[OK] Cleaned up stale test processes"
else
    log_message "[OK] No stale test processes found"
fi

log_message ""

################################################################################
# CLEAN METRICS DATABASE
################################################################################

print_header "Cleaning Metrics Database"
log_message "Checking for test database files..."

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
        log_message "  [OK] Deleted: $dbfile"
    fi
done

if [ "$FOUND_FILES" = false ]; then
    log_message "No old test database files found"
fi

log_message ""

################################################################################
# RUN TESTS
################################################################################

print_header "Running Tests"
update_terminal_title "Building"
log_message "Executing: mvn clean test"
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
    RESULT_COLOR=""
else
    TEST_RESULT="FAILURE"
    RESULT_COLOR=""
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
# GENERATE SUMMARY (using DRY function)
################################################################################

if generate_summary "${DETAIL_LOG}" "${SUMMARY_LOG}" "$START_TIME" "$END_TIME"; then
    EXIT_CODE=0
else
    EXIT_CODE=1
fi

log_message ""
log_message "Logs saved to:"
log_message "  ${DETAIL_LOG}"
log_message "  ${SUMMARY_LOG}"
echo ""

exit ${EXIT_CODE}
