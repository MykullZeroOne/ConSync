# Configuration Options Reference

Complete reference for all `consync.yaml` configuration options.

## Overview

This document provides detailed information about every configuration option available in ConSync.

## Configuration File Structure

```yaml
confluence:      # Confluence connection settings
  # ...
space:          # Space configuration
  # ...
content:        # Content processing
  # ...
sync:           # Sync behavior
  # ...
files:          # File handling
  # ...
logging:        # Logging configuration
  # ...
```

## Confluence Section

Connection settings for your Confluence instance.

### confluence.base_url

- **Type**: String
- **Required**: Yes
- **Description**: Base URL of your Confluence instance
- **Format**:
  - Cloud: `https://your-domain.atlassian.net/wiki`
  - Server/DC: `https://confluence.yourcompany.com`
- **Example**:
  ```yaml
  confluence:
    base_url: "https://company.atlassian.net/wiki"
  ```

**Notes:**
- Must include `https://`
- Cloud instances must include `/wiki`
- No trailing slash

### confluence.username

- **Type**: String
- **Required**: For Cloud (with api_token)
- **Description**: Confluence username/email
- **Example**:
  ```yaml
  confluence:
    username: "your-email@company.com"
  ```

**Notes:**
- Cloud: Use your Atlassian account email
- Server/DC: Not needed when using PAT

### confluence.api_token

- **Type**: String
- **Required**: For Cloud authentication
- **Description**: Atlassian API token
- **Example**:
  ```yaml
  confluence:
    api_token: "${CONFLUENCE_API_TOKEN}"
  ```

**Notes:**
- Create at: https://id.atlassian.com/manage-profile/security/api-tokens
- Use environment variable (recommended)
- Used with `username` for Cloud

### confluence.pat

- **Type**: String
- **Required**: For Server/DC authentication
- **Description**: Personal Access Token
- **Example**:
  ```yaml
  confluence:
    pat: "${CONFLUENCE_PAT}"
  ```

**Notes:**
- Alternative to username/api_token
- For Data Center/Server only
- Use environment variable (recommended)

### confluence.timeout

- **Type**: Integer
- **Required**: No
- **Default**: `30`
- **Description**: Request timeout in seconds
- **Range**: 1-600
- **Example**:
  ```yaml
  confluence:
    timeout: 60
  ```

**Notes:**
- Increase for slow connections
- Decrease for faster failure detection

### confluence.retry_count

- **Type**: Integer
- **Required**: No
- **Default**: `3`
- **Description**: Number of retry attempts for failed requests
- **Range**: 0-10
- **Example**:
  ```yaml
  confluence:
    retry_count: 5
  ```

**Notes:**
- Uses exponential backoff
- 0 = no retries

## Space Section

Confluence space configuration.

### space.key

- **Type**: String
- **Required**: Yes
- **Description**: Confluence space key
- **Format**: Uppercase alphanumeric, typically 2-10 characters
- **Example**:
  ```yaml
  space:
    key: "DOCS"
  ```

**Notes:**
- Visible in Confluence URL
- Case-sensitive
- Must exist in Confluence

### space.name

- **Type**: String
- **Required**: No
- **Description**: Space display name (informational)
- **Example**:
  ```yaml
  space:
    name: "Documentation"
  ```

**Notes:**
- Not used for sync operations
- Helpful for documentation

### space.root_page_id

- **Type**: String
- **Required**: No
- **Description**: ID of root page to sync under
- **Example**:
  ```yaml
  space:
    root_page_id: "123456789"
  ```

**Notes:**
- Find in Confluence page URL
- Alternative to `root_page_title`
- Creates pages as children of this page

### space.root_page_title

- **Type**: String
- **Required**: No
- **Description**: Title of root page to sync under
- **Example**:
  ```yaml
  space:
    root_page_title: "Documentation Home"
  ```

**Notes:**
- Alternative to `root_page_id`
- Creates page if doesn't exist
- Searches by title in space

## Content Section

Content processing configuration.

### content.root_page

- **Type**: String
- **Required**: No
- **Description**: Title for root page of synced content
- **Example**:
  ```yaml
  content:
    root_page: "Documentation"
  ```

**Notes:**
- Used when syncing multiple files
- Creates parent page for all content

### content.title_source

- **Type**: Enum
- **Required**: No
- **Default**: `filename`
- **Values**: `filename`, `frontmatter`, `first_heading`
- **Description**: How to determine page titles
- **Example**:
  ```yaml
  content:
    title_source: frontmatter
  ```

**Options:**
- `filename`: Convert filename to title (e.g., `api-docs.md` → "API Docs")
- `frontmatter`: Use `title` from YAML frontmatter
- `first_heading`: Use first H1 heading from content

### content.toc

Table of contents configuration.

#### content.toc.enabled

- **Type**: Boolean
- **Required**: No
- **Default**: `true`
- **Description**: Generate table of contents for each page
- **Example**:
  ```yaml
  content:
    toc:
      enabled: true
  ```

#### content.toc.depth

- **Type**: Integer
- **Required**: No
- **Default**: `3`
- **Range**: 1-6
- **Description**: Maximum heading depth to include in TOC
- **Example**:
  ```yaml
  content:
    toc:
      depth: 4
  ```

**Notes:**
- 1 = H1 only
- 6 = H1-H6

#### content.toc.position

- **Type**: Enum
- **Required**: No
- **Default**: `top`
- **Values**: `top`, `bottom`, `none`
- **Description**: Where to place TOC
- **Example**:
  ```yaml
  content:
    toc:
      position: top
  ```

**Options:**
- `top`: TOC at page start
- `bottom`: TOC at page end
- `none`: No TOC (same as `enabled: false`)

### content.frontmatter

Frontmatter handling configuration.

#### content.frontmatter.strip

- **Type**: Boolean
- **Required**: No
- **Default**: `true`
- **Description**: Remove YAML frontmatter from content
- **Example**:
  ```yaml
  content:
    frontmatter:
      strip: true
  ```

**Notes:**
- When true, frontmatter is removed from page content
- When false, frontmatter appears in page

#### content.frontmatter.use_title

- **Type**: Boolean
- **Required**: No
- **Default**: `true`
- **Description**: Use `title` field from frontmatter if present
- **Example**:
  ```yaml
  content:
    frontmatter:
      use_title: true
  ```

**Notes:**
- Overrides `title_source` when frontmatter has title
- Requires `strip: true` to work properly

## Sync Section

Synchronization behavior configuration.

### sync.delete_orphans

- **Type**: Boolean
- **Required**: No
- **Default**: `false`
- **Description**: Delete Confluence pages not in local files
- **Example**:
  ```yaml
  sync:
    delete_orphans: true
  ```

**⚠️ Warning:**
- When enabled, removes pages from Confluence that don't have local files
- Use with caution
- Test with `--dry-run` first

### sync.update_unchanged

- **Type**: Boolean
- **Required**: No
- **Default**: `false`
- **Description**: Update pages even if content hasn't changed
- **Example**:
  ```yaml
  sync:
    update_unchanged: false
  ```

**Notes:**
- When false, ConSync skips pages with no changes
- When true, updates all pages

### sync.preserve_labels

- **Type**: Boolean
- **Required**: No
- **Default**: `true`
- **Description**: Keep existing Confluence labels when updating
- **Example**:
  ```yaml
  sync:
    preserve_labels: true
  ```

**Notes:**
- When true, existing labels are kept
- When false, labels may be removed

### sync.conflict_resolution

- **Type**: Enum
- **Required**: No
- **Default**: `local`
- **Values**: `local`, `remote`, `ask`
- **Description**: How to handle conflicts
- **Example**:
  ```yaml
  sync:
    conflict_resolution: ask
  ```

**Options:**
- `local`: Always use local content (overwrites Confluence)
- `remote`: Skip update if Confluence has newer changes
- `ask`: Prompt user for each conflict (interactive)

