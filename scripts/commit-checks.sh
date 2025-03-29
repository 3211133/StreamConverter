#!/bin/sh
# Consolidated script for commit-time checks
# This script combines code style and commit message format checks

# Function to display error message and exit
error_exit() {
  echo "ERROR: $1" >&2
  exit 1
}

# Function to display success message
success_message() {
  echo "SUCCESS: $1"
}

# Function to check code style using Spotless
check_code_style() {
  echo "Running Spotless check..."
  ./gradlew spotlessCheck
  
  if [ $? -ne 0 ]; then
    error_exit "Spotless check failed. Run './gradlew spotlessApply' to fix code style issues."
  fi
  
  success_message "Code style check passed!"
}

# Function to check commit message format using commitlint
check_commit_message() {
  local commit_msg_file=$1
  
  echo "Checking commit message format..."
  
  if [ -z "$commit_msg_file" ]; then
    error_exit "No commit message file provided."
  fi
  
  if [ ! -f "$commit_msg_file" ]; then
    error_exit "Commit message file not found: $commit_msg_file"
  fi
  
  # Use --edit for commit-msg hook, or pipe content for pre-commit hook
  if [ "$2" = "--edit" ]; then
    npx commitlint --edit "$commit_msg_file"
  else
    cat "$commit_msg_file" | npx commitlint
  fi
  
  if [ $? -ne 0 ]; then
    error_exit "Commit message does not follow the conventional commit format. See CONTRIBUTING.md for more information."
  fi
  
  success_message "Commit message format is valid!"
}

# Main execution logic
main() {
  local mode=$1
  local commit_msg_file=$2
  
  case "$mode" in
    "pre-commit")
      # Run code style check
      check_code_style
      
      # If commit message file is provided, check it too
      if [ -n "$commit_msg_file" ]; then
        check_commit_message "$commit_msg_file"
      fi
      ;;
      
    "commit-msg")
      # Check commit message format
      check_commit_message "$commit_msg_file" "--edit"
      ;;
      
    "all")
      # Run all checks
      check_code_style
      
      if [ -n "$commit_msg_file" ]; then
        check_commit_message "$commit_msg_file" "--edit"
      else
        echo "Skipping commit message check (no file provided)."
      fi
      ;;
      
    *)
      error_exit "Unknown mode: $mode. Use 'pre-commit', 'commit-msg', or 'all'."
      ;;
  esac
  
  echo "All commit checks completed successfully!"
}

# Execute main function with all arguments
main "$@"
