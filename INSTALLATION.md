# ConSync Installation Guide

ConSync can be installed using either Windows Scoop package manager or direct download from GitHub Releases.

## Table of Contents

- [Prerequisites](#prerequisites)
- [Windows Installation (Scoop)](#windows-installation-scoop)
- [Direct Download (All Platforms)](#direct-download-all-platforms)
  - [Windows](#windows)
  - [macOS/Linux](#macoslinux)
- [Authentication Setup](#authentication-setup)
- [Verification](#verification)
- [Troubleshooting](#troubleshooting)

---

## Prerequisites

**Java 21 or higher is required** to run ConSync.

### Check if Java is installed:

```bash
java -version
```

You should see output indicating Java 21 or higher.

### Install Java if needed:

**Windows (Scoop):**
```powershell
scoop install openjdk
```

**Windows (Direct):**
Download from [Adoptium](https://adoptium.net/)

**macOS:**
```bash
brew install openjdk@21
```

**Linux (Ubuntu/Debian):**
```bash
sudo apt update
sudo apt install openjdk-21-jdk
```

---

## Windows Installation (Scoop)

[Scoop](https://scoop.sh/) is a command-line installer for Windows. It provides the easiest installation experience.

### 1. Install Scoop (if not already installed)

```powershell
# In PowerShell
Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser
Invoke-RestMethod -Uri https://get.scoop.sh | Invoke-Expression
```

### 2. Add the ConSync bucket

```powershell
scoop bucket add consync https://github.com/MykullZeroOne/scoop-consync
```

### 3. Install ConSync

```powershell
scoop install consync
```

Scoop will automatically:
- Download the latest ConSync release
- Verify the download using SHA256 checksums
- Add ConSync to your PATH
- Suggest Java installation if not present

### 4. Verify installation

```powershell
consync --version
```

### Updating ConSync (Scoop)

```powershell
scoop update consync
```

### Uninstalling ConSync (Scoop)

```powershell
scoop uninstall consync
```

---

## Direct Download (All Platforms)

Download ConSync directly from [GitHub Releases](https://github.com/MykullZeroOne/ConSync/releases).

### Windows

#### Option 1: Windows Package with Installer

1. **Download** `consync-0.1.0-windows.zip` from the [latest release](https://github.com/MykullZeroOne/ConSync/releases/latest)

2. **Verify the download** (optional but recommended):
   ```powershell
   # Download the checksum file too
   # Then verify:
   certutil -hashfile consync-0.1.0-windows.zip SHA256
   # Compare with the contents of consync-0.1.0-windows.zip.sha256
   ```

3. **Extract** the ZIP file to a directory (e.g., `C:\ConSync`)

4. **Run the installer** as Administrator:
   ```powershell
   .\install.ps1
   ```

   The installer will:
   - Check for Java 21+
   - Copy files to `%LOCALAPPDATA%\ConSync`
   - Add ConSync to your PATH
   - Display authentication setup instructions

5. **Restart your terminal** for PATH changes to take effect

6. **Verify**:
   ```powershell
   consync --version
   ```

#### Option 2: Direct JAR Usage

1. **Download** `consync-0.1.0.jar` from the [latest release](https://github.com/MykullZeroOne/ConSync/releases/latest)

2. **Verify the download** (optional):
   ```powershell
   certutil -hashfile consync-0.1.0.jar SHA256
   ```

3. **Run directly**:
   ```powershell
   java -jar consync-0.1.0.jar --help
   ```

4. **Create a batch file** for convenience (optional):
   ```batch
   @echo off
   java -jar C:\path\to\consync-0.1.0.jar %*
   ```
   Save as `consync.bat` in a directory in your PATH.

### macOS/Linux

#### Option 1: Unix Package with Installer

1. **Download** `consync-0.1.0-unix.tar.gz` from the [latest release](https://github.com/MykullZeroOne/ConSync/releases/latest)

2. **Verify the download** (optional but recommended):
   ```bash
   sha256sum consync-0.1.0-unix.tar.gz
   # Compare with the contents of consync-0.1.0-unix.tar.gz.sha256
   # Or:
   sha256sum -c consync-0.1.0-unix.tar.gz.sha256
   ```

3. **Extract**:
   ```bash
   tar -xzf consync-0.1.0-unix.tar.gz
   cd consync-0.1.0-unix
   ```

4. **Run the installer**:
   ```bash
   ./install.sh
   ```

   The installer will:
   - Check for Java 21+
   - Install ConSync to `~/.consync-app`
   - Create a wrapper script
   - Link to `/usr/local/bin` (may ask for sudo password)
   - Prompt for authentication credentials (optional)

5. **Verify**:
   ```bash
   consync --version
   ```

#### Option 2: Direct JAR Usage

1. **Download** `consync-0.1.0.jar` from the [latest release](https://github.com/MykullZeroOne/ConSync/releases/latest)

2. **Verify the download** (optional):
   ```bash
   sha256sum consync-0.1.0.jar
   ```

3. **Run directly**:
   ```bash
   java -jar consync-0.1.0.jar --help
   ```

4. **Create an alias** for convenience (optional):
   ```bash
   # Add to ~/.bashrc or ~/.zshrc
   alias consync='java -jar /path/to/consync-0.1.0.jar'
   ```

---

## Authentication Setup

ConSync requires authentication credentials to access your Confluence instance.

### Confluence Cloud

Set environment variables:

**Windows (PowerShell):**
```powershell
$env:CONFLUENCE_USERNAME = "your-email@example.com"
$env:CONFLUENCE_API_TOKEN = "your-api-token"
```

To make permanent, add to your PowerShell profile or use System Environment Variables.

**macOS/Linux:**
```bash
export CONFLUENCE_USERNAME="your-email@example.com"
export CONFLUENCE_API_TOKEN="your-api-token"
```

Add to `~/.bashrc`, `~/.zshrc`, or `~/.profile` to make permanent.

**Generate API Token:**
1. Go to https://id.atlassian.com/manage-profile/security/api-tokens
2. Click "Create API token"
3. Copy the token (you won't be able to see it again!)

### Confluence Data Center/Server

Set environment variable:

**Windows (PowerShell):**
```powershell
$env:CONFLUENCE_PAT = "your-personal-access-token"
```

**macOS/Linux:**
```bash
export CONFLUENCE_PAT="your-personal-access-token"
```

**Generate Personal Access Token:**
1. Go to your Confluence instance
2. Click your profile → Settings
3. Select "Personal Access Tokens"
4. Click "Create token"

### Alternative: Credentials File

The Unix installer creates `~/.consync-app/credentials` where you can store credentials:

```bash
export CONFLUENCE_USERNAME="your-email@example.com"
export CONFLUENCE_API_TOKEN="your-api-token"
```

This file is automatically sourced when running ConSync.

---

## Verification

After installation and authentication setup, verify ConSync is working:

```bash
# Check version
consync --version

# Test connection (dry run)
consync sync /path/to/your/docs --dry-run
```

---

## Troubleshooting

### "Java not found" or "Unsupported Java version"

**Problem:** ConSync requires Java 21 or higher.

**Solution:**
- Install Java 21+ (see [Prerequisites](#prerequisites))
- Verify installation: `java -version`
- Ensure Java is in your PATH

### "Command not found: consync"

**Problem:** ConSync is not in your PATH.

**Solutions:**

**Windows:**
1. Restart your terminal after installation
2. Check if `%LOCALAPPDATA%\ConSync` is in your PATH
3. Run as: `java -jar C:\path\to\consync.jar`

**macOS/Linux:**
1. Check if `/usr/local/bin` is in your PATH: `echo $PATH`
2. Verify the symlink exists: `ls -la /usr/local/bin/consync`
3. Re-run the installer with appropriate permissions

### "Authentication failed"

**Problem:** Invalid or missing credentials.

**Solutions:**
1. Verify environment variables are set: `echo $CONFLUENCE_USERNAME`
2. Regenerate API token/PAT
3. Check for typos in credentials
4. Ensure no extra spaces or quotes

### "UnsupportedClassVersionError"

**Problem:** Java version too old (compiled with Java 21).

**Solution:**
- Update to Java 21 or higher
- Verify: `java -version` shows version 21+

### Scoop installation fails

**Problem:** Various Scoop-related errors.

**Solutions:**
1. Update Scoop: `scoop update`
2. Check Scoop status: `scoop checkup`
3. Remove and re-add bucket:
   ```powershell
   scoop bucket rm consync
   scoop bucket add consync https://github.com/MykullZeroOne/scoop-consync
   scoop install consync
   ```

### Need more help?

- Check the [README](README.md) for usage examples
- Review the [documentation](examples/index.md)
- Open an issue on [GitHub](https://github.com/MykullZeroOne/ConSync/issues)

---

## Next Steps

Once installed, see:
- [Quick Start Guide](examples/getting-started/quickstart.md)
- [Configuration Guide](examples/guides/configuration.md)
- [Usage Examples](examples/guides/usage.md)
