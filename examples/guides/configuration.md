# Configuration Guide

Complete guide to configuring ConSync using the `consync.yaml` file.

## Overview

ConSync uses a YAML configuration file named `consync.yaml` placed in your documentation directory. This file controls all aspects of synchronization behavior.

## Basic Configuration

Minimal required configuration:

```yaml
confluence:
  base_url: "https://your-domain.atlassian.net/wiki"
  username: "your-email@example.com"
  api_token: "${CONFLUENCE_API_TOKEN}"

space:
  key: "DOCS"
```

## Configuration Sections

### Confluence Connection

Controls how ConSync connects to your Confluence instance.

```yaml
confluence:
  # Required: Base URL of your Confluence instance
  base_url: "https://your-domain.atlassian.net/wiki"

  # Required for Cloud: Username (email address)
  username: "your-email@example.com"

  # Required for Cloud: API token
  api_token: "${CONFLUENCE_API_TOKEN}"

  # Alternative for Data Center/Server: Personal Access Token
  # pat: "${CONFLUENCE_PAT}"

  # Optional: Request timeout in seconds (default: 30)
  timeout: 30

  # Optional: Number of retry attempts (default: 3)
  retry_count: 3
```

**Environment Variable Substitution:**

Use `${VAR_NAME}` syntax to reference environment variables. This is recommended for credentials:

```bash
export CONFLUENCE_API_TOKEN="your-token"
```

### Space Configuration

Defines the target Confluence space and root page.

```yaml
space:
  # Required: Space key (visible in Confluence URLs)
  key: "DOCS"

  # Optional: Space display name
  name: "Documentation"

  # Optional: Specific root page ID to sync under
  # root_page_id: "123456789"

  # Optional: Root page title (alternative to root_page_id)
  root_page_title: "Documentation Home"
```

**Choosing Root Page:**

