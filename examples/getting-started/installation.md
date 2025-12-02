# Installation

This guide walks you through installing ConSync and obtaining necessary credentials.

## Prerequisites

Ensure you have the following installed:

### Java Development Kit (JDK)

ConSync requires Java 17 or higher.

**Check your Java version:**

```bash
java -version
```

You should see output like:

```
openjdk version "17.0.8" 2023-07-18
```

**Install Java if needed:**

- **macOS**: `brew install openjdk@17`
- **Linux (Ubuntu/Debian)**: `sudo apt install openjdk-17-jdk`
- **Windows**: Download from [Adoptium](https://adoptium.net/)

### Maven (Optional, for building from source)

If you plan to build ConSync from source, install Maven 3.9.x or higher:

```bash
mvn -version
```

**Install Maven:**

- **macOS**: `brew install maven`
- **Linux (Ubuntu/Debian)**: `sudo apt install maven`
- **Windows**: Download from [Apache Maven](https://maven.apache.org/download.cgi)

## Installation Methods

### Method 1: Build from Source (Recommended)

1. **Clone the repository:**

```bash
git clone https://github.com/yourusername/consync.git
cd consync
```

2. **Build the application:**

```bash
mvn clean package
```

This creates a fat JAR at `target/consync.jar` containing all dependencies.

3. **Verify the build:**

```bash
java -jar target/consync.jar --version
```

4. **Optional: Create an alias:**

Add to your `.bashrc` or `.zshrc`:

```bash
alias consync='java -jar /path/to/consync/target/consync.jar'
```

### Method 2: Download Pre-built Binary

*Coming soon: Pre-built releases will be available on GitHub Releases.*

## Obtaining Confluence Credentials

ConSync needs credentials to access your Confluence instance.

### For Confluence Cloud (API Token)

1. **Log in to Atlassian:**

Visit [https://id.atlassian.com/manage-profile/security/api-tokens](https://id.atlassian.com/manage-profile/security/api-tokens)

2. **Create API Token:**

- Click "Create API token"
- Give it a label (e.g., "ConSync")
- Copy the token (you won't see it again)

3. **Set environment variable:**

```bash
export CONFLUENCE_API_TOKEN="your-token-here"
```

Add to your `.bashrc`/`.zshrc` to make it permanent.

4. **Note your details:**

- **Base URL**: `https://your-domain.atlassian.net/wiki`
- **Username**: Your Atlassian email address
- **API Token**: The token you just created

### For Data Center/Server (Personal Access Token)

1. **Log in to Confluence:**

Navigate to your profile settings.

2. **Create Personal Access Token:**

- Go to Settings → Personal Access Tokens
- Click "Create token"
- Set a name and expiration
- Copy the token

3. **Set environment variable:**

```bash
export CONFLUENCE_PAT="your-pat-here"
```

4. **Note your details:**

- **Base URL**: `https://confluence.yourcompany.com`
- **PAT**: The token you just created

## Verification

Test your installation and credentials:

```bash
# Test ConSync installation
java -jar target/consync.jar --help

# Create a test directory
mkdir test-docs
cd test-docs

# Create a minimal config (update with your details)
cat > consync.yaml <<EOF
confluence:
  base_url: "https://your-domain.atlassian.net/wiki"
  username: "your-email@example.com"
  api_token: "\${CONFLUENCE_API_TOKEN}"

space:
  key: "TEST"

content:
  root_page: "Test Documentation"
EOF

# Create a test markdown file
echo "# Test Page\n\nThis is a test." > test.md

# Dry run to test connectivity (doesn't modify Confluence)
java -jar ../target/consync.jar sync --dry-run .
```

If the dry run succeeds, ConSync is properly installed and configured!

## Troubleshooting

### Java Version Issues

**Error**: `UnsupportedClassVersionError`

**Solution**: Update to Java 17 or higher.

### Connection Errors

**Error**: `Connection refused` or `Unknown host`

**Solution**: Check your `base_url` in `consync.yaml`. Ensure you can access Confluence in a browser.

### Authentication Errors

**Error**: `401 Unauthorized`

**Solution**:
- Verify your API token/PAT is correct
- Ensure environment variable is set: `echo $CONFLUENCE_API_TOKEN`
- Check username matches your Confluence account

### Space Not Found

**Error**: `Space 'XXX' not found`

**Solution**:
- Verify the space key exists in Confluence
- Ensure you have permissions to access the space
- Space keys are case-sensitive

## Next Steps

Now that ConSync is installed, proceed to the [Quickstart](quickstart.md) guide to sync your first documentation.
