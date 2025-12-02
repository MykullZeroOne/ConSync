# Installing ConSync

Quick installation guide for users.

## Choose Your Platform

- [macOS](#macos)
- [Linux](#linux)
- [Windows](#windows)
- [Docker](#docker-coming-soon)

---

## macOS

### Option 1: Homebrew (Recommended)

```bash
# Add ConSync tap
brew tap yourusername/consync

# Install
brew install consync

# Verify
consync --version
```

### Option 2: Direct Download

```bash
# Download latest release
curl -LO https://github.com/yourusername/consync/releases/latest/download/consync-unix.tar.gz

# Extract
tar -xzf consync-unix.tar.gz
cd consync-*-unix

# Install
./install.sh
```

---

## Linux

### Option 1: Homebrew

```bash
# Install Homebrew (if not already installed)
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"

# Add ConSync tap
brew tap yourusername/consync

# Install
brew install consync

# Verify
consync --version
```

### Option 2: Direct Download

```bash
# Download latest release
wget https://github.com/yourusername/consync/releases/latest/download/consync-unix.tar.gz

# Extract
tar -xzf consync-unix.tar.gz
cd consync-*-unix

# Install
./install.sh
```

### Option 3: Manual Installation

```bash
# Download JAR
wget https://github.com/yourusername/consync/releases/latest/download/consync.jar

# Create wrapper script
cat > consync << 'EOF'
#!/bin/bash
exec java -jar /path/to/consync.jar "$@"
EOF

chmod +x consync

# Move to PATH
sudo mv consync /usr/local/bin/
```

---

## Windows

### Option 1: Scoop (Recommended)

```powershell
# Install Scoop (if not already installed)
iwr -useb get.scoop.sh | iex

# Add ConSync bucket
scoop bucket add consync https://github.com/yourusername/scoop-consync

# Install
scoop install consync

# Verify
consync --version
```

### Option 2: Chocolatey

```powershell
# Run PowerShell as Administrator

# Install Chocolatey (if not already installed)
Set-ExecutionPolicy Bypass -Scope Process -Force
iwr https://community.chocolatey.org/install.ps1 -UseBasicParsing | iex

# Install ConSync
choco install consync

# Verify
consync --version
```

### Option 3: Direct Download

```powershell
# Download latest release
# Go to: https://github.com/yourusername/consync/releases/latest
# Download: consync-X.X.X-windows.zip

# Extract the ZIP file

# Run installer (PowerShell as Administrator)
cd consync-X.X.X-windows
.\install.ps1
```

### Option 4: Manual Installation

```powershell
# Download JAR
# Go to: https://github.com/yourusername/consync/releases/latest
# Download: consync.jar

# Create batch file (consync.bat)
@echo off
java -jar C:\path\to\consync.jar %*

# Add batch file location to PATH
# System Properties → Environment Variables → Path → Edit → New
```

---

## Docker (Coming Soon)

```bash
docker pull consync/consync:latest

docker run -v $(pwd)/docs:/docs consync/consync sync /docs
```

---

## Post-Installation

### Configure Authentication

After installation, configure Confluence credentials:

#### Confluence Cloud

```bash
# macOS/Linux
export CONFLUENCE_USERNAME="your-email@example.com"
export CONFLUENCE_API_TOKEN="your-api-token"

# Add to ~/.zshrc or ~/.bashrc for persistence
echo 'export CONFLUENCE_USERNAME="your-email@example.com"' >> ~/.zshrc
echo 'export CONFLUENCE_API_TOKEN="your-token"' >> ~/.zshrc
```

```powershell
# Windows (PowerShell)
$env:CONFLUENCE_USERNAME = "your-email@example.com"
$env:CONFLUENCE_API_TOKEN = "your-api-token"

# Add to PowerShell profile for persistence
Add-Content $PROFILE @"
`$env:CONFLUENCE_USERNAME = 'your-email@example.com'
`$env:CONFLUENCE_API_TOKEN = 'your-token'
"@
```

#### Confluence Data Center/Server

```bash
# macOS/Linux
export CONFLUENCE_PAT="your-personal-access-token"
echo 'export CONFLUENCE_PAT="your-token"' >> ~/.zshrc
```

```powershell
# Windows
$env:CONFLUENCE_PAT = "your-personal-access-token"
Add-Content $PROFILE "`$env:CONFLUENCE_PAT = 'your-token'"
```

### Get API Token

**Confluence Cloud:**
1. Go to https://id.atlassian.com/manage-profile/security/api-tokens
2. Click "Create API token"
3. Copy the token

**Confluence Data Center/Server:**
1. Log in to Confluence
2. Go to Settings → Personal Access Tokens
3. Create new token
4. Copy the token

### Verify Installation

```bash
# Check version
consync --version

# Show help
consync --help

# Test with dry run (doesn't modify anything)
cd /path/to/your/docs
consync sync --dry-run .
```

---

## Requirements

- **Java**: Version 17 or higher

### Install Java

**macOS:**
```bash
brew install openjdk@17
```

**Linux (Ubuntu/Debian):**
```bash
sudo apt update
sudo apt install openjdk-17-jdk
```

**Windows:**
- Download from https://adoptium.net/
- Or with Scoop: `scoop install openjdk17`
- Or with Chocolatey: `choco install openjdk17`

---

## Troubleshooting

### Command not found

**Solution:** Restart your terminal or reload shell profile

```bash
# macOS/Linux
source ~/.zshrc  # or ~/.bashrc

# Windows: Restart PowerShell
```

### Java not found

**Solution:** Install Java 17 or higher

```bash
java -version  # Should show 17+
```

### Permission denied (macOS/Linux)

**Solution:** Run installer with sudo

```bash
sudo ./install.sh
```

### Credentials not working

**Solution:** Verify environment variables are set

```bash
# macOS/Linux
echo $CONFLUENCE_API_TOKEN

# Windows
echo $env:CONFLUENCE_API_TOKEN
```

---

## Getting Started

1. **Create a config file** in your docs directory:

```yaml
# consync.yaml
confluence:
  base_url: "https://your-domain.atlassian.net/wiki"
  username: "your-email@example.com"
  api_token: "${CONFLUENCE_API_TOKEN}"

space:
  key: "DOCS"

content:
  root_page: "Documentation"
```

2. **Add some markdown files**:

```bash
echo "# Welcome" > index.md
echo "# Getting Started" > getting-started.md
```

3. **Sync to Confluence**:

```bash
consync sync --dry-run .   # Preview changes
consync sync .             # Actually sync
```

---

## Next Steps

- **Read the docs**: https://github.com/yourusername/consync
- **See examples**: https://github.com/yourusername/consync/tree/main/examples
- **Report issues**: https://github.com/yourusername/consync/issues

---

## Update ConSync

### Homebrew

```bash
brew update
brew upgrade consync
```

### Scoop

```powershell
scoop update
scoop update consync
```

### Chocolatey

```powershell
choco upgrade consync
```

### Manual

Download latest release and reinstall:
https://github.com/yourusername/consync/releases/latest

---

## Uninstall

### Homebrew

```bash
brew uninstall consync
brew untap yourusername/consync
```

### Scoop

```powershell
scoop uninstall consync
```

### Chocolatey

```powershell
choco uninstall consync
```

### Manual

```bash
# macOS/Linux
sudo rm /usr/local/bin/consync
rm -rf ~/.consync-app

# Windows
# Remove from PATH in Environment Variables
# Delete installation directory
```
