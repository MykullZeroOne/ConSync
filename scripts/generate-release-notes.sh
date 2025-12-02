#!/bin/bash
# Generate release notes from CHANGELOG.md
# Extracts the section for a specific version

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
CHANGELOG="CHANGELOG.md"

if [ ! -f "$CHANGELOG" ]; then
    echo -e "${RED}Error: CHANGELOG.md not found${NC}"
    exit 1
fi

echo -e "${BLUE}Extracting release notes for v$VERSION...${NC}"

# Extract section between version headers
# Matches from ## [VERSION] to the next ## [
RELEASE_NOTES=$(awk -v ver="$VERSION" '
    /^## \['"$VERSION"'\]/ { found=1; next }
    found && /^## \[/ { exit }
    found { print }
' "$CHANGELOG")

if [ -z "$RELEASE_NOTES" ]; then
    echo -e "${YELLOW}Warning: No release notes found for version $VERSION${NC}"
    echo ""
    echo "Please add release notes to CHANGELOG.md:"
    echo ""
    echo "## [$VERSION] - $(date +%Y-%m-%d)"
    echo ""
    echo "### Added"
    echo "- Feature 1"
    echo ""
    exit 1
fi

# Create formatted release notes
cat > release-notes.md << EOF
# ConSync v$VERSION

$RELEASE_NOTES

## Installation

### macOS/Linux (Homebrew)
\`\`\`bash
brew tap yourusername/consync
brew install consync
\`\`\`

### Windows (Scoop)
\`\`\`powershell
scoop bucket add consync https://github.com/yourusername/scoop-consync
scoop install consync
\`\`\`

### Windows (Chocolatey)
\`\`\`powershell
choco install consync
\`\`\`

### Direct Download

Download the appropriate package for your platform:
- **macOS/Linux**: [\`consync-$VERSION-unix.tar.gz\`](https://github.com/yourusername/consync/releases/download/v$VERSION/consync-$VERSION-unix.tar.gz)
- **Windows**: [\`consync-$VERSION-windows.zip\`](https://github.com/yourusername/consync/releases/download/v$VERSION/consync-$VERSION-windows.zip)
- **Universal JAR**: [\`consync-$VERSION.jar\`](https://github.com/yourusername/consync/releases/download/v$VERSION/consync-$VERSION.jar)

All downloads include SHA256 checksums for verification.

## Documentation

- 📖 [Installation Guide](https://github.com/yourusername/consync/blob/main/docs/INSTALL_USERS.md)
- 📚 [Full Documentation](https://github.com/yourusername/consync/blob/main/README.md)
- 🎯 [Examples](https://github.com/yourusername/consync/tree/main/examples)

## Checksums

Verify your download:

\`\`\`bash
# Download checksum file
wget https://github.com/yourusername/consync/releases/download/v$VERSION/consync-$VERSION-unix.tar.gz.sha256

# Verify
sha256sum -c consync-$VERSION-unix.tar.gz.sha256
\`\`\`
EOF

echo -e "${GREEN}✓ Release notes generated: release-notes.md${NC}"
echo ""
cat release-notes.md