- **Without `root_page_id` or `root_page_title`**: Creates pages at space root level
- **With `root_page_title`**: Creates pages under the specified page (creates if doesn't exist)
- **With `root_page_id`**: Creates pages under the page with that ID

### Content Settings

Controls how markdown content is processed and converted.

```yaml
content:
  # Optional: Root page title for synced content
  root_page: "Documentation"

  # How to determine page titles
  # Options: filename, frontmatter, first_heading
  title_source: filename

  # Table of contents configuration
  toc:
    enabled: true      # Generate TOC for each page
    depth: 3           # Maximum heading depth (1-6)
    position: top      # Position: top, bottom, none

  # Frontmatter handling
  frontmatter:
    strip: true        # Remove YAML frontmatter from content
    use_title: true    # Use title from frontmatter if present
```

**Title Source Options:**

- `filename`: Use filename as title (e.g., `getting-started.md` → "Getting Started")
- `frontmatter`: Use `title` field from frontmatter
- `first_heading`: Use first H1 heading from content

**Example with frontmatter:**

```markdown
---
title: "Custom Page Title"
labels: [documentation, guide]
---

# Getting Started

Content here...
```

### Sync Behavior

Controls how ConSync handles synchronization.

```yaml
sync:
  # Delete Confluence pages not in local files
  delete_orphans: false

  # Update pages even if content hasn't changed
  update_unchanged: false

  # Preserve existing Confluence labels on update
  preserve_labels: true

  # Conflict resolution strategy
  # Options: local, remote, ask
  conflict_resolution: local

  # Path to state file for tracking sync status
  state_file: ".consync/state.json"
```

**Conflict Resolution Strategies:**

- `local`: Always use local content (overwrites Confluence)
- `remote`: Skip update if Confluence has newer changes
- `ask`: Prompt user for each conflict (interactive mode)

**Delete Orphans:**

When enabled, ConSync will delete pages in Confluence that don't have corresponding local files. Use with caution!

```yaml
sync:
  delete_orphans: true  # Will delete pages not in local files
```

### File Handling

Controls which files are included in synchronization.

```yaml
files:
  # Glob patterns for files to include
  include:
    - "**/*.md"         # All markdown files
    - "*.md"            # Root level markdown files

  # Glob patterns for files to exclude
  exclude:
    - "**/node_modules/**"
    - "**/_drafts/**"
    - "**/README.md"
    - "**/.git/**"
    - "**/TODO.md"

  # Name of index files (represent directory content)
  index_file: "index.md"
```

**Glob Pattern Examples:**

- `*.md`: All `.md` files in root directory
- `**/*.md`: All `.md` files in any directory
- `guides/*.md`: All `.md` files in `guides/` directory
- `**/images/**`: All files in any `images/` directory

**Index Files:**

Files named `index.md` (or your configured `index_file`) provide content for directory-level pages:

```
guides/
├── index.md      # Content for "Guides" page
├── guide1.md     # Child page
└── guide2.md     # Child page
```

### Logging

Controls logging behavior.

```yaml
logging:
  # Log level: debug, info, warn, error
  level: info

  # Optional: Write logs to file
  file: ".consync/consync.log"
```

## Complete Example

Here's a complete configuration with all options:

```yaml
# Confluence connection
confluence:
  base_url: "https://your-domain.atlassian.net/wiki"
  username: "your-email@example.com"
  api_token: "${CONFLUENCE_API_TOKEN}"
  timeout: 30
  retry_count: 3

# Target space
space:
  key: "DOCS"
  name: "Documentation"
  root_page_title: "Documentation Home"

# Content processing
content:
  root_page: "Documentation"
  title_source: frontmatter
  toc:
    enabled: true
    depth: 3
    position: top
  frontmatter:
    strip: true
    use_title: true

# Sync behavior
sync:
  delete_orphans: false
  update_unchanged: false
  preserve_labels: true
  conflict_resolution: local
  state_file: ".consync/state.json"

# File patterns
files:
  include:
    - "**/*.md"
  exclude:
    - "**/node_modules/**"
    - "**/_drafts/**"
    - "**/README.md"
  index_file: "index.md"

# Logging
logging:
  level: info
  file: ".consync/consync.log"
```

## Configuration Validation

Validate your configuration without syncing:

```bash
consync validate /path/to/docs
```

This checks for:

- Required fields present
- Valid enum values
- Valid URLs
- File path accessibility

## Environment-Specific Configurations

Use different configs for different environments:

```bash
# Development
consync sync --config consync.dev.yaml docs/

# Production
consync sync --config consync.prod.yaml docs/
```

**Development config** (`consync.dev.yaml`):

```yaml
confluence:
  base_url: "https://dev-confluence.company.com"
space:
  key: "DEV"
```

**Production config** (`consync.prod.yaml`):

```yaml
confluence:
  base_url: "https://confluence.company.com"
space:
  key: "DOCS"
```

## Command-Line Overrides

Override configuration values via command-line:

```bash
# Override space key
consync sync --space DOCS-TEST /path/to/docs

# Use alternate config file
consync sync --config custom.yaml /path/to/docs

# Enable dry run (doesn't require config change)
consync sync --dry-run /path/to/docs
```

## Security Best Practices

### Never Commit Credentials

Always use environment variables for sensitive data:

```yaml
# Good
api_token: "${CONFLUENCE_API_TOKEN}"

# Bad - never do this
api_token: "your-actual-token-here"
```

### Use .gitignore

Exclude sensitive and generated files:

```gitignore
# State files
.consync/

# Logs
*.log

# Environment-specific configs with credentials
consync.*.yaml
!consync.example.yaml
```

### Restrict Configuration Access

Set appropriate file permissions:

```bash
chmod 600 consync.yaml
```

## Troubleshooting Configuration

### Configuration File Not Found

**Error**: `Configuration file not found: consync.yaml`

**Solution**: Create `consync.yaml` in your documentation directory, or specify path:

```bash
consync sync --config /path/to/config.yaml docs/
```

### Invalid YAML Syntax

**Error**: `Error loading configuration: Invalid YAML`

**Solution**: Validate YAML syntax using a validator or:

```bash
python -c "import yaml; yaml.safe_load(open('consync.yaml'))"
```

### Environment Variable Not Set

**Error**: `Environment variable CONFLUENCE_API_TOKEN not set`

**Solution**: Export the variable:

```bash
export CONFLUENCE_API_TOKEN="your-token"
```

Or set in your shell profile (`.bashrc`, `.zshrc`).

### Invalid Base URL

**Error**: `Invalid base_url format`

**Solution**: Ensure URL format is correct:

- Include scheme: `https://`
- Include `/wiki` for Cloud: `https://domain.atlassian.net/wiki`
- No trailing slash

## Next Steps

- Learn about [Usage](usage.md) patterns and commands
- Explore [Advanced Authentication](advanced/authentication.md)
- See [Configuration Reference](../reference/configuration-options.md) for all options
