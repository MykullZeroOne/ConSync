# Authentication

Detailed guide to authenticating ConSync with Confluence Cloud and Data Center/Server.

## Overview

ConSync supports multiple authentication methods depending on your Confluence deployment. This guide covers setup, security best practices, and troubleshooting.

## Confluence Cloud Authentication

Confluence Cloud uses **API tokens** for authentication.

### Creating an API Token

1. **Log in to Atlassian Account:**

Navigate to [https://id.atlassian.com/manage-profile/security/api-tokens](https://id.atlassian.com/manage-profile/security/api-tokens)

2. **Create Token:**

- Click "Create API token"
- Enter a label (e.g., "ConSync Production")
- Click "Create"
- **Copy the token immediately** (you won't see it again)

3. **Save Token Securely:**

Store in password manager or environment variable (never in code).

### Configuring Cloud Authentication

**Configuration file:**

```yaml
confluence:
  base_url: "https://your-domain.atlassian.net/wiki"
  username: "your-email@example.com"
  api_token: "${CONFLUENCE_API_TOKEN}"
```

**Environment variable:**

```bash
# Add to ~/.bashrc or ~/.zshrc
export CONFLUENCE_API_TOKEN="your-api-token-here"

# Or set for single command
CONFLUENCE_API_TOKEN="your-token" consync sync docs/
```

**Important:**
- Use your **Atlassian account email** as username
- Include `/wiki` in the base URL
- **Never commit the token to Git**

### Testing Cloud Authentication

```bash
# Set token
export CONFLUENCE_API_TOKEN="your-token"

# Test with dry run
consync sync --dry-run docs/

# Expected output:
# Configuration loaded successfully
#   Space: DOCS
#   Base URL: https://your-domain.atlassian.net/wiki
```

## Data Center/Server Authentication

Confluence Data Center and Server use **Personal Access Tokens (PAT)**.

### Creating a Personal Access Token

1. **Log in to Confluence:**

Navigate to your Confluence instance.

2. **Access Settings:**

- Click your profile picture (top right)
- Select "Settings"
- Go to "Personal Access Tokens" (left sidebar)

3. **Create Token:**

- Click "Create token"
- Enter a name (e.g., "ConSync Sync")
- Set expiration date (or make it permanent)
- Click "Create"
- **Copy the token immediately**

### Configuring Data Center/Server Authentication

**Configuration file:**

```yaml
confluence:
  base_url: "https://confluence.yourcompany.com"
  pat: "${CONFLUENCE_PAT}"
```

**Note:** When using PAT, you **don't need** `username` or `api_token`.

**Environment variable:**

```bash
# Add to ~/.bashrc or ~/.zshrc
export CONFLUENCE_PAT="your-personal-access-token"

# Or set for single command
CONFLUENCE_PAT="your-token" consync sync docs/
```

### Testing Server Authentication

```bash
# Set token
export CONFLUENCE_PAT="your-pat"

# Test with dry run
consync sync --dry-run docs/

# Expected output:
# Configuration loaded successfully
#   Space: DOCS
#   Base URL: https://confluence.yourcompany.com
```

## Security Best Practices

### 1. Use Environment Variables

**Never** commit credentials to Git:

```yaml
# Good ✓
api_token: "${CONFLUENCE_API_TOKEN}"

# Bad ✗
api_token: "ATATTxxx..."
```

### 2. Scope Tokens Appropriately

**API Token Permissions:**
- Read/write access to Confluence
- Limited to specific spaces (if possible)
- No admin permissions needed

**PAT Permissions:**
- Create and edit pages
- Read space information
- Attach files (if using attachments)

### 3. Rotate Tokens Regularly

Set expiration dates and rotate tokens:

```bash
# Update token
export CONFLUENCE_API_TOKEN="new-token"

# Test
consync sync --dry-run docs/
```

### 4. Use Separate Tokens per Environment

Different tokens for different environments:

```bash
# Development
export CONFLUENCE_API_TOKEN_DEV="dev-token"

# Production
export CONFLUENCE_API_TOKEN_PROD="prod-token"
```

**Config files:**

```yaml
# consync.dev.yaml
confluence:
  base_url: "https://dev-confluence.company.com"
  api_token: "${CONFLUENCE_API_TOKEN_DEV}"

# consync.prod.yaml
confluence:
  base_url: "https://confluence.company.com"
  api_token: "${CONFLUENCE_API_TOKEN_PROD}"
```

### 5. Restrict File Permissions

Protect configuration files:

```bash
chmod 600 consync.yaml
chmod 700 .consync/
```

### 6. Use CI/CD Secrets

In CI/CD, use secret management:

**GitHub Actions:**

```yaml
- name: Sync to Confluence
  env:
    CONFLUENCE_API_TOKEN: ${{ secrets.CONFLUENCE_API_TOKEN }}
  run: consync sync docs/
```

**GitLab CI:**

```yaml
sync-docs:
  script:
    - consync sync docs/
  variables:
    CONFLUENCE_API_TOKEN: $CONFLUENCE_API_TOKEN
```

**Jenkins:**

```groovy
withCredentials([string(credentialsId: 'confluence-token', variable: 'TOKEN')]) {
  sh '''
    export CONFLUENCE_API_TOKEN=$TOKEN
    consync sync docs/
  '''
}
```

## Advanced Authentication Scenarios

### SSO and SAML

If your Confluence uses SSO:

**For Cloud with SSO:**
- API tokens still work
- No additional configuration needed
- Token authenticates directly with Atlassian

**For Server with SSO:**
- Use Personal Access Tokens
- PAT bypasses SSO
- Ensure PATs are enabled in admin settings

### Multiple Confluence Instances

Managing multiple instances:

```bash
# Set multiple tokens
export CONFLUENCE_TOKEN_INTERNAL="internal-token"
export CONFLUENCE_TOKEN_EXTERNAL="external-token"
```

**Config files:**

```yaml
# consync.internal.yaml
confluence:
  base_url: "https://internal-confluence.company.com"
  pat: "${CONFLUENCE_TOKEN_INTERNAL}"

# consync.external.yaml
confluence:
  base_url: "https://company.atlassian.net/wiki"
  username: "user@company.com"
  api_token: "${CONFLUENCE_TOKEN_EXTERNAL}"
```

**Sync to both:**

```bash
consync sync --config consync.internal.yaml docs/
consync sync --config consync.external.yaml docs/
```

### Service Accounts

For automated syncing, use service accounts:

**Setup:**
1. Create dedicated Confluence user (e.g., `consync-bot@company.com`)
2. Grant minimal required permissions
3. Generate API token/PAT for this user
4. Use in CI/CD

**Benefits:**
- Clear audit trail
- Separate from personal accounts
- Can be managed centrally
- Easier permission management

### Token Storage Solutions

**1. Environment Variables (Simple)**

```bash
export CONFLUENCE_API_TOKEN="token"
```

**2. `.env` Files (Local Development)**

```bash
# .env
CONFLUENCE_API_TOKEN=your-token

# Load in shell
source .env
```

Add to `.gitignore`:
```
.env
```

**3. Secret Management (Production)**

**AWS Secrets Manager:**

```bash
# Store secret
aws secretsmanager create-secret \
  --name confluence-api-token \
  --secret-string "your-token"

# Retrieve in script
TOKEN=$(aws secretsmanager get-secret-value \
  --secret-id confluence-api-token \
  --query SecretString \
  --output text)

export CONFLUENCE_API_TOKEN=$TOKEN
consync sync docs/
```

**HashiCorp Vault:**

```bash
# Store secret
vault kv put secret/confluence token="your-token"

# Retrieve in script
export CONFLUENCE_API_TOKEN=$(vault kv get -field=token secret/confluence)
consync sync docs/
```

**Azure Key Vault:**

```bash
# Store secret
az keyvault secret set \
  --vault-name my-vault \
  --name confluence-token \
  --value "your-token"

# Retrieve in script
export CONFLUENCE_API_TOKEN=$(az keyvault secret show \
  --vault-name my-vault \
  --name confluence-token \
  --query value -o tsv)
consync sync docs/
```

## Troubleshooting Authentication

### 401 Unauthorized

**Error:**

```
Error: 401 Unauthorized
Authentication failed. Check your credentials.
```

**Solutions:**

1. **Verify token is set:**
   ```bash
   echo $CONFLUENCE_API_TOKEN
   ```

2. **Check token hasn't expired** (PATs can expire)

3. **Verify username** (for Cloud, must be email)

4. **Regenerate token** if lost or compromised

### 403 Forbidden

**Error:**

```
Error: 403 Forbidden
You don't have permission to access this resource.
```

**Solutions:**

1. **Check space permissions:**
   - Log in to Confluence
   - Navigate to space
   - Verify you have write access

2. **Verify token scope** (for PATs, ensure correct permissions)

3. **Check if space exists:**
   ```bash
   curl -u user@email.com:$CONFLUENCE_API_TOKEN \
     https://your-domain.atlassian.net/wiki/rest/api/space/SPACE_KEY
   ```

### Token Not Recognized

**Error:**

```
Error: Environment variable CONFLUENCE_API_TOKEN not set
```

**Solutions:**

1. **Export variable:**
   ```bash
   export CONFLUENCE_API_TOKEN="your-token"
   ```

2. **Check variable name in config:**
   ```yaml
   api_token: "${CONFLUENCE_API_TOKEN}"  # Must match export
   ```

3. **Use full path in shell:**
   ```bash
   CONFLUENCE_API_TOKEN="token" consync sync docs/
   ```

### Connection Timeout

**Error:**

```
Error: Connection timeout
Could not connect to Confluence.
```

**Solutions:**

1. **Check base URL:**
   ```yaml
   base_url: "https://your-domain.atlassian.net/wiki"  # Include /wiki for Cloud
   ```

2. **Test connectivity:**
   ```bash
   curl -I https://your-domain.atlassian.net/wiki
   ```

3. **Check proxy settings** (if behind corporate proxy)

4. **Increase timeout:**
   ```yaml
   confluence:
     timeout: 60
   ```

### Certificate Issues

**Error:**

```
Error: SSL certificate verification failed
```

**Solutions:**

1. **For self-signed certificates** (Server/Data Center):
   ```bash
   # Import certificate to Java keystore
   keytool -import -alias confluence \
     -file confluence-cert.crt \
     -keystore $JAVA_HOME/lib/security/cacerts
   ```

2. **Use proper CA-signed certificate** (recommended)

## Credential Management Checklist

- [ ] Use environment variables for tokens
- [ ] Never commit credentials to Git
- [ ] Add `.env` to `.gitignore`
- [ ] Use separate tokens per environment
- [ ] Set appropriate token permissions
- [ ] Configure token expiration
- [ ] Rotate tokens regularly
- [ ] Use service accounts for automation
- [ ] Store tokens in secret management system
- [ ] Restrict file permissions on configs
- [ ] Use CI/CD secret management
- [ ] Document token rotation procedure
- [ ] Monitor token usage

## Next Steps

- Review [Configuration Guide](../configuration.md)
- Learn about [Hierarchy Management](hierarchy.md)
- Check [Usage Guide](../usage.md) for sync commands
