#!/bin/bash
# Generate changelog entries from Git commits using Conventional Commits
# Extracts commits since last tag and categorizes them

set -e

BLUE='\033[0;34m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${BLUE}Generating changelog from commits...${NC}"
echo ""

# Get the last tag
LAST_TAG=$(git describe --tags --abbrev=0 2>/dev/null || echo "")

if [ -z "$LAST_TAG" ]; then
    echo -e "${YELLOW}No previous tag found. Using all commits.${NC}"
    COMMIT_RANGE="HEAD"
else
    echo "Last tag: $LAST_TAG"
    COMMIT_RANGE="$LAST_TAG..HEAD"
fi

echo ""
echo "Analyzing commits in range: $COMMIT_RANGE"
echo ""

# Temporary files for categorized commits
ADDED=$(mktemp)
CHANGED=$(mktemp)
FIXED=$(mktemp)
DEPRECATED=$(mktemp)
REMOVED=$(mktemp)
SECURITY=$(mktemp)
BREAKING=$(mktemp)

# Parse commits and categorize based on conventional commit format
git log $COMMIT_RANGE --pretty=format:"%s" | while read -r commit; do
    # Extract type and description
    if [[ $commit =~ ^([a-z]+)(\(.+\))?!?:(.+)$ ]]; then
        type="${BASH_REMATCH[1]}"
        scope="${BASH_REMATCH[2]}"
        desc="${BASH_REMATCH[3]}"

        # Trim whitespace
        desc=$(echo "$desc" | xargs)

        # Check for breaking change
        if [[ $commit =~ ! ]] || [[ $commit =~ BREAKING[[:space:]]CHANGE ]]; then
            echo "- **BREAKING**: $desc" >> "$BREAKING"
        fi

        # Categorize by type
        case "$type" in
            feat|feature)
                echo "- $desc" >> "$ADDED"
                ;;
            fix)
                echo "- $desc" >> "$FIXED"
                ;;
            docs)
                echo "- Documentation: $desc" >> "$CHANGED"
                ;;
            refactor|perf|style)
                echo "- $desc" >> "$CHANGED"
                ;;
            chore|build|ci)
                # Usually skip these
                ;;
            deprecate)
                echo "- $desc" >> "$DEPRECATED"
                ;;
            remove)
                echo "- $desc" >> "$REMOVED"
                ;;
            security|sec)
                echo "- $desc" >> "$SECURITY"
                ;;
        esac
    fi
done

# Generate changelog output
echo "## [Unreleased]"
echo ""

if [ -s "$BREAKING" ]; then
    echo "### ⚠️ Breaking Changes"
    echo ""
    cat "$BREAKING"
    echo ""
fi

if [ -s "$ADDED" ]; then
    echo "### Added"
    echo ""
    cat "$ADDED"
    echo ""
fi

if [ -s "$CHANGED" ]; then
    echo "### Changed"
    echo ""
    cat "$CHANGED"
    echo ""
fi

if [ -s "$DEPRECATED" ]; then
    echo "### Deprecated"
    echo ""
    cat "$DEPRECATED"
    echo ""
fi

if [ -s "$REMOVED" ]; then
    echo "### Removed"
    echo ""
    cat "$REMOVED"
    echo ""
fi

if [ -s "$FIXED" ]; then
    echo "### Fixed"
    echo ""
    cat "$FIXED"
    echo ""
fi

if [ -s "$SECURITY" ]; then
    echo "### Security"
    echo ""
    cat "$SECURITY"
    echo ""
fi

# Cleanup
rm -f "$ADDED" "$CHANGED" "$FIXED" "$DEPRECATED" "$REMOVED" "$SECURITY" "$BREAKING"

echo ""
echo -e "${GREEN}✓ Changelog generated${NC}"
echo ""
echo "Copy the output above and paste into CHANGELOG.md under [Unreleased]"
echo ""
echo "Or save to file:"
echo "  $0 > changelog-draft.md"
echo ""
