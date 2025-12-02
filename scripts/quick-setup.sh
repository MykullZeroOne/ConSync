#!/bin/bash
# Quick Setup - Just adds an alias to your shell profile
# For users who want minimal installation

set -e

BLUE='\033[0;34m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${BLUE}ConSync Quick Setup${NC}"
echo ""

# Detect shell profile
if [ -f "$HOME/.zshrc" ]; then
    SHELL_PROFILE="$HOME/.zshrc"
    SHELL_NAME="zsh"
elif [ -f "$HOME/.bashrc" ]; then
    SHELL_PROFILE="$HOME/.bashrc"
    SHELL_NAME="bash"
else
    echo -e "${YELLOW}Could not detect shell profile${NC}"
    read -p "Enter path to shell profile: " SHELL_PROFILE
    SHELL_NAME="shell"
fi

echo "Using: $SHELL_PROFILE"
echo ""

# Get ConSync directory
CONSYNC_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )/.." && pwd )"
CONSYNC_JAR="$CONSYNC_DIR/target/consync.jar"

if [ ! -f "$CONSYNC_JAR" ]; then
    echo -e "${YELLOW}ConSync JAR not found. Building...${NC}"
    cd "$CONSYNC_DIR"
    mvn clean package -q
fi

# Add alias
echo ""
echo "Adding alias to $SHELL_PROFILE..."

cat >> "$SHELL_PROFILE" << EOF

# ConSync alias
alias consync='java -jar $CONSYNC_JAR'
EOF

echo -e "${GREEN}✓ Alias added${NC}"
echo ""
echo "Now set up your credentials:"
echo ""
echo "For Confluence Cloud:"
echo "  export CONFLUENCE_USERNAME=\"your-email@example.com\""
echo "  export CONFLUENCE_API_TOKEN=\"your-api-token\""
echo ""
echo "For Confluence Data Center/Server:"
echo "  export CONFLUENCE_PAT=\"your-personal-access-token\""
echo ""
read -p "Would you like to add credentials now? [y/N]: " add_creds

if [[ "$add_creds" =~ ^[Yy]$ ]]; then
    echo ""
    echo "Choose type:"
    echo "  1) Confluence Cloud (API Token)"
    echo "  2) Confluence Data Center/Server (PAT)"
    read -p "Enter choice [1-2]: " cred_type

    case $cred_type in
        1)
            read -p "Atlassian email: " email
            read -sp "API Token: " token
            echo ""

            cat >> "$SHELL_PROFILE" << EOF

# Confluence credentials (Cloud)
export CONFLUENCE_USERNAME="$email"
export CONFLUENCE_API_TOKEN="$token"
EOF
            ;;
        2)
            read -sp "Personal Access Token: " token
            echo ""

            cat >> "$SHELL_PROFILE" << EOF

# Confluence credentials (Data Center/Server)
export CONFLUENCE_PAT="$token"
EOF
            ;;
    esac

    echo -e "${GREEN}✓ Credentials added${NC}"
fi

echo ""
echo -e "${GREEN}Setup complete!${NC}"
echo ""
echo "To use immediately, run:"
echo -e "  ${YELLOW}source $SHELL_PROFILE${NC}"
echo ""
echo "Or open a new terminal."
echo ""
echo "Try it:"
echo -e "  ${YELLOW}consync --version${NC}"
echo ""
