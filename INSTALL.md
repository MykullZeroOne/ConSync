# ConSync Installation Guide

Complete guide for installing ConSync on macOS, Linux, and Windows.

## Quick Start

```bash
git clone https://github.com/MykullZeroOne/consync.git
cd consync
./install.sh
```

That's it! The installer handles everything.

## Installation Options

### Option 1: Automated Installer (Recommended)

**Best for:** First-time users, production use

The automated installer provides a complete installation experience:

```bash
./install.sh
```

**What it does:**
1. Checks Java version (requires Java 17+)
2. Builds ConSync from source
3. Installs to `~/.consync-app/`
4. Creates `consync` command in `/usr/local/bin`
5. Prompts for and securely stores credentials
6. Adds credentials to your shell profile

**After installation:**

```bash
# Reload your shell
source ~/.zshrc  # or ~/.bashrc

# Test it works
consync --version

# Use it
consync sync --dry-run /path/to/docs
```

**Uninstall:**

```bash
./uninstall.sh
```

### Option 2: Quick Setup (Alias)

**Best for:** Development, testing, minimal installation

Creates an alias without copying files:

```bash
./scripts/quick-setup.sh
```

**What it does:**
1. Builds ConSync
2. Adds alias to shell profile: `alias consync='java -jar /path/to/consync.jar'`
3. Optionally adds credentials to shell profile

**Benefits:**
- No files copied to system directories
- Easy to update (just rebuild)
- Can run multiple versions

**Drawbacks:**
- Requires ConSync source directory to remain in place
- Less portable

### Option 3: Manual Installation

**Best for:** Custom setups, understanding the process

#### Step 1: Build ConSync

```bash
git clone https://github.com/MykullZeroOne/consync.git
cd consync
mvn clean package
```

Output: `target/consync.jar`

#### Step 2: Create Wrapper Script

Create a script (e.g., `~/bin/consync`):

```bash
#!/bin/bash
exec java -jar /path/to/consync/target/consync.jar "$@"
```

Make it executable:

```bash
chmod +x ~/bin/consync
```

Ensure `~/bin` is in your PATH:

```bash
export PATH="$HOME/bin:$PATH"
```

#### Step 3: Configure Credentials

Add to `~/.zshrc` or `~/.bashrc`:

```bash
# Confluence Cloud
export CONFLUENCE_USERNAME="your-email@example.com"
export CONFLUENCE_API_TOKEN="your-api-token"

# OR for Data Center/Server
export CONFLUENCE_PAT="your-personal-access-token"
```

Reload shell:

```bash
source ~/.zshrc
```

### Option 4: Docker (Coming Soon)

**Best for:** Containerized environments, CI/CD

```bash
docker run -v $(pwd)/docs:/docs consync/consync sync /docs
```

*Docker images coming in future release*

## Platform-Specific Instructions

### macOS

**Prerequisites:**

```bash
# Install Java
brew install openjdk@17

# Install Maven
brew install maven
```

**Recommended:** Use Option 1 (Automated Installer)

```bash
./install.sh
```

**Notes:**
- Installer uses `/usr/local/bin` (may require sudo)
- Shell profile: `~/.zshrc` (default shell is zsh)
- Credentials stored in `~/.consync-app/credentials`

### Linux (Ubuntu/Debian)

**Prerequisites:**

```bash
# Install Java
sudo apt update
sudo apt install openjdk-17-jdk

# Install Maven
sudo apt install maven
```

**Recommended:** Use Option 1 (Automated Installer)

```bash
./install.sh
```

**Notes:**
- Installer uses `/usr/local/bin` (may require sudo)
- Shell profile: `~/.bashrc`
- Credentials stored in `~/.consync-app/credentials`

### Linux (RHEL/CentOS/Fedora)

**Prerequisites:**

```bash
# Install Java
sudo dnf install java-17-openjdk-devel

# Install Maven
sudo dnf install maven
```

**Recommended:** Use Option 1 (Automated Installer)

```bash
./install.sh
```

### Windows (WSL)

ConSync works on Windows via WSL (Windows Subsystem for Linux):

