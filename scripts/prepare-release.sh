#!/bin/bash
# Prepare ConSync Release
# This script prepares a release by updating version numbers and creating a tag

set -e

BLUE='\033[0;34m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

# Check if version argument provided
if [ -z "$1" ]; then
    echo -e "${RED}Error: Version number required${NC}"
    echo "Usage: $0 <version>"
    echo "Example: $0 1.0.0"
    exit 1
fi

VERSION=$1

echo -e "${BLUE}╔════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║     ConSync Release Preparation       ║${NC}"
echo -e "${BLUE}╚════════════════════════════════════════╝${NC}"
echo ""
echo "Version: $VERSION"
echo ""

# Check if version follows semver
if ! [[ $VERSION =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
    echo -e "${RED}Error: Version must follow semantic versioning (e.g., 1.0.0)${NC}"
    exit 1
fi

# Check if git repo is clean
if ! git diff-index --quiet HEAD --; then
    echo -e "${YELLOW}Warning: You have uncommitted changes${NC}"
    read -p "Continue anyway? [y/N]: " continue
    if [[ ! "$continue" =~ ^[Yy]$ ]]; then
        exit 1
    fi
fi

# Check if on main branch
BRANCH=$(git rev-parse --abbrev-ref HEAD)
if [ "$BRANCH" != "main" ]; then
    echo -e "${YELLOW}Warning: Not on main branch (currently on: $BRANCH)${NC}"
    read -p "Continue anyway? [y/N]: " continue
    if [[ ! "$continue" =~ ^[Yy]$ ]]; then
        exit 1
    fi
fi

echo -e "${BLUE}Updating version in files...${NC}"

# Update pom.xml
if [ -f "pom.xml" ]; then
    mvn versions:set -DnewVersion=$VERSION
    mvn versions:commit
    echo -e "${GREEN}✓ Updated pom.xml${NC}"
fi

# Update Homebrew formula
if [ -f "Formula/consync.rb" ]; then
    sed -i.bak "s/version \".*\"/version \"$VERSION\"/" Formula/consync.rb
    sed -i.bak "s|/v.*/consync-.*-unix.tar.gz|/v$VERSION/consync-$VERSION-unix.tar.gz|" Formula/consync.rb
    rm Formula/consync.rb.bak
    echo -e "${GREEN}✓ Updated Homebrew formula${NC}"
fi

# Update Scoop manifest
if [ -f "scoop/consync.json" ]; then
    sed -i.bak "s/\"version\": \".*\"/\"version\": \"$VERSION\"/" scoop/consync.json
    sed -i.bak "s|/v.*/consync-.*-windows.zip|/v$VERSION/consync-$VERSION-windows.zip|" scoop/consync.json
    sed -i.bak "s|\"extract_dir\": \"consync-.*-windows\"|\"extract_dir\": \"consync-$VERSION-windows\"|" scoop/consync.json
    rm scoop/consync.json.bak
    echo -e "${GREEN}✓ Updated Scoop manifest${NC}"
fi

# Update Chocolatey nuspec
if [ -f "chocolatey/consync.nuspec" ]; then
    sed -i.bak "s|<version>.*</version>|<version>$VERSION</version>|" chocolatey/consync.nuspec
    sed -i.bak "s|/v.*/|/v$VERSION/|g" chocolatey/consync.nuspec
    rm chocolatey/consync.nuspec.bak
    echo -e "${GREEN}✓ Updated Chocolatey package${NC}"
fi

# Update Chocolatey install script
if [ -f "chocolatey/tools/chocolateyinstall.ps1" ]; then
    sed -i.bak "s/\$version = '.*'/\$version = '$VERSION'/" chocolatey/tools/chocolateyinstall.ps1
    rm chocolatey/tools/chocolateyinstall.ps1.bak
    echo -e "${GREEN}✓ Updated Chocolatey install script${NC}"
fi

echo ""
echo -e "${BLUE}Building project...${NC}"
mvn clean package
if [ $? -ne 0 ]; then
    echo -e "${RED}Build failed!${NC}"
    exit 1
fi
echo -e "${GREEN}✓ Build successful${NC}"

echo ""
echo -e "${BLUE}Running tests...${NC}"
mvn test
if [ $? -ne 0 ]; then
    echo -e "${RED}Tests failed!${NC}"
    exit 1
fi
echo -e "${GREEN}✓ Tests passed${NC}"

echo ""
echo -e "${YELLOW}Ready to create release v$VERSION${NC}"
echo ""
echo "Next steps:"
echo "  1. Review changes: git diff"
echo "  2. Commit changes: git add . && git commit -m \"Prepare release v$VERSION\""
echo "  3. Create tag: git tag -a v$VERSION -m \"Release v$VERSION\""
echo "  4. Push changes: git push && git push --tags"
echo ""
echo "Or run: ./scripts/create-release.sh $VERSION"
echo ""
