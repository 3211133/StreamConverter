#!/bin/bash

# Test Failure Analyzer Script
# Analyzes JUnit XML test reports and provides detailed failure information

set -euo pipefail

# Default paths
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CORE_DIR="${SCRIPT_DIR}/streamconverter-core"
TEST_RESULTS_DIR="${CORE_DIR}/build/test-results/test"
BUILD_DIR="${CORE_DIR}/build"
CLASSES_DIR="${BUILD_DIR}/classes/java/main"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    local color=$1
    local message=$2
    echo -e "${color}${message}${NC}"
}

print_usage() {
    echo "Usage: $0 [options]"
    echo ""
    echo "Options:"
    echo "  -p, --path PATH     Specify test results directory (default: ${TEST_RESULTS_DIR})"
    echo "  -c, --compile       Compile the analyzer before running"
    echo "  -r, --run-tests     Run tests first, then analyze results"
    echo "  -h, --help          Show this help message"
    echo ""
    echo "Examples:"
    echo "  $0                                    # Analyze existing test results"
    echo "  $0 -r                                 # Run tests and analyze failures"
    echo "  $0 -p build/test-results/test         # Analyze specific directory"
    echo "  $0 -c -r                              # Compile, run tests, and analyze"
}

# Parse command line arguments
COMPILE_FIRST=false
RUN_TESTS_FIRST=false
CUSTOM_PATH=""

while [[ $# -gt 0 ]]; do
    case $1 in
        -p|--path)
            CUSTOM_PATH="$2"
            shift 2
            ;;
        -c|--compile)
            COMPILE_FIRST=true
            shift
            ;;
        -r|--run-tests)
            RUN_TESTS_FIRST=true
            shift
            ;;
        -h|--help)
            print_usage
            exit 0
            ;;
        *)
            echo "Unknown option: $1"
            print_usage
            exit 1
            ;;
    esac
done

# Use custom path if provided
if [[ -n "$CUSTOM_PATH" ]]; then
    TEST_RESULTS_DIR="$CUSTOM_PATH"
fi

print_status "$YELLOW" "=== Test Failure Analyzer ==="

# Change to core directory
cd "$CORE_DIR"

# Compile if requested
if [[ "$COMPILE_FIRST" == true ]]; then
    print_status "$YELLOW" "Compiling project..."
    if ./gradlew compileJava; then
        print_status "$GREEN" "✓ Compilation successful"
    else
        print_status "$RED" "✗ Compilation failed"
        exit 1
    fi
fi

# Run tests if requested
if [[ "$RUN_TESTS_FIRST" == true ]]; then
    print_status "$YELLOW" "Running tests..."
    # Run tests and capture exit code (don't exit on test failure)
    set +e
    ./gradlew test
    TEST_EXIT_CODE=$?
    set -e
    
    if [[ $TEST_EXIT_CODE -eq 0 ]]; then
        print_status "$GREEN" "✓ All tests passed"
    else
        print_status "$YELLOW" "⚠ Some tests failed - analyzing results..."
    fi
fi

# Check if test results exist
if [[ ! -d "$TEST_RESULTS_DIR" ]]; then
    print_status "$RED" "✗ Test results directory not found: $TEST_RESULTS_DIR"
    print_status "$YELLOW" "Run tests first with: ./gradlew test"
    exit 1
fi

# Check if there are any XML files
XML_COUNT=$(find "$TEST_RESULTS_DIR" -name "*.xml" | wc -l)
if [[ $XML_COUNT -eq 0 ]]; then
    print_status "$RED" "✗ No XML test result files found in $TEST_RESULTS_DIR"
    exit 1
fi

print_status "$YELLOW" "Analyzing test results from: $TEST_RESULTS_DIR"
print_status "$YELLOW" "Found $XML_COUNT XML test result files"

# Compile and run the analyzer
print_status "$YELLOW" "Compiling TestFailureAnalyzer..."
if ! javac -cp "$CLASSES_DIR" \
    -d "$BUILD_DIR/tmp" \
    src/main/java/com/streamConverter/test/TestFailureAnalyzer.java 2>/dev/null; then
    
    # If compilation fails, try with gradle
    print_status "$YELLOW" "Compiling with Gradle..."
    ./gradlew compileJava
fi

# Run the analyzer
print_status "$YELLOW" "Running failure analysis..."
echo ""

# Set classpath and run
export CLASSPATH="$CLASSES_DIR:$BUILD_DIR/tmp"
if java com.streamConverter.test.TestFailureAnalyzer "$TEST_RESULTS_DIR"; then
    print_status "$GREEN" "✓ Analysis completed successfully"
    exit 0
else
    print_status "$RED" "✗ Test failures detected"
    exit 1
fi