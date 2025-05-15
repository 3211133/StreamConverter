#!/bin/sh

# Set up Git hooks
echo "Setting up Git hooks..."
git config core.hooksPath .githooks

# Make commit checks script executable
chmod +x scripts/commit-checks.sh

# Check if Node.js is installed
if ! command -v node >/dev/null 2>&1; then
  echo "Warning: Node.js is not installed. You will need Node.js to run commitlint."
  echo "Please install Node.js from https://nodejs.org/"
  exit 1
fi

# Install commitlint dependencies
echo "Installing commitlint dependencies..."
npm install --save-dev @commitlint/cli @commitlint/config-conventional

echo "Git hooks setup complete!"
echo "Your commits will now be checked for conventional commit format and code style."
echo "See CONTRIBUTING.md for more information on commit message format."
echo "All commit-time checks are now consolidated in scripts/commit-checks.sh"
