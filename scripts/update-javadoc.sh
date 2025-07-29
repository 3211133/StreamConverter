#!/bin/bash
# Script to safely update Javadoc with conflict resolution

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

print_status() {
    echo -e "${GREEN}✓${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}⚠${NC} $1"
}

print_error() {
    echo -e "${RED}✗${NC} $1"
}

print_info() {
    echo -e "${BLUE}ℹ${NC} $1"
}

echo "🔧 Javadoc Update Script"
echo "======================="

# Check if we're in the right directory
if [ ! -f "build.gradle.kts" ]; then
    print_error "Not in project root directory"
    exit 1
fi

# Check for uncommitted changes in docs/javadoc
if ! git diff --quiet docs/javadoc/ 2>/dev/null; then
    print_warning "Uncommitted changes detected in docs/javadoc/"
    read -p "Do you want to continue? This will overwrite local Javadoc changes. [y/N]: " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        print_info "Operation cancelled by user"
        exit 0
    fi
fi

# Backup current javadoc if it exists
if [ -d "docs/javadoc" ]; then
    print_info "Creating backup of current Javadoc..."
    backup_dir="docs/javadoc.backup.$(date +%Y%m%d_%H%M%S)"
    cp -r docs/javadoc "$backup_dir"
    print_status "Backup created at: $backup_dir"
fi

# Pull latest changes from remote
print_info "Fetching latest changes from remote..."
git fetch origin

# Check if docs/javadoc has conflicts with remote
current_branch=$(git branch --show-current)
if git diff --quiet HEAD origin/$current_branch -- docs/javadoc/ 2>/dev/null; then
    print_status "No conflicts with remote Javadoc"
else
    print_warning "Javadoc differs from remote. Merging remote changes first..."
    
    # Try to merge only javadoc changes
    if git merge origin/$current_branch --no-commit --no-ff -- docs/javadoc/ 2>/dev/null; then
        git commit -m "docs: merge remote Javadoc changes" 2>/dev/null || true
        print_status "Successfully merged remote Javadoc changes"
    else
        print_warning "Auto-merge failed. Using remote version as base..."
        git checkout origin/$current_branch -- docs/javadoc/ 2>/dev/null || true
    fi
fi

# Generate fresh Javadoc
print_info "Generating fresh Javadoc..."
if ./gradlew clean javadoc; then
    print_status "Javadoc generation completed successfully"
else
    print_error "Javadoc generation failed"
    
    # Restore backup if available
    if [ -d "$backup_dir" ]; then
        print_info "Restoring backup..."
        rm -rf docs/javadoc
        mv "$backup_dir" docs/javadoc
        print_status "Backup restored"
    fi
    exit 1
fi

# Check if there are any changes
if git diff --quiet docs/javadoc/; then
    print_info "No changes in generated Javadoc"
    
    # Clean up backup if no changes
    if [ -d "$backup_dir" ]; then
        rm -rf "$backup_dir"
        print_info "Backup removed (no changes detected)"
    fi
else
    print_status "Javadoc updated with new changes"
    
    # Show summary of changes
    added=$(git diff --name-status docs/javadoc/ | grep "^A" | wc -l)
    modified=$(git diff --name-status docs/javadoc/ | grep "^M" | wc -l)
    deleted=$(git diff --name-status docs/javadoc/ | grep "^D" | wc -l)
    
    echo "📊 Change Summary:"
    echo "   Added: $added files"
    echo "   Modified: $modified files" 
    echo "   Deleted: $deleted files"
    
    # Ask if user wants to commit
    read -p "Do you want to commit these changes? [y/N]: " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        git add docs/javadoc/
        git commit <<EOF
docs: update Javadoc

🔄 Updated Javadoc documentation

Generated from latest source code changes.
See individual commits for specific API changes.

Co-Authored-By: update-javadoc.sh <noreply@local>
EOF
        print_status "Javadoc changes committed successfully"
        
        # Clean up backup after successful commit
        if [ -d "$backup_dir" ]; then
            rm -rf "$backup_dir"
            print_info "Backup cleaned up"
        fi
        
        # Ask about pushing
        read -p "Do you want to push the changes? [y/N]: " -n 1 -r
        echo
        if [[ $REPLY =~ ^[Yy]$ ]]; then
            git push
            print_status "Changes pushed to remote"
        fi
    else
        print_info "Changes staged but not committed"
        print_info "Run 'git add docs/javadoc/ && git commit' to commit manually"
    fi
fi

echo ""
print_status "Javadoc update completed!"

# Show next steps
echo ""
echo "📋 Next Steps:"
echo "   • Review changes: git diff --cached docs/javadoc/ (if staged)"
echo "   • Manual commit: git add docs/javadoc/ && git commit"
echo "   • View docs: open docs/javadoc/index.html"
echo "   • Backup location: $backup_dir (if created)"