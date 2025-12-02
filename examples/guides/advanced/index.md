# Advanced Topics

Advanced features and patterns for ConSync power users.

## Overview

This section covers advanced ConSync usage scenarios, complex configurations, and optimization techniques.

## Topics

### [Hierarchy Management](hierarchy.md)

Learn how to manage complex page hierarchies, including:
- Multi-level directory structures
- Custom parent-child relationships
- Index files and directory representation
- Flattening and restructuring hierarchies

### [Authentication](authentication.md)

Detailed authentication configuration for different Confluence deployments:
- Confluence Cloud with API tokens
- Data Center/Server with Personal Access Tokens
- SSO and advanced authentication scenarios
- Credential management and security

## Advanced Patterns

### Monorepo Documentation

Managing documentation for multiple projects in a single repository:

```
monorepo/
├── project-a/
│   └── docs/
│       ├── consync.yaml
│       └── index.md
├── project-b/
│   └── docs/
│       ├── consync.yaml
│       └── index.md
└── shared-docs/
    ├── consync.yaml
    └── index.md
```

Sync each independently:

```bash
consync sync project-a/docs/
consync sync project-b/docs/
consync sync shared-docs/
```

### Multi-Space Sync

Sync same content to multiple spaces with different configurations:

```bash
# Development space
consync sync --config consync.dev.yaml \
  --space DEV docs/

# Staging space
consync sync --config consync.staging.yaml \
  --space STAGING docs/

# Production space
consync sync --config consync.prod.yaml \
  --space DOCS docs/
```

### Selective Sync

Use file patterns to sync subsets of documentation:

**Config for API docs only:**

```yaml
files:
  include:
    - "api/**/*.md"
  exclude:
    - "**/_internal/**"
```

**Config for public docs only:**

```yaml
files:
  include:
    - "**/*.md"
  exclude:
    - "**/_internal/**"
    - "**/_drafts/**"
    - "**/confidential/**"
```

### Version-Specific Documentation

Maintain documentation for multiple versions:

```
docs/
├── v1.0/
│   ├── consync.yaml  # space: DOCS-V1
│   └── index.md
├── v2.0/
│   ├── consync.yaml  # space: DOCS-V2
│   └── index.md
└── latest/
    ├── consync.yaml  # space: DOCS
    └── index.md
```

### Cross-Repository Links

Link between documentation in different repositories:

```markdown
<!-- Use full Confluence URLs for cross-repo links -->
See [Other Project Docs](https://confluence.company.com/display/OTHER/Page)

<!-- Or use Confluence page IDs -->
See [Other Project Docs](https://confluence.company.com/pages/viewpage.action?pageId=123456)
```

## Performance Optimization

### Incremental Sync

ConSync automatically detects changes, but you can optimize:

**Use state file efficiently:**

```yaml
sync:
  state_file: ".consync/state.json"
  update_unchanged: false  # Skip unchanged pages
```

**Commit state file to Git** to track changes across machines.

### Parallel Processing

For large documentation sets, ConSync processes pages in parallel. Control with environment variables:

```bash
# Increase parallel requests
export CONSYNC_PARALLEL_REQUESTS=10

consync sync docs/
```

### Caching

ConSync caches Confluence API responses. Control cache behavior:

```yaml
confluence:
  # Shorter timeout for faster failures
  timeout: 15

  # Fewer retries
  retry_count: 2
```

## Error Handling and Recovery

### Handling Failures

When sync fails partway through:

1. **Check state file:** `.consync/state.json` tracks what was synced
2. **Review errors:** Use `--debug` for detailed error info
3. **Re-run sync:** ConSync will resume from where it stopped

### Manual Recovery

If state becomes corrupted:

```bash
# Delete state file
rm .consync/state.json

# Force full sync
consync sync --force docs/
```

### Conflict Resolution

Handle conflicts when both local and Confluence have changes:

**Strategy 1: Always local (default)**

```yaml
sync:
  conflict_resolution: local
```

Overwrites Confluence changes with local content.

**Strategy 2: Ask per conflict**

```yaml
sync:
  conflict_resolution: ask
```

Prompts for each conflict:

