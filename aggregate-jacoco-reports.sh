#!/bin/bash

# JaCoCo Report Aggregation Script
# Aggregates coverage reports from all modules into a consolidated CSV format

set -e  # Exit on any error

# Script configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$SCRIPT_DIR"
OUTPUT_FILE="$PROJECT_ROOT/docs/reports/jacoco/coverage-history.csv"
JAVA_CLASS="com.streamConverter.test.JacocoReportAggregator"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Logging functions
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Function to check if JaCoCo reports exist
check_jacoco_reports() {
    log_info "Checking for existing JaCoCo reports..."
    
    local modules=("streamconverter-core" "streamconverter-web" "streamconverter-examples" "streamconverter-tools")
    local reports_found=0
    
    for module in "${modules[@]}"; do
        local report_path="$PROJECT_ROOT/$module/build/reports/jacoco/test/jacocoTestReport.xml"
        if [[ -f "$report_path" ]]; then
            log_success "Found JaCoCo report for $module"
            reports_found=$((reports_found + 1))
        else
            log_warning "No JaCoCo report found for $module (may be disabled on this platform)"
        fi
    done
    
    if [[ $reports_found -eq 0 ]]; then
        log_warning "No JaCoCo reports found. Run './gradlew test' first to generate reports."
        return 1
    fi
    
    log_info "Found $reports_found JaCoCo report(s)"
    return 0
}

# Function to compile Java classes if needed
ensure_java_classes() {
    log_info "Ensuring Java classes are compiled..."
    
    local class_file="$PROJECT_ROOT/streamconverter-core/build/classes/java/main/com/streamConverter/test/JacocoReportAggregator.class"
    
    if [[ ! -f "$class_file" ]]; then
        log_info "Compiling Java classes..."
        cd "$PROJECT_ROOT"
        if ! ./gradlew compileJava; then
            log_error "Failed to compile Java classes"
            exit 1
        fi
    else
        log_success "Java classes already compiled"
    fi
}

# Function to create output directory
create_output_directory() {
    local output_dir
    output_dir="$(dirname "$OUTPUT_FILE")"
    
    if [[ ! -d "$output_dir" ]]; then
        log_info "Creating output directory: $output_dir"
        mkdir -p "$output_dir"
    fi
}

# Function to run the aggregation
run_aggregation() {
    log_info "Running JaCoCo report aggregation..."
    
    # Set up classpath
    local classpath="$PROJECT_ROOT/streamconverter-core/build/classes/java/main"
    
    # Add any required dependencies to classpath if needed
    # (Currently JacocoReportAggregator only uses standard Java libraries)
    
    cd "$PROJECT_ROOT"
    
    if java -cp "$classpath" "$JAVA_CLASS" "$PROJECT_ROOT" "$OUTPUT_FILE"; then
        log_success "JaCoCo reports aggregated successfully"
        return 0
    else
        log_error "Failed to aggregate JaCoCo reports"
        return 1
    fi
}

# Function to display summary
display_summary() {
    if [[ -f "$OUTPUT_FILE" ]]; then
        log_info "Coverage report summary:"
        echo ""
        
        # Display the header and last few lines
        echo "Header:"
        head -n 1 "$OUTPUT_FILE"
        echo ""
        
        echo "Latest entries:"
        tail -n 5 "$OUTPUT_FILE"
        echo ""
        
        local total_lines
        total_lines=$(wc -l < "$OUTPUT_FILE")
        log_info "Total entries in report: $((total_lines - 1))"
        log_info "Report location: $OUTPUT_FILE"
    fi
}

# Function to show usage
show_usage() {
    echo "Usage: $0 [OPTIONS]"
    echo ""
    echo "Aggregates JaCoCo coverage reports from all modules into a consolidated CSV format."
    echo ""
    echo "Options:"
    echo "  -h, --help     Show this help message"
    echo "  -f, --force    Force aggregation even if no recent reports found"
    echo "  -v, --verbose  Enable verbose output"
    echo ""
    echo "Environment Variables:"
    echo "  OUTPUT_FILE    Override default output file location"
    echo ""
    echo "Examples:"
    echo "  $0                    # Basic aggregation"
    echo "  $0 --force           # Force aggregation"
    echo "  OUTPUT_FILE=custom.csv $0  # Custom output location"
}

# Main function
main() {
    local force_mode=false
    local verbose_mode=false
    
    # Parse command line arguments
    while [[ $# -gt 0 ]]; do
        case $1 in
            -h|--help)
                show_usage
                exit 0
                ;;
            -f|--force)
                force_mode=true
                shift
                ;;
            -v|--verbose)
                verbose_mode=true
                shift
                ;;
            *)
                log_error "Unknown option: $1"
                show_usage
                exit 1
                ;;
        esac
    done
    
    # Override output file if environment variable is set
    if [[ -n "${OUTPUT_FILE_OVERRIDE:-}" ]]; then
        OUTPUT_FILE="$OUTPUT_FILE_OVERRIDE"
    fi
    
    log_info "JaCoCo Report Aggregation Script"
    log_info "Project root: $PROJECT_ROOT"
    log_info "Output file: $OUTPUT_FILE"
    echo ""
    
    # Check for JaCoCo reports unless force mode is enabled
    if [[ "$force_mode" != true ]]; then
        if ! check_jacoco_reports; then
            if [[ "$verbose_mode" == true ]]; then
                log_info "Use --force to aggregate anyway (will create N/A entries for missing reports)"
            fi
            exit 1
        fi
    else
        log_info "Force mode enabled - will aggregate regardless of report availability"
    fi
    
    # Ensure prerequisites
    ensure_java_classes
    create_output_directory
    
    # Run the aggregation
    if run_aggregation; then
        display_summary
        log_success "JaCoCo report aggregation completed successfully!"
        exit 0
    else
        log_error "JaCoCo report aggregation failed!"
        exit 1
    fi
}

# Run main function with all arguments
main "$@"