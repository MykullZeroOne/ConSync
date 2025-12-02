#!/bin/bash
# ConSync Uninstaller

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}╔════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║       ConSync Uninstaller              ║${NC}"
echo -e "${BLUE}╚════════════════════════════════════════╝${NC}"
echo ""

CONSYNC_HOME="$HOME/.consync-app"
INSTALL_DIR="/usr/local/bin"
SHELL_PROFILE="$HOME/.zshrc"
if [ "$(uname -s)" = "Linux" ]; then
    SHELL_PROFILE="$HOME/.bashrc"
fi

echo -e "${YELLOW}This will remove ConSync from your system.${NC}"
echo ""
read -p "Continue? [y/N]: " confirm

if [[ ! "$confirm" =~ ^[Yy]$ ]]; then
    echo "Uninstall cancelled."
    exit 0
fi

echo ""

# Remove command
if [ -f "$INSTALL_DIR/consync" ]; then
    echo -e "${BLUE}Removing consync command...${NC}"
    if [ -w "$INSTALL_DIR" ]; then
        rm -f "$INSTALL_DIR/consync"
    else
        sudo rm -f "$INSTALL_DIR/consync"
    fi
    echo -e "${GREEN}✓ Command removed${NC}"
fi

# Remove installation directory
if [ -d "$CONSYNC_HOME" ]; then
    echo -e "${BLUE}Removing ConSync files...${NC}"

    # Ask about credentials
    if [ -f "$CONSYNC_HOME/credentials" ]; then
        echo ""
        read -p "Remove stored credentials? [y/N]: " remove_creds
        if [[ "$remove_creds" =~ ^[Yy]$ ]]; then
            rm -f "$CONSYNC_HOME/credentials"
            echo -e "${GREEN}✓ Credentials removed${NC}"
        else
            echo -e "${YELLOW}Credentials kept at $CONSYNC_HOME/credentials${NC}"
        fi
    fi

    rm -rf "$CONSYNC_HOME"
    echo -e "${GREEN}✓ ConSync directory removed${NC}"
fi

# Remove from shell profile
if [ -f "$SHELL_PROFILE" ]; then
    echo -e "${BLUE}Cleaning shell profile...${NC}"
    sed -i.bak '/# ConSync credentials/d' "$SHELL_PROFILE" 2>/dev/null || true
    sed -i.bak "\|source $CONSYNC_HOME/credentials|d" "$SHELL_PROFILE" 2>/dev/null || true
    rm -f "${SHELL_PROFILE}.bak"
    echo -e "${GREEN}✓ Shell profile cleaned${NC}"
fi

echo ""
echo -e "${GREEN}ConSync has been uninstalled.${NC}"
echo ""
echo "Note: Reload your shell or run:"
echo -e "  ${YELLOW}source $SHELL_PROFILE${NC}"
echo ""