```
Conflict detected for page: Getting Started
Local modified: 2024-01-15 10:30
Remote modified: 2024-01-15 11:45

Choose action:
  [l] Use local version
  [r] Use remote version
  [d] Show diff
  [s] Skip this page
```

**Strategy 3: Prefer remote**

```yaml
sync:
  conflict_resolution: remote
```

Skips update if Confluence is newer.

## Security and Compliance

### Audit Logging

Enable detailed logging for compliance:

```yaml
logging:
  level: info
  file: ".consync/audit.log"
```

Log includes:
- All API requests
- Changes made
- Timestamp and user
- Success/failure status

### Sensitive Content

Exclude sensitive files:

```yaml
files:
  exclude:
    - "**/confidential/**"
    - "**/*.secret.md"
    - "**/internal-only/**"
```

### Access Control

Ensure ConSync API token has minimal required permissions:

- Read/Write access to target space
- No admin permissions
- Scoped to specific spaces if possible

## Automation and Scripting

### Pre-Sync Hook

Run checks before syncing:

```bash
#!/bin/bash
# pre-sync.sh

# Validate markdown
markdownlint docs/

# Check for broken links
markdown-link-check docs/**/*.md

# Run consync if checks pass
if [ $? -eq 0 ]; then
  consync sync docs/
fi
```

### Post-Sync Notification

Notify team after sync:

```bash
#!/bin/bash
# post-sync.sh

consync sync docs/

if [ $? -eq 0 ]; then
  curl -X POST https://slack.com/api/chat.postMessage \
    -H "Authorization: Bearer $SLACK_TOKEN" \
    -d "channel=#docs" \
    -d "text=Documentation synced successfully!"
fi
```

### Scheduled Sync

Use cron for automatic syncing:

```cron
# Sync every day at 2 AM
0 2 * * * cd /path/to/docs && consync sync .
```

### CI/CD Integration

Advanced GitHub Actions workflow:

```yaml
name: Sync Documentation

on:
  push:
    branches: [main, develop]
    paths: ['docs/**']
  schedule:
    - cron: '0 2 * * *'  # Daily at 2 AM

jobs:
  validate:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Validate markdown
        run: markdownlint docs/

  sync:
    needs: validate
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-java@v3
        with:
          java-version: '17'

      - name: Cache Maven packages
        uses: actions/cache@v3
        with:
          path: ~/.m2
          key: ${{ runner.os }}-m2-${{ hashFiles('**/pom.xml') }}

      - name: Build ConSync
        run: mvn clean package -DskipTests

      - name: Sync to appropriate space
        env:
          CONFLUENCE_API_TOKEN: ${{ secrets.CONFLUENCE_API_TOKEN }}
        run: |
          if [ "${{ github.ref }}" == "refs/heads/main" ]; then
            consync sync --space DOCS docs/
          else
            consync sync --space DOCS-DEV docs/
          fi

      - name: Notify on failure
        if: failure()
        run: |
          curl -X POST ${{ secrets.SLACK_WEBHOOK }} \
            -d '{"text":"Documentation sync failed!"}'
```

## Troubleshooting Advanced Issues

### Large File Sets

For thousands of files:

1. **Split into multiple spaces**
2. **Use selective sync patterns**
3. **Increase timeouts:**

```yaml
confluence:
  timeout: 60
  retry_count: 5
```

### Rate Limiting

If hitting Confluence rate limits:

1. **Reduce parallel requests**
2. **Add delays between operations**
3. **Use exponential backoff (automatic)**

### Memory Issues

For very large documentation:

```bash
# Increase JVM heap size
java -Xmx2g -jar consync.jar sync docs/
```

## Best Practices Summary

1. **Use dry run before syncing**
2. **Version control everything** (docs, config, state)
3. **Automate validation** (linting, link checking)
4. **Monitor sync results**
5. **Use environment-specific configs**
6. **Keep credentials secure**
7. **Document your hierarchy**
8. **Test with subsets first**
9. **Enable audit logging**
10. **Have a rollback plan**

## Next Steps

- Read about [Hierarchy Management](hierarchy.md)
- Learn about [Authentication](authentication.md)
- Check [Configuration Reference](../../reference/configuration-options.md)
