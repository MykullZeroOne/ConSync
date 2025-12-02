# Usage Guide

Complete guide to using ConSync commands and workflows.

## Overview

ConSync provides a command-line interface for synchronizing markdown documentation to Confluence. This guide covers all commands, options, and common usage patterns.

## Basic Command Structure

```bash
java -jar consync.jar <command> [options] <directory>
```

## Commands

### sync

Synchronize local Markdown files to Confluence.

**Syntax:**

```bash
consync sync [options] <directory>
```

**Arguments:**

- `<directory>`: Path to documentation directory containing `consync.yaml` (default: current directory)

**Options:**

- `-n, --dry-run`: Preview changes without modifying Confluence
- `-f, --force`: Force update all pages regardless of change detection
- `-c, --config <file>`: Specify alternate config file
- `-s, --space <key>`: Override space key from config
- `-v, --verbose`: Enable verbose output
- `-d, --debug`: Enable debug output

**Examples:**

```bash
# Sync current directory
consync sync .

# Sync specific directory
consync sync /path/to/docs

# Dry run (preview changes)
consync sync --dry-run /path/to/docs

# Force update all pages
consync sync --force /path/to/docs

# Use alternate config
consync sync --config consync.prod.yaml /path/to/docs

# Override space key
consync sync --space DEV /path/to/docs

# Verbose output
consync sync --verbose /path/to/docs
```

### validate

Validate configuration file without syncing.

**Syntax:**

```bash
consync validate [options] <directory>
```

**Examples:**

```bash
# Validate config in current directory
consync validate .

# Validate specific directory
consync validate /path/to/docs

# Validate alternate config
consync validate --config custom.yaml .
```

### status

Show current sync status and pending changes.

**Syntax:**

```bash
consync status [options] <directory>
```

**Examples:**

```bash
# Show status
consync status .

# Show detailed status
consync status --verbose .
```

### diff

Show differences between local and Confluence content.

**Syntax:**

```bash
consync diff [options] <directory>
```

**Examples:**

```bash
# Show diff
consync diff .

# Show detailed diff
consync diff --verbose .
```

## Global Options

Available for all commands:

- `--version`: Show ConSync version
- `--help`: Show help message
- `-v, --verbose`: Enable verbose output
- `-d, --debug`: Enable debug logging

**Examples:**

```bash
# Show version
consync --version

# Show help
consync --help

# Show command-specific help
consync sync --help
```

## Common Workflows

### Initial Setup and First Sync

1. **Create documentation directory:**

```bash
mkdir my-docs
cd my-docs
```

2. **Create configuration:**

```bash
cat > consync.yaml <<EOF
confluence:
  base_url: "https://your-domain.atlassian.net/wiki"
  username: "your-email@example.com"
  api_token: "\${CONFLUENCE_API_TOKEN}"
space:
  key: "DOCS"
content:
  root_page: "Documentation"
EOF
```

3. **Create initial documentation:**

```bash
echo "# Documentation Home" > index.md
echo "Welcome to the docs!" >> index.md
```

4. **Test with dry run:**

```bash
consync sync --dry-run .
```

5. **Perform first sync:**

```bash
consync sync .
```

### Daily Documentation Updates

1. **Edit markdown files:**

```bash
vim getting-started.md
```

2. **Preview changes:**

```bash
consync sync --dry-run .
```

3. **Sync to Confluence:**

```bash
consync sync .
```

4. **Commit to Git:**

```bash
git add .
git commit -m "Update getting started guide"
git push
```

### Adding New Pages

1. **Create new markdown file:**

```bash
echo "# New Feature Guide" > new-feature.md
```

2. **Link from existing page:**

```markdown
<!-- In index.md -->
- [New Feature Guide](new-feature.md)
```

3. **Sync:**

```bash
consync sync .
```

### Reorganizing Documentation

1. **Move files:**

```bash
mkdir guides
mv feature-*.md guides/
```

2. **Update links in markdown files** (ConSync will update Confluence links)

3. **Preview changes:**

```bash
consync sync --dry-run .
```

4. **Sync:**

```bash
consync sync .
```

### Deleting Pages

**Option 1: Safe Delete (default)**

1. **Delete local file:**

```bash
rm old-guide.md
```

2. **Sync:**

```bash
consync sync .
```

3. **Manual cleanup:** Orphaned pages remain in Confluence (manual delete required)

**Option 2: Automatic Delete**

1. **Enable in config:**

```yaml
sync:
  delete_orphans: true
```

2. **Delete local file:**

```bash
rm old-guide.md
```

3. **Preview:**

```bash
consync sync --dry-run .
```

4. **Sync (will delete Confluence page):**

```bash
consync sync .
```

### Working with Branches

When using Git branches for documentation:

```bash
# Feature branch
git checkout -b feature/new-docs

# Make changes
echo "# New Section" >> guide.md

# Sync to dev space
consync sync --space DEV .

# Merge to main
git checkout main
git merge feature/new-docs

# Sync to production space
consync sync --space DOCS .
```

### Batch Updates

When making many changes:

```bash
# Make multiple edits
# ... edit files ...

# Preview all changes
consync diff .

# Dry run
consync sync --dry-run .

# Review output carefully
# If everything looks good:
consync sync .
```

