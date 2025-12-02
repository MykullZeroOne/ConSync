#!/bin/bash
# ConSync Installer
# This script installs ConSync and sets up authentication credentials

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}╔════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║       ConSync Installer v1.0           ║${NC}"
echo -e "${BLUE}╚════════════════════════════════════════╝${NC}"
echo ""

# Detect OS
OS="$(uname -s)"
case "${OS}" in
    Linux*)     MACHINE=Linux;;
    Darwin*)    MACHINE=Mac;;
    *)          MACHINE="UNKNOWN:${OS}"
esac

echo -e "${BLUE}Detected OS: ${MACHINE}${NC}"
echo ""

# Check Java version
echo -e "${BLUE}Checking Java installation...${NC}"
if ! command -v java &> /dev/null; then
    echo -e "${RED}Error: Java is not installed${NC}"
    echo "Please install Java 17 or higher"
    echo ""
    echo "macOS: brew install openjdk@17"
    echo "Linux: sudo apt install openjdk-17-jdk"
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 17 ]; then
    echo -e "${RED}Error: Java 17 or higher is required${NC}"
    echo "Current version: $JAVA_VERSION"
    exit 1
fi

echo -e "${GREEN}✓ Java $JAVA_VERSION detected${NC}"
echo ""

# Build ConSync if needed
echo -e "${BLUE}Building ConSync...${NC}"
if [ ! -f "target/consync.jar" ]; then
    if ! command -v mvn &> /dev/null; then
        echo -e "${RED}Error: Maven is not installed${NC}"
        echo "Please install Maven to build ConSync"
        exit 1
    fi

    mvn clean package -q
    if [ $? -ne 0 ]; then
        echo -e "${RED}Build failed${NC}"
        exit 1
    fi
fi

if [ ! -f "target/consync.jar" ]; then
    echo -e "${RED}Error: consync.jar not found after build${NC}"
    exit 1
fi

echo -e "${GREEN}✓ ConSync built successfully${NC}"
echo ""

# Determine installation directory
if [ "$MACHINE" = "Mac" ]; then
    INSTALL_DIR="/usr/local/bin"
    SHELL_PROFILE="$HOME/.zshrc"
elif [ "$MACHINE" = "Linux" ]; then
    INSTALL_DIR="/usr/local/bin"
    SHELL_PROFILE="$HOME/.bashrc"
else
    echo -e "${YELLOW}Unknown OS, using default installation directory${NC}"
    INSTALL_DIR="/usr/local/bin"
    SHELL_PROFILE="$HOME/.bashrc"
fi

# Check if we can write to install dir
if [ ! -w "$INSTALL_DIR" ]; then
    echo -e "${YELLOW}Need sudo permissions to install to $INSTALL_DIR${NC}"
    USE_SUDO="sudo"
else
    USE_SUDO=""
fi

# Create installation directory for ConSync
CONSYNC_HOME="$HOME/.consync-app"
mkdir -p "$CONSYNC_HOME"

# Copy JAR to installation directory
echo -e "${BLUE}Installing ConSync to $CONSYNC_HOME...${NC}"
cp target/consync.jar "$CONSYNC_HOME/consync.jar"
echo -e "${GREEN}✓ ConSync JAR installed${NC}"
echo ""

# Create wrapper script
WRAPPER_SCRIPT="$CONSYNC_HOME/consync"
cat > "$WRAPPER_SCRIPT" << 'EOF'
#!/bin/bash
# ConSync wrapper script
# This script wraps the ConSync JAR for easy command-line usage

# Path to ConSync JAR
CONSYNC_JAR="$HOME/.consync-app/consync.jar"

# Check if JAR exists
if [ ! -f "$CONSYNC_JAR" ]; then
    echo "Error: ConSync JAR not found at $CONSYNC_JAR"
    echo "Please run the installer again."
    exit 1
fi

# Load credentials from config file if it exists
CONSYNC_CONFIG="$HOME/.consync-app/credentials"
if [ -f "$CONSYNC_CONFIG" ]; then
    source "$CONSYNC_CONFIG"
fi

# Run ConSync with all arguments
exec java -jar "$CONSYNC_JAR" "$@"
EOF

chmod +x "$WRAPPER_SCRIPT"
echo -e "${GREEN}✓ Wrapper script created${NC}"
echo ""

# Create symlink to wrapper script
echo -e "${BLUE}Creating command-line shortcut...${NC}"
$USE_SUDO ln -sf "$WRAPPER_SCRIPT" "$INSTALL_DIR/consync"
echo -e "${GREEN}✓ 'consync' command installed to $INSTALL_DIR${NC}"
echo ""

