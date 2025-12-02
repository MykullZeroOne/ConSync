# Guides

Comprehensive guides for using ConSync effectively.

## Overview

This section provides detailed guides covering all aspects of ConSync usage, from basic configuration to advanced scenarios.

## Available Guides

### [Configuration](configuration.md)
Complete guide to the `consync.yaml` configuration file, including all available options and examples.

### [Usage](usage.md)
Detailed information on ConSync commands, options, and common workflows.

### [Advanced Topics](advanced/index.md)
Advanced features and scenarios:
- [Hierarchy Management](advanced/hierarchy.md) - Complex directory structures and page hierarchies
- [Authentication](advanced/authentication.md) - Detailed authentication setup for different Confluence versions

## Common Workflows

### Daily Documentation Updates

The typical workflow for maintaining documentation:

1. Edit markdown files locally
2. Review changes with `git diff`
3. Test with `consync sync --dry-run`
4. Sync to Confluence with `consync sync`
5. Commit changes to Git

### Batch Updates

When making large changes:

```bash
# Make multiple edits to your markdown files
# ...

# Preview all changes
consync sync --dry-run /path/to/docs

# Review the output carefully
# If everything looks good, sync
consync sync /path/to/docs
```

### Collaborative Documentation

When working with a team:

1. **Use Git**: Version control your markdown files
2. **Review PRs**: Review documentation changes like code
3. **CI/CD**: Automate syncs on merge to main branch
4. **Conflict Resolution**: Configure `conflict_resolution: ask` for safety

### Migration from Existing Confluence

To migrate existing Confluence pages to ConSync:

1. Export Confluence pages to HTML
2. Convert HTML to Markdown (use pandoc or similar)
3. Organize files in desired structure
4. Create `consync.yaml`
5. Perform initial sync

## Best Practices

### Structure Your Documentation

Organize documentation logically:

```
docs/
├── index.md              # Overview
├── getting-started/      # New user guides
├── guides/               # How-to guides
├── reference/            # Reference docs
└── troubleshooting/      # Common issues
```

### Use Meaningful Filenames

Filenames become page titles (unless overridden):

- Good: `authentication-guide.md` → "Authentication Guide"
- Bad: `doc1.md` → "Doc1"

### Link Between Pages

Use relative links to connect pages:

```markdown
See the [Configuration Guide](../reference/configuration.md) for details.
```

### Version Your Configuration

Keep `consync.yaml` in version control alongside your docs.

### Test Before Syncing

Always run with `--dry-run` first:

```bash
consync sync --dry-run .
```

### Use Environment Variables

Never commit credentials:

```yaml
confluence:
  api_token: "${CONFLUENCE_API_TOKEN}"  # Good
  # api_token: "actual-token"           # Bad - never do this
```

### Automate with CI/CD

Example GitHub Actions workflow:

```yaml
name: Sync to Confluence
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
      - name: Sync to Confluence
        env:
          CONFLUENCE_API_TOKEN: ${{ secrets.CONFLUENCE_API_TOKEN }}
        run: |
          mvn clean package
          java -jar target/consync.jar sync docs/
```

## Tips and Tricks

### Using Index Files

Create `index.md` in directories to provide overview content for that section:

```
guides/
├── index.md        # Overview of guides section
├── guide1.md
└── guide2.md
```

### Excluding Files

Exclude drafts and private files:

```yaml
files:
  exclude:
    - "**/_drafts/**"
    - "**/*.private.md"
    - "**/TODO.md"
```

### Custom Page Titles

Use frontmatter to override filename-based titles:

```markdown
---
title: "Custom Page Title"
---

# Page content starts here
```

### Table of Contents Control

Control TOC per-page with frontmatter:

```markdown
---
toc: false
---

# Simple page without TOC
```

### Dry Run for Validation

Use dry run as a validation step in your CI:

```bash
# This will fail if config is invalid
consync sync --dry-run docs/
```

## Next Steps

- Deep dive into [Configuration](configuration.md)
- Learn about [Usage](usage.md) patterns
- Explore [Advanced Topics](advanced/index.md)
