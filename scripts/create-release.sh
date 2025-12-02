#!/bin/bash
# Create and push a ConSync release
# This commits version changes, creates a tag, and pushes to GitHub

set -e

BLUE='\033[0;34m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

if [ -z "$1" ]; then
    echo -e "${RED}Error: Version number required${NC}"
    echo "Usage: $0 <version>"
    echo "Example: $0 1.0.0"
    exit 1
fi

VERSION=$1

echo -e "${BLUE}╔════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║        ConSync Release Creator        ║${NC}"
echo -e "${BLUE}╚════════════════════════════════════════╝${NC}"
echo ""
echo "Version: v$VERSION"
echo ""

# Check if version tag already exists
if git rev-parse "v$VERSION" >/dev/null 2>&1; then
    echo -e "${RED}Error: Tag v$VERSION already exists${NC}"
    exit 1
fi

# First prepare the release (updates version numbers)
echo -e "${BLUE}Preparing release...${NC}"
./scripts/prepare-release.sh $VERSION

echo ""
echo -e "${BLUE}Creating release commit and tag...${NC}"

# Commit version changes
git add -A
git commit -m "Release v$VERSION

- Updated version to $VERSION in all package manifests
- Built and tested release artifacts
"

echo -e "${GREEN}✓ Created release commit${NC}"

# Create annotated tag
git tag -a "v$VERSION" -m "Release v$VERSION

ConSync $VERSION

See CHANGELOG.md for details.
"

echo -e "${GREEN}✓ Created tag v$VERSION${NC}"

echo ""
echo -e "${YELLOW}Ready to push!${NC}"
echo ""
echo "This will:"
echo "  - Push commits to origin/main"
echo "  - Push tag v$VERSION"
echo "  - Trigger GitHub Actions to build and publish release"
echo ""
read -p "Push now? [y/N]: " push

if [[ "$push" =~ ^[Yy]$ ]]; then
    echo ""
    echo -e "${BLUE}Pushing to GitHub...${NC}"

    git push origin main
    echo -e "${GREEN}✓ Pushed commits${NC}"

    git push origin "v$VERSION"
    echo -e "${GREEN}✓ Pushed tag${NC}"

    echo ""
    echo -e "${GREEN}╔════════════════════════════════════════╗${NC}"
    echo -e "${GREEN}║        Release Created!                ║${NC}"
    echo -e "${GREEN}╚════════════════════════════════════════╝${NC}"
    echo ""
    echo "GitHub Actions is now building the release."
    echo ""
    echo "Monitor progress:"
    echo "  https://github.com/$(git config --get remote.origin.url | sed 's/.*://;s/.git$//')/actions"
    echo ""
    echo "Release will be available at:"
    echo "  https://github.com/$(git config --get remote.origin.url | sed 's/.*://;s/.git$//')/releases/tag/v$VERSION"
    echo ""
else
    echo ""
    echo -e "${YELLOW}Push cancelled${NC}"
    echo ""
    echo "To push later, run:"
    echo "  git push origin main"
    echo "  git push origin v$VERSION"
    echo ""
fi