# Configure authentication
echo -e "${BLUE}╔════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║     Authentication Setup               ║${NC}"
echo -e "${BLUE}╚════════════════════════════════════════╝${NC}"
echo ""
echo "ConSync needs Confluence credentials to sync documentation."
echo ""
echo "Choose authentication method:"
echo "  1) Confluence Cloud (API Token)"
echo "  2) Confluence Data Center/Server (Personal Access Token)"
echo "  3) Skip (configure manually later)"
echo ""
read -p "Enter choice [1-3]: " auth_choice

CREDS_FILE="$CONSYNC_HOME/credentials"

case $auth_choice in
    1)
        echo ""
        echo -e "${BLUE}Confluence Cloud Setup${NC}"
        echo ""
        echo "You'll need:"
        echo "  - Your Atlassian email address"
        echo "  - An API token (create at: https://id.atlassian.com/manage-profile/security/api-tokens)"
        echo ""

        read -p "Atlassian email: " confluence_email
        read -sp "API Token: " api_token
        echo ""

        cat > "$CREDS_FILE" << EOF
# ConSync Credentials - Confluence Cloud
export CONFLUENCE_USERNAME="$confluence_email"
export CONFLUENCE_API_TOKEN="$api_token"
EOF

        echo -e "${GREEN}✓ API token saved${NC}"

        # Also add to shell profile
        if ! grep -q "source $CREDS_FILE" "$SHELL_PROFILE" 2>/dev/null; then
            echo "" >> "$SHELL_PROFILE"
            echo "# ConSync credentials" >> "$SHELL_PROFILE"
            echo "source $CREDS_FILE" >> "$SHELL_PROFILE"
            echo -e "${GREEN}✓ Added to $SHELL_PROFILE${NC}"
        fi
        ;;

    2)
        echo ""
        echo -e "${BLUE}Confluence Data Center/Server Setup${NC}"
        echo ""
        echo "You'll need a Personal Access Token"
        echo "Create in: Confluence Settings → Personal Access Tokens"
        echo ""

        read -sp "Personal Access Token: " pat_token
        echo ""

        cat > "$CREDS_FILE" << EOF
# ConSync Credentials - Confluence Data Center/Server
export CONFLUENCE_PAT="$pat_token"
EOF

        echo -e "${GREEN}✓ Personal Access Token saved${NC}"

        # Also add to shell profile
        if ! grep -q "source $CREDS_FILE" "$SHELL_PROFILE" 2>/dev/null; then
            echo "" >> "$SHELL_PROFILE"
            echo "# ConSync credentials" >> "$SHELL_PROFILE"
            echo "source $CREDS_FILE" >> "$SHELL_PROFILE"
            echo -e "${GREEN}✓ Added to $SHELL_PROFILE${NC}"
        fi
        ;;

    3)
        echo ""
        echo -e "${YELLOW}Skipping authentication setup${NC}"
        echo ""
        echo "You'll need to set environment variables manually:"
        echo ""
        echo "For Confluence Cloud:"
        echo "  export CONFLUENCE_USERNAME=\"your-email@example.com\""
        echo "  export CONFLUENCE_API_TOKEN=\"your-token\""
        echo ""
        echo "For Confluence Data Center/Server:"
        echo "  export CONFLUENCE_PAT=\"your-personal-access-token\""
        ;;

    *)
        echo -e "${RED}Invalid choice${NC}"
        ;;
esac

# Secure the credentials file
if [ -f "$CREDS_FILE" ]; then
    chmod 600 "$CREDS_FILE"
    echo -e "${GREEN}✓ Credentials file secured (600 permissions)${NC}"
fi

echo ""
echo -e "${BLUE}╔════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║     Installation Complete!             ║${NC}"
echo -e "${BLUE}╚════════════════════════════════════════╝${NC}"
echo ""
echo -e "${GREEN}ConSync has been installed successfully!${NC}"
echo ""
echo "Installation details:"
echo "  ConSync JAR: $CONSYNC_HOME/consync.jar"
echo "  Command: $INSTALL_DIR/consync"
echo "  Credentials: $CREDS_FILE"
echo ""
echo "To use ConSync, run:"
echo -e "  ${YELLOW}consync sync --dry-run /path/to/docs${NC}"
echo ""
echo "For the changes to take effect in your current shell:"
echo -e "  ${YELLOW}source $SHELL_PROFILE${NC}"
echo ""
echo "Or open a new terminal window."
echo ""
echo "Quick test:"
echo -e "  ${YELLOW}consync --version${NC}"
echo ""
echo "For help:"
echo -e "  ${YELLOW}consync --help${NC}"
echo ""
echo -e "${BLUE}Happy documenting! 📚${NC}"