### sync.state_file

- **Type**: String
- **Required**: No
- **Default**: `.consync/state.json`
- **Description**: Path to state file for tracking sync status
- **Example**:
  ```yaml
  sync:
    state_file: ".consync/state.json"
  ```

**Notes:**
- Relative to documentation directory
- Used for change detection
- Commit to Git for cross-machine sync

## Files Section

File inclusion and exclusion configuration.

### files.include

- **Type**: Array of Strings
- **Required**: No
- **Default**: `["*.md", "**/*.md"]`
- **Description**: Glob patterns for files to include
- **Example**:
  ```yaml
  files:
    include:
      - "**/*.md"
      - "**/*.markdown"
  ```

**Pattern syntax:**
- `*.md`: Files in root directory
- `**/*.md`: Files in any directory
- `docs/*.md`: Files in docs directory only
- `{docs,guides}/**/*.md`: Files in docs or guides directories

### files.exclude

- **Type**: Array of Strings
- **Required**: No
- **Default**: `[]`
- **Description**: Glob patterns for files to exclude
- **Example**:
  ```yaml
  files:
    exclude:
      - "**/node_modules/**"
      - "**/_drafts/**"
      - "**/README.md"
  ```

**Common exclusions:**
- `**/node_modules/**`: Dependencies
- `**/.*/**`: Hidden directories
- `**/_*/**`: Underscore-prefixed directories
- `**/README.md`: Repository readmes

### files.index_file

- **Type**: String
- **Required**: No
- **Default**: `index.md`
- **Description**: Name of files that represent directory content
- **Example**:
  ```yaml
  files:
    index_file: "README.md"
  ```

**Notes:**
- Files with this name provide content for directory pages
- Common values: `index.md`, `README.md`, `_index.md`

## Logging Section

Logging configuration.

### logging.level

- **Type**: Enum
- **Required**: No
- **Default**: `info`
- **Values**: `debug`, `info`, `warn`, `error`
- **Description**: Logging verbosity level
- **Example**:
  ```yaml
  logging:
    level: debug
  ```

**Levels:**
- `debug`: Very detailed (API calls, processing steps)
- `info`: Normal (high-level operations)
- `warn`: Warnings only
- `error`: Errors only

### logging.file

- **Type**: String
- **Required**: No
- **Default**: None (console only)
- **Description**: Path to log file
- **Example**:
  ```yaml
  logging:
    file: ".consync/consync.log"
  ```

**Notes:**
- Relative or absolute path
- Directory must exist
- Logs also go to console

## Complete Example

```yaml
# Confluence connection
confluence:
  base_url: "https://company.atlassian.net/wiki"
  username: "user@company.com"
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

## Validation Rules

ConSync validates configuration on load:

**Required fields:**
- `confluence.base_url`
- `space.key`
- Either:
  - `confluence.username` AND `confluence.api_token` (Cloud)
  - OR `confluence.pat` (Server/DC)

**Format validation:**
- `base_url`: Valid HTTPS URL
- `space.key`: Non-empty string
- Numeric fields: Within valid ranges
- Enum fields: Valid values only

**Validate configuration:**

```bash
consync validate docs/
```

## Environment Variable Substitution

Use `${VAR_NAME}` syntax for environment variables:

```yaml
confluence:
  base_url: "${CONFLUENCE_URL}"
  username: "${CONFLUENCE_USER}"
  api_token: "${CONFLUENCE_API_TOKEN}"
space:
  key: "${CONFLUENCE_SPACE}"
```

**Set variables:**

```bash
export CONFLUENCE_URL="https://company.atlassian.net/wiki"
export CONFLUENCE_USER="user@company.com"
export CONFLUENCE_API_TOKEN="token"
export CONFLUENCE_SPACE="DOCS"
```

## Next Steps

- Review [Configuration Guide](../guides/configuration.md) for usage examples
- Check [Usage Guide](../guides/usage.md) for command-line options
- See [Getting Started](../getting-started/index.md) for quick setup
