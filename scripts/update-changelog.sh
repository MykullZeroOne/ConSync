#!/bin/bash
# Update CHANGELOG.md with new version
# Moves [Unreleased] changes to new version section

set -e

BLUE='\033[0;34m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

if [ -z "$1" ]; then
    echo -e "${RED}Error: Version required${NC}"
    echo "Usage: $0 <version>"
    echo "Example: $0 1.0.0"
    exit 1
fi

VERSION=$1
DATE=$(date +%Y-%m-%d)
CHANGELOG="CHANGELOG.md"

if [ ! -f "$CHANGELOG" ]; then
    echo -e "${RED}Error: CHANGELOG.md not found${NC}"
    exit 1
fi

echo -e "${BLUE}Updating CHANGELOG.md for v$VERSION...${NC}"

# Create backup
cp "$CHANGELOG" "${CHANGELOG}.bak"

# Create temporary file
TEMP_FILE=$(mktemp)

# Process the changelog
awk -v ver="$VERSION" -v date="$DATE" '
    # Copy everything until we hit [Unreleased]
    /^## \[Unreleased\]/ {
        print $0
        print ""
        print "### Added"
        print ""
        print "### Changed"
        print ""
        print "### Deprecated"
        print ""
        print "### Removed"
        print ""
        print "### Fixed"
        print ""
        print "### Security"
        print ""
        print "## [" ver "] - " date
        in_unreleased = 1
        next
    }

    # When we hit the next version, stop copying unreleased content
    in_unreleased && /^## \[/ {
        in_unreleased = 0
    }

    # Print everything else
    { print }

    # Update version links at the end
    /^\[Unreleased\]:/ {
        sub(/compare\/.*\.\.\.HEAD/, "compare/v" ver "...HEAD")
    }

    # Add new version link after Unreleased link
    END {
        if (!found_version_link) {
            print "[" ver "]: https://github.com/yourusername/consync/releases/tag/v" ver
        }
    }
' "$CHANGELOG" > "$TEMP_FILE"

# Replace original with updated version
mv "$TEMP_FILE" "$CHANGELOG"

echo -e "${GREEN}✓ CHANGELOG.md updated${NC}"
echo ""
echo "Updated sections:"
echo "  - [Unreleased] → Reset for new changes"
echo "  - [$VERSION] → Added with date: $DATE"
echo ""
echo "Backup saved: ${CHANGELOG}.bak"
echo ""
echo "Next steps:"
echo "  1. Edit CHANGELOG.md to add your changes under [$VERSION]"
echo "  2. Review the changes"
echo "  3. Commit: git add CHANGELOG.md && git commit -m 'Update CHANGELOG for v$VERSION'"
echo ""