1. **Install WSL:** [Microsoft WSL Guide](https://docs.microsoft.com/en-us/windows/wsl/install)
2. **Follow Linux instructions** in WSL terminal

### Windows (Native)

**Prerequisites:**
- Install Java 17+ from [Adoptium](https://adoptium.net/)
- Install Maven from [Apache Maven](https://maven.apache.org/download.cgi)

**Setup:**

```powershell
# Build
mvn clean package

# Create batch file (consync.bat)
@echo off
java -jar C:\path\to\consync\target\consync.jar %*
```

Add batch file location to PATH.

**Set credentials:**

```powershell
# PowerShell
$env:CONFLUENCE_USERNAME = "your-email@example.com"
$env:CONFLUENCE_API_TOKEN = "your-token"

# Or permanently via System Properties → Environment Variables
```

## Authentication Setup

ConSync needs credentials to access Confluence.

### Confluence Cloud

1. **Create API Token:**
   - Go to [https://id.atlassian.com/manage-profile/security/api-tokens](https://id.atlassian.com/manage-profile/security/api-tokens)
   - Click "Create API token"
   - Label it "ConSync"
   - Copy the token

2. **Set Environment Variables:**

```bash
export CONFLUENCE_USERNAME="your-email@example.com"
export CONFLUENCE_API_TOKEN="ATATTxxx..."
```

3. **Add to Shell Profile:**

```bash
echo 'export CONFLUENCE_USERNAME="your-email@example.com"' >> ~/.zshrc
echo 'export CONFLUENCE_API_TOKEN="your-token"' >> ~/.zshrc
source ~/.zshrc
```

### Confluence Data Center/Server

1. **Create Personal Access Token:**
   - Log in to Confluence
   - Go to Settings → Personal Access Tokens
   - Create new token
   - Copy the token

2. **Set Environment Variable:**

```bash
export CONFLUENCE_PAT="your-token"
```

3. **Add to Shell Profile:**

```bash
echo 'export CONFLUENCE_PAT="your-token"' >> ~/.zshrc
source ~/.zshrc
```

## Verification

Test your installation:

```bash
# Check version
consync --version

# Check help
consync --help

# Test with dry run (doesn't modify Confluence)
cd /path/to/docs
consync sync --dry-run .
```

Expected output:

```
Configuration loaded successfully
  Space: DOCS
  Base URL: https://your-domain.atlassian.net/wiki

DRY RUN MODE - No changes will be made to Confluence
...
```

## Troubleshooting

### Command Not Found

**Error:** `bash: consync: command not found`

**Solutions:**

1. **Reload shell:**
   ```bash
   source ~/.zshrc
   ```

2. **Check installation:**
   ```bash
   ls -la /usr/local/bin/consync
   ls -la ~/.consync-app/
   ```

3. **Check PATH:**
   ```bash
   echo $PATH
   ```

4. **Re-run installer:**
   ```bash
   ./install.sh
   ```

### Java Not Found

**Error:** `java: command not found`

**Solution:** Install Java 17 or higher

```bash
# macOS
brew install openjdk@17

# Linux
sudo apt install openjdk-17-jdk
```

### Wrong Java Version

**Error:** `UnsupportedClassVersionError`

**Solution:** Update to Java 17+

```bash
# Check version
java -version

# Install Java 17
brew install openjdk@17  # macOS
sudo apt install openjdk-17-jdk  # Linux

# Set JAVA_HOME if needed
export JAVA_HOME=$(/usr/libexec/java_home -v 17)  # macOS
```

### Credentials Not Working

**Error:** `401 Unauthorized`

**Solutions:**

1. **Verify credentials are set:**
   ```bash
   echo $CONFLUENCE_API_TOKEN
   echo $CONFLUENCE_USERNAME
   # or
   echo $CONFLUENCE_PAT
   ```

2. **Check credentials file:**
   ```bash
   cat ~/.consync-app/credentials
   ```

3. **Re-run setup:**
   ```bash
   ./install.sh
   # Choose authentication setup again
   ```

4. **Manually set credentials:**
   ```bash
   export CONFLUENCE_API_TOKEN="your-token"
   consync sync --dry-run .
   ```

### Permission Denied

**Error:** `Permission denied` when installing

**Solution:** Use sudo or install to user directory

```bash
# Option 1: Use sudo
sudo ./install.sh

# Option 2: Install to home directory
# Edit install.sh to use $HOME/bin instead of /usr/local/bin
```

### Build Fails

**Error:** Maven build fails

**Solutions:**

1. **Check Maven is installed:**
   ```bash
   mvn -version
   ```

2. **Clean and rebuild:**
   ```bash
   mvn clean
   mvn package
   ```

3. **Check Java version:**
   ```bash
   java -version  # Should be 17+
   ```

## Security Best Practices

1. **Never commit credentials to Git:**
   ```bash
   # Add to .gitignore
   echo ".consync-app/credentials" >> .gitignore
   echo "consync.yaml" >> .gitignore  # If it contains tokens
   ```

2. **Secure credentials file:**
   ```bash
   chmod 600 ~/.consync-app/credentials
   ```

3. **Use environment variables:**
   - Store in shell profile
   - Use secret management in CI/CD
   - Never hardcode in config files

4. **Rotate tokens regularly:**
   - Set expiration dates
   - Update credentials periodically

## Updating ConSync

To update to the latest version:

```bash
# Pull latest changes
cd /path/to/consync
git pull

# Rebuild
mvn clean package

# If using automated installer, re-run:
./install.sh  # Will rebuild and update

# If using alias, just rebuild:
mvn clean package
```

## Uninstalling

### Automated Uninstaller

```bash
./uninstall.sh
```

This removes:
- ConSync command from `/usr/local/bin`
- Installation directory `~/.consync-app`
- Credentials (optional)
- Shell profile entries

### Manual Uninstall

```bash
# Remove command
sudo rm /usr/local/bin/consync

# Remove installation directory
rm -rf ~/.consync-app

# Remove alias from shell profile
# Edit ~/.zshrc or ~/.bashrc and remove ConSync lines

# Reload shell
source ~/.zshrc
```

## Next Steps

After installation:

1. **Create your first config:** [Configuration Guide](examples/guides/configuration.md)
2. **Try the quickstart:** [Quickstart Guide](examples/getting-started/quickstart.md)
3. **Explore examples:** [Example Documentation](examples/)

## Support

Having issues? Check:

1. [Troubleshooting section above](#troubleshooting)
2. [Usage Guide](examples/guides/usage.md)
3. [GitHub Issues](https://github.com/MykullZeroOne/consync/issues)