## Advanced Usage

### Multiple Spaces

Sync same docs to multiple spaces:

```bash
# Dev space
consync sync --space DEV --config consync.dev.yaml docs/

# Staging space
consync sync --space STAGING --config consync.staging.yaml docs/

# Production space
consync sync --space DOCS --config consync.prod.yaml docs/
```

### Partial Sync

Sync specific subdirectories:

```bash
# Sync only API docs
consync sync docs/api/

# Sync only guides
consync sync docs/guides/
```

Note: Each subdirectory needs its own `consync.yaml` or use parent config.

### Force Update

Force update all pages (ignores change detection):

```bash
consync sync --force .
```

Useful when:
- Confluence pages were modified externally
- You updated ConSync and want to reprocess all pages
- State file is corrupted

### Custom State File

Use custom state file location:

```yaml
sync:
  state_file: "/path/to/custom/state.json"
```

Or per-environment:

```yaml
sync:
  state_file: ".consync/state.${ENVIRONMENT}.json"
```

### Verbose and Debug Output

Get detailed information:

```bash
# Verbose output
consync sync --verbose .

# Debug output (very detailed)
consync sync --debug .
```

Debug output shows:
- Configuration details
- File discovery
- Markdown processing
- API requests/responses
- State management

## Output and Feedback

### Normal Output

```
Configuration loaded successfully
  Space: DOCS
  Base URL: https://your-domain.atlassian.net/wiki

Scanning files...
Found 15 markdown files

Analyzing changes...
  3 files modified
  1 file added
  0 files deleted

Syncing to Confluence...
  ✓ Updated: Getting Started
  ✓ Created: New Feature Guide
  ✓ Updated: API Reference

Sync completed successfully!
  Created: 1 page
  Updated: 2 pages
  Deleted: 0 pages
```

### Dry Run Output

```
DRY RUN MODE - No changes will be made to Confluence

Sync Plan:
  CREATE: New Feature Guide
  UPDATE: Getting Started (content changed)
  UPDATE: API Reference (content changed)
  SKIP: User Guide (unchanged)

Summary:
  1 page will be created
  2 pages will be updated
  0 pages will be deleted
  12 pages unchanged
```

### Error Output

```
Error: Configuration file not found: consync.yaml
Create a consync.yaml file in your documentation directory.
```

```
Error loading configuration: Invalid YAML syntax at line 5
  Expected ':', found 'invalid'
```

```
Error: Connection refused
Could not connect to Confluence at https://invalid-url.com
Check your base_url configuration.
```

## Exit Codes

ConSync uses standard exit codes:

- `0`: Success
- `1`: General error
- `2`: Configuration error
- `3`: Authentication error
- `4`: Network error
- `5`: Confluence API error

Use in scripts:

```bash
#!/bin/bash
consync sync /path/to/docs
if [ $? -eq 0 ]; then
  echo "Sync successful"
else
  echo "Sync failed"
  exit 1
fi
```

## Integration with CI/CD

### GitHub Actions

```yaml
name: Sync Documentation
on:
  push:
    branches: [main]
    paths: ['docs/**']

jobs:
  sync:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3

      - uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'

      - name: Build ConSync
        run: mvn clean package

      - name: Sync to Confluence
        env:
          CONFLUENCE_API_TOKEN: ${{ secrets.CONFLUENCE_API_TOKEN }}
        run: |
          java -jar target/consync.jar sync docs/
```

### GitLab CI

```yaml
sync-docs:
  image: maven:3.9-eclipse-temurin-17
  stage: deploy
  only:
    - main
  changes:
    - docs/**
  script:
    - mvn clean package
    - java -jar target/consync.jar sync docs/
  variables:
    CONFLUENCE_API_TOKEN: $CONFLUENCE_API_TOKEN
```

### Jenkins

```groovy
pipeline {
  agent any

  stages {
    stage('Build') {
      steps {
        sh 'mvn clean package'
      }
    }

    stage('Sync Docs') {
      when {
        changeset "docs/**"
      }
      steps {
        withCredentials([string(credentialsId: 'confluence-token', variable: 'TOKEN')]) {
          sh '''
            export CONFLUENCE_API_TOKEN=$TOKEN
            java -jar target/consync.jar sync docs/
          '''
        }
      }
    }
  }
}
```

## Troubleshooting

### Sync Failing

1. **Check configuration:**
   ```bash
   consync validate .
   ```

2. **Test connectivity:**
   ```bash
   consync sync --dry-run .
   ```

3. **Enable debug logging:**
   ```bash
   consync sync --debug .
   ```

### Changes Not Detected

- Content hasn't actually changed (whitespace doesn't count)
- State file is tracking old hash
- Use `--force` to update anyway:
  ```bash
  consync sync --force .
  ```

### Permission Errors

- Verify you have permission to create/edit pages in the space
- Check space permissions in Confluence
- Verify API token has correct scopes

## Next Steps

- Explore [Advanced Hierarchy](advanced/hierarchy.md) patterns
- Learn about [Authentication](advanced/authentication.md) options
- Check [Configuration Reference](../reference/configuration-options.md)
