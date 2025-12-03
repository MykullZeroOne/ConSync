#!/bin/bash
# ConSync Update Script
set -e

BLUE='\033[0;34m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${BLUE}ConSync Updater${NC}"
echo ""

# Check if ConSync is installed
if ! command -v consync &> /dev/null; then
    echo -e "${RED}Error: ConSync is not installed${NC}"
    echo "Install it first using: ./install.sh"
    exit 1
fi

# Get current version
CURRENT_VERSION=$(consync --version 2>&1 | grep -oE '[0-9]+\.[0-9]+\.[0-9]+' || echo "unknown")
echo -e "Current version: ${YELLOW}${CURRENT_VERSION}${NC}"

# Get latest release version from GitHub
echo "Checking for updates..."
LATEST_VERSION=$(curl -s https://api.github.com/repos/MykullZeroOne/ConSync/releases/latest | grep -oP '"tag_name": "\K(.*)(?=")')
if [ -z "$LATEST_VERSION" ]; then
    echo -e "${RED}Error: Could not fetch latest version${NC}"
    exit 1
fi

# Remove 'v' prefix if present
LATEST_VERSION=${LATEST_VERSION#v}
echo -e "Latest version: ${GREEN}${LATEST_VERSION}${NC}"
echo ""

# Compare versions
if [ "$CURRENT_VERSION" == "$LATEST_VERSION" ]; then
    echo -e "${GREEN}✓ You're already running the latest version!${NC}"
    exit 0
fi

# Download and install update
echo -e "${YELLOW}Updating ConSync...${NC}"
echo ""

# Create temp directory
TEMP_DIR=$(mktemp -d)
cd "$TEMP_DIR"

# Download latest release
echo "Downloading ConSync v${LATEST_VERSION}..."
curl -LO "https://github.com/MykullZeroOne/ConSync/releases/download/v${LATEST_VERSION}/consync-${LATEST_VERSION}-unix.tar.gz"
curl -LO "https://github.com/MykullZeroOne/ConSync/releases/download/v${LATEST_VERSION}/consync-${LATEST_VERSION}-unix.tar.gz.sha256"

# Verify checksum
echo "Verifying checksum..."
if sha256sum -c "consync-${LATEST_VERSION}-unix.tar.gz.sha256" > /dev/null 2>&1; then
    echo -e "${GREEN}✓ Checksum verified${NC}"
else
    echo -e "${RED}✗ Checksum verification failed${NC}"
    exit 1
fi

# Extract
echo "Extracting..."
tar -xzf "consync-${LATEST_VERSION}-unix.tar.gz"
cd "consync-${LATEST_VERSION}-unix"

# Run installer (will replace existing installation)
echo "Installing..."
./install.sh --skip-credentials

# Cleanup
cd /
rm -rf "$TEMP_DIR"

echo ""
echo -e "${GREEN}✓ ConSync updated successfully to v${LATEST_VERSION}!${NC}"
echo ""
echo "Verify: consync --version"
