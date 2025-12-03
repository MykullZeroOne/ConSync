# Installation

This guide walks you through installing ConSync on Windows, macOS, or Linux.

## Prerequisites

### Java 21 or Higher

ConSync requires Java 21 or higher to run.

**Check your Java version:**

```bash
java -version
```

You should see version 21 or higher:

```
openjdk version "21.0.1" 2023-10-17
```

**Install Java if needed:**

- **macOS**: `brew install openjdk@21`
- **Linux (Ubuntu/Debian)**: `sudo apt install openjdk-21-jdk`
- **Windows**: Download from [Adoptium](https://adoptium.net/) or [Oracle](https://www.oracle.com/java/technologies/downloads/)

## Installation Methods

Choose the installation method that works best for you:

### Method 1: Automated Installer (Recommended)

The automated installer downloads the latest release and sets up ConSync with a convenient wrapper script.

#### macOS / Linux

1. **Download the latest release:**

```bash
curl -LO https://github.com/MykullZeroOne/ConSync/releases/latest/download/consync-0.1.2-unix.tar.gz
```

2. **Extract the archive:**

```bash
tar -xzf consync-0.1.2-unix.tar.gz
cd consync-0.1.2-unix
```

3. **Run the installer:**

```bash
./install.sh
```

The installer will:
- Install ConSync to `~/.consync-app/`
- Create a wrapper script in `/usr/local/bin/consync`
- Prompt for Confluence credentials (optional)
- Verify the installation

4. **Verify installation:**

```bash
consync --version
```

You should see: `consync version 0.1.2`

#### Windows

1. **Download the latest release:**

Download `consync-0.1.2-windows.zip` from [GitHub Releases](https://github.com/MykullZeroOne/ConSync/releases/latest)

2. **Extract the ZIP file** to a directory (e.g., `C:\consync`)

3. **Run PowerShell as Administrator:**

Right-click PowerShell and select "Run as Administrator"

4. **Navigate to the extracted directory:**

```powershell
cd C:\consync\consync-0.1.2-windows
```

5. **Run the installer:**

```powershell
.\install.ps1
```

The installer will:
- Install ConSync to `$env:USERPROFILE\.consync-app\`
- Add ConSync to your PATH
- Prompt for Confluence credentials (optional)
- Verify the installation

6. **Restart PowerShell** to load the new PATH

7. **Verify installation:**

```powershell
consync --version
```

### Method 2: Update Existing Installation

If you already have ConSync installed, use the update script:

#### macOS / Linux

```bash
# Navigate to your ConSync installation directory
cd /path/to/ConSync

# Run the update script
./update.sh
```

The update script will:
- Check your current version
- Download the latest release from GitHub
- Verify checksums
- Install the update automatically

#### Windows

```powershell
# Navigate to your ConSync installation directory
cd C:\path\to\ConSync

# Run the update script
.\update.ps1
```

### Method 3: Direct JAR Download

For advanced users or CI/CD environments, you can download the JAR directly:

```bash
# Download the JAR
curl -LO https://github.com/MykullZeroOne/ConSync/releases/latest/download/consync-0.1.2.jar

# Run directly with Java
java -jar consync-0.1.2.jar --version
```

**Create an alias for convenience:**

**macOS/Linux** (add to `.bashrc` or `.zshrc`):
```bash
alias consync='java -jar /path/to/consync-0.1.2.jar'
```

**Windows** (PowerShell profile):
```powershell
function consync { java -jar C:\path\to\consync-0.1.2.jar @args }
```

## Obtaining Confluence Credentials

ConSync needs credentials to access your Confluence instance.

### For Confluence Cloud (API Token)

Confluence Cloud uses username + API token authentication.

1. **Log in to Atlassian:**

Visit [https://id.atlassian.com/manage-profile/security/api-tokens](https://id.atlassian.com/manage-profile/security/api-tokens)

2. **Create API Token:**

- Click "Create API token"
- Give it a label (e.g., "ConSync")
- Copy the token immediately (you won't see it again!)

3. **Set environment variable:**

**macOS/Linux:**
```bash
export CONFLUENCE_API_TOKEN="your-token-here"
```

Add to your `.bashrc` or `.zshrc` to make it permanent:
```bash
echo 'export CONFLUENCE_API_TOKEN="your-token-here"' >> ~/.bashrc
source ~/.bashrc
```

**Windows (PowerShell):**
```powershell
$env:CONFLUENCE_API_TOKEN = "your-token-here"
```

To make it permanent, add to your PowerShell profile or set as system environment variable.

4. **Note your details:**

- **Base URL**: `https://your-domain.atlassian.net/wiki`
- **Username**: Your Atlassian email address
- **API Token**: The token you just created

### For Data Center/Server (Personal Access Token)

Confluence Data Center/Server uses Personal Access Token (PAT) authentication.

1. **Log in to Confluence:**

Navigate to your profile settings.

2. **Create Personal Access Token:**

- Go to Settings → Personal Access Tokens
- Click "Create token"
- Set a name (e.g., "ConSync") and expiration
- Copy the token immediately

3. **Set environment variable:**

**macOS/Linux:**
```bash
export CONFLUENCE_PAT="your-pat-here"
```

**Windows:**
```powershell
$env:CONFLUENCE_PAT = "your-pat-here"
```

4. **Note your details:**

- **Base URL**: `https://confluence.yourcompany.com`
- **PAT**: The token you just created

## Configuration Setup

After installation, create a `.env` file in your project directory to store credentials:

```bash
# .env file (add to .gitignore!)
CONFLUENCE_API_TOKEN=your-token-here
```

**Important**: Add `.env` to your `.gitignore` to prevent committing credentials!

## Verification

Test your installation and credentials:

```bash
# Test ConSync installation
consync --version

# Create a test directory
mkdir test-docs
cd test-docs

# Create a minimal config
cat > consync.yaml <<EOF
confluence:
  base_url: "https://your-domain.atlassian.net/wiki"
  username: "your-email@example.com"
  api_token: "\${CONFLUENCE_API_TOKEN}"

space:
  key: "TEST"
  root_page_title: "ConSync Test Documentation"

content:
  title_source: filename
  toc:
    enabled: true
    depth: 3

sync:
  delete_orphans: false
  state_file: ".consync/state.json"

files:
  include:
    - "**/*.md"
  exclude:
    - "**/node_modules/**"
    - "**/.git/**"
  index_file: "index.md"
EOF

# Create a test markdown file
echo "# Test Page

This is a test page to verify ConSync is working.

## Features

ConSync automatically:
- Creates root pages if they don't exist
- Maintains page hierarchy
- Converts markdown to Confluence storage format
- Tracks sync state to detect changes

" > test.md

# Dry run to test connectivity (doesn't modify Confluence)
consync sync --dry-run .
```

If the dry run succeeds, ConSync is properly installed and configured!

## New in v0.1.2

Recent improvements include:

- **Automatic Root Page Creation**: When using `root_page_title`, ConSync now automatically creates the root page if it doesn't exist
- **Fixed Duplicate Pages**: Directories with `index.md` files no longer create duplicate pages
- **macOS Compatibility**: Update script now works correctly on macOS
- **Cleaner Logging**: Removed confusing warnings about unused authentication methods
- **Improved Hierarchy**: More robust directory-to-page hierarchy conversion

## Troubleshooting

### Java Version Issues

**Error**: `UnsupportedClassVersionError`

**Solution**: Update to Java 21 or higher. Check with `java -version`.

### Connection Errors

**Error**: `Connection refused` or `Unknown host`

**Solution**:
- Check your `base_url` in `consync.yaml`
- Ensure you can access Confluence in a browser
- For Cloud: URL should be `https://your-domain.atlassian.net/wiki`
- For Data Center/Server: URL should be `https://confluence.yourcompany.com`

### Authentication Errors

**Error**: `401 Unauthorized`

**Solution**:
- Verify your API token/PAT is correct
- Ensure environment variable is set: `echo $CONFLUENCE_API_TOKEN` (macOS/Linux) or `$env:CONFLUENCE_API_TOKEN` (Windows)
- Check username matches your Confluence account (for Cloud)
- Verify the token hasn't expired (for Data Center/Server PATs)

### Space Not Found

**Error**: `Space 'XXX' not found`

**Solution**:
- Verify the space key exists in Confluence
- Ensure you have permissions to access the space
- Space keys are case-sensitive
- Try accessing the space in your browser first

### Permission Errors

**Error**: `403 Forbidden` or `You do not have permission to create pages`

**Solution**:
- Verify you have "Create Pages" permission in the space
- Contact your Confluence administrator if needed
- Ensure you're in the correct space

### Root Page Creation Issues

**Error**: Root page not being created automatically

**Solution**:
- Ensure you're using `root_page_title` (not `root_page_id`) in your config
- Verify you have permission to create pages at the space root level
- Check the logs for detailed error messages

### Installer Script Issues

**macOS/Linux**: If you get "Permission denied":
```bash
chmod +x install.sh
./install.sh
```

**Windows**: If you get execution policy errors:
```powershell
Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser
```

## Next Steps

Now that ConSync is installed, proceed to the [Quickstart](quickstart.md) guide to sync your first documentation!

## Additional Resources

- [GitHub Repository](https://github.com/MykullZeroOne/ConSync)
- [Latest Releases](https://github.com/MykullZeroOne/ConSync/releases)
- [Configuration Guide](../guides/configuration.md)
- [Usage Guide](../guides/usage.md)
