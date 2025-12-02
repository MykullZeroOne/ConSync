# Reference Documentation

Complete reference documentation for ConSync configuration and command-line interface.

## Overview

This section provides detailed reference information for all ConSync features, options, and configurations.

## Reference Sections

### [Configuration Options](configuration-options.md)

Complete reference for all `consync.yaml` configuration options including:
- All configuration sections
- Every available option
- Data types and valid values
- Default values
- Examples for each option

## Quick Reference

### Command Syntax

```bash
consync <command> [options] <directory>
```

### Available Commands

| Command | Description |
|---------|-------------|
| `sync` | Synchronize local markdown to Confluence |
| `validate` | Validate configuration file |
| `status` | Show current sync status |
| `diff` | Show differences between local and Confluence |

### Common Options

| Option | Short | Description |
|--------|-------|-------------|
| `--dry-run` | `-n` | Preview changes without syncing |
| `--force` | `-f` | Force update all pages |
| `--config` | `-c` | Specify config file path |
| `--space` | `-s` | Override space key |
| `--verbose` | `-v` | Enable verbose output |
| `--debug` | `-d` | Enable debug output |
| `--help` | | Show help message |
| `--version` | | Show version |

### Configuration Quick Reference

**Minimal configuration:**

```yaml
confluence:
  base_url: "https://your-domain.atlassian.net/wiki"
  username: "your-email@example.com"
  api_token: "${CONFLUENCE_API_TOKEN}"
space:
  key: "DOCS"
```

**Required fields:**
- `confluence.base_url`
- `confluence.username` (for Cloud) OR `confluence.pat` (for Server)
- `confluence.api_token` (for Cloud) OR `confluence.pat` (for Server)
- `space.key`

## Environment Variables

### Authentication

| Variable | Used For | Example |
|----------|----------|---------|
| `CONFLUENCE_API_TOKEN` | Cloud API authentication | `export CONFLUENCE_API_TOKEN="token"` |
| `CONFLUENCE_PAT` | Server/DC authentication | `export CONFLUENCE_PAT="token"` |

### Advanced

| Variable | Purpose | Default |
|----------|---------|---------|
| `CONSYNC_PARALLEL_REQUESTS` | Max parallel API calls | `5` |
| `CONSYNC_CACHE_DIR` | API response cache dir | `.consync/cache` |
| `CONSYNC_LOG_LEVEL` | Override log level | `info` |

## Exit Codes

| Code | Meaning |
|------|---------|
| `0` | Success |
| `1` | General error |
| `2` | Configuration error |
| `3` | Authentication error |
| `4` | Network error |
| `5` | Confluence API error |

## File Patterns

ConSync uses glob patterns for file inclusion/exclusion:

| Pattern | Matches |
|---------|---------|
| `*.md` | All `.md` files in root |
| `**/*.md` | All `.md` files in any directory |
| `docs/**/*.md` | All `.md` files under `docs/` |
| `**/README.md` | All `README.md` files |
| `guides/*.md` | `.md` files in `guides/` only |
| `**/_*.md` | All files starting with `_` |

## Markdown Support

### Supported Markdown Features

- **Headings**: H1-H6
- **Text formatting**: Bold, italic, strikethrough, code
- **Lists**: Ordered, unordered, nested
- **Links**: Inline, reference-style, relative
- **Images**: Inline, reference-style
- **Code blocks**: With syntax highlighting
- **Blockquotes**: Single and nested
- **Tables**: GFM-style tables
- **Horizontal rules**
- **Task lists**: `- [ ]` and `- [x]`

### Confluence-Specific Conversions

| Markdown | Confluence |
|----------|------------|
| `[link](page.md)` | Page link |
| `![image](img.png)` | Embedded image |
| ` ```java ` | Code macro with syntax |
| `> quote` | Quote macro |
| `| table |` | Table |
| `- [ ] task` | Status macro |

### Limitations

- **HTML**: Limited support (basic tags only)
- **Custom HTML**: Not supported
- **Embedded videos**: Use Confluence macros
- **Math equations**: Not directly supported

## State File Format

ConSync maintains state in `.consync/state.json`:

```json
{
  "version": "1.0",
  "last_sync": "2024-01-15T10:30:00Z",
  "pages": {
    "index.md": {
      "page_id": "123456789",
      "title": "Documentation Home",
      "hash": "abc123...",
      "last_modified": "2024-01-15T10:30:00Z"
    }
  }
}
```

**Fields:**
- `version`: State format version
- `last_sync`: Last successful sync timestamp
- `pages`: Map of local files to Confluence pages
  - `page_id`: Confluence page ID
  - `title`: Page title
  - `hash`: Content hash for change detection
  - `last_modified`: Last modification time

## API Rate Limits

### Confluence Cloud

- **Rate limit**: 100 requests per 10 seconds per user
- **Concurrent requests**: Max 10
- **Retry behavior**: Exponential backoff

### Confluence Data Center/Server

- **Rate limit**: Varies by deployment
- **Concurrent requests**: Configured by admin
- **Retry behavior**: Exponential backoff

ConSync automatically handles rate limiting with:
- Request throttling
- Automatic retries
- Exponential backoff
- Clear error messages

## Troubleshooting Reference

### Common Error Messages

| Error | Cause | Solution |
|-------|-------|----------|
| `Configuration file not found` | Missing `consync.yaml` | Create config file |
| `401 Unauthorized` | Invalid credentials | Check token/PAT |
| `403 Forbidden` | Insufficient permissions | Check space access |
| `404 Not Found` | Invalid space/page | Verify space exists |
| `Connection timeout` | Network/URL issue | Check base URL |
| `Invalid YAML syntax` | Malformed config | Validate YAML syntax |

### Debug Commands

```bash
# Validate configuration
consync validate docs/

# Test connectivity (dry run)
consync sync --dry-run docs/

# Enable debug logging
consync sync --debug docs/

# Check environment variables
echo $CONFLUENCE_API_TOKEN

# Test Confluence connectivity
curl -u email:$CONFLUENCE_API_TOKEN \
  https://domain.atlassian.net/wiki/rest/api/space/KEY
```

## Performance Tips

1. **Use incremental sync** - Don't use `--force` unnecessarily
2. **Commit state file** - Enables change detection across machines
3. **Exclude unnecessary files** - Reduces processing time
4. **Use parallel requests** - Default is optimal for most cases
5. **Cache API responses** - Automatic, but ensure `.consync/` is writable

## Security Reference

### Best Practices

- ✓ Use environment variables for credentials
- ✓ Never commit tokens to Git
- ✓ Rotate tokens regularly
- ✓ Use minimal required permissions
- ✓ Set file permissions: `chmod 600 consync.yaml`
- ✓ Use service accounts for automation
- ✓ Enable audit logging
- ✓ Use HTTPS for base URL
- ✓ Validate certificates (don't disable SSL)

### Credentials Checklist

- [ ] Token stored in environment variable
- [ ] Config uses `${VAR}` syntax
- [ ] `.env` in `.gitignore`
- [ ] Token has appropriate permissions
- [ ] Token has expiration date
- [ ] Service account used for CI/CD
- [ ] CI/CD uses secret management
- [ ] Config file permissions restricted

## Version Compatibility

| ConSync Version | Java Version | Confluence Cloud | Confluence DC/Server |
|-----------------|--------------|------------------|---------------------|
| 1.0.x | 17+ | ✓ | 7.0+ |

## Further Reading

- [Configuration Options](configuration-options.md) - Detailed config reference
- [Usage Guide](../guides/usage.md) - Command usage and workflows
- [Advanced Topics](../guides/advanced/index.md) - Advanced features
