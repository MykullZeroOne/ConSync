# Quickstart Guide

Get your first documentation synced to Confluence in 5 minutes.

## Before You Begin

Ensure you've completed the [Installation](installation.md) guide and have:

- ConSync installed
- Confluence credentials (API token or PAT)
- A Confluence space to sync to

## Step 1: Create Your Documentation Directory

Create a new directory for your documentation:

```bash
mkdir my-docs
cd my-docs
```

## Step 2: Create Configuration File

Create a `consync.yaml` configuration file:

```yaml
# Confluence connection
confluence:
  base_url: "https://your-domain.atlassian.net/wiki"
  username: "your-email@example.com"
  api_token: "${CONFLUENCE_API_TOKEN}"

# Target space
space:
  key: "DOCS"

# Content settings
content:
  root_page: "My Documentation"
  toc:
    enabled: true
    depth: 3
```

**Important**: Update `base_url`, `username`, and `space.key` with your values.

## Step 3: Write Some Markdown

Create your first markdown file `index.md`:

```markdown
# Welcome to My Documentation

This is the main page of my documentation.

## Overview

This documentation covers:

- Product features
- Installation guides
- API reference

## Quick Links

- [Getting Started](getting-started.md)
- [API Documentation](api/index.md)
```

Create a getting started guide `getting-started.md`:

```markdown
# Getting Started

Follow these steps to get started:

## Installation

Install the application:

\`\`\`bash
npm install my-app
\`\`\`

## Configuration

Configure the application:

\`\`\`bash
my-app init
\`\`\`

## First Run

Run the application:

\`\`\`bash
my-app start
\`\`\`

You're all set! Return to [home](index.md) for more information.
```

Create an API directory with documentation:

```bash
mkdir api
```

Create `api/index.md`:

```markdown
# API Documentation

Welcome to the API documentation.

## Available APIs

- [User API](users.md)
- [Data API](data.md)

## Authentication

All API requests require an API key. See [Getting Started](../getting-started.md) for details.
```

Create `api/users.md`:

```markdown
# User API

The User API provides endpoints for managing users.

## Get User

\`\`\`
GET /api/users/{id}
\`\`\`

Returns user information.

## Create User

\`\`\`
POST /api/users
\`\`\`

Creates a new user.

Back to [API Documentation](index.md)
```

Your directory structure should look like:

```
my-docs/
├── consync.yaml
├── index.md
├── getting-started.md
└── api/
    ├── index.md
    └── users.md
```

## Step 4: Preview Changes (Dry Run)

Before syncing, preview what ConSync will do:

```bash
java -jar /path/to/consync.jar sync --dry-run .
```

You should see output like:

```
Configuration loaded successfully
  Space: DOCS
  Base URL: https://your-domain.atlassian.net/wiki

DRY RUN MODE - No changes will be made to Confluence

Sync Plan:
  CREATE: My Documentation (root)
    CREATE: Getting Started
    CREATE: API Documentation (parent)
      CREATE: User API

Total: 4 pages to create, 0 to update, 0 to delete
```

## Step 5: Sync to Confluence

If the dry run looks good, perform the actual sync:

```bash
java -jar /path/to/consync.jar sync .
```

ConSync will:

1. Connect to Confluence
2. Create the root page "My Documentation"
3. Create child pages for each markdown file
4. Convert markdown links to Confluence page links
5. Generate table of contents for each page

## Step 6: Verify in Confluence

1. Open your Confluence space
2. Navigate to the "My Documentation" page
3. Verify the hierarchy:
   - My Documentation
     - Getting Started
     - API Documentation
       - User API

4. Check that internal links work (e.g., clicking "Getting Started" from the main page)

## Making Updates

When you update your local markdown files:

```bash
# Edit a file
echo "\n## New Section\n\nNew content here." >> getting-started.md

# Sync changes
java -jar /path/to/consync.jar sync .
```

ConSync will detect changes and update only the modified pages.

## Next Steps

Congratulations! You've synced your first documentation to Confluence.

### Learn More

- [Configuration Guide](../guides/configuration.md) - Explore all configuration options
- [Usage Guide](../guides/usage.md) - Learn about commands and workflows
- [Hierarchy Management](../guides/advanced/hierarchy.md) - Advanced hierarchy patterns

### Best Practices

- **Version Control**: Put your docs in Git
- **CI/CD Integration**: Automate syncs on commit
- **Dry Run First**: Always test with `--dry-run`
- **State Tracking**: Commit the `.consync/` directory

### Common Tasks

**Add new pages**: Create new `.md` files and sync

**Reorganize**: Move files in directory structure and sync

**Delete pages**: Remove `.md` files and sync with `delete_orphans: true`

**Update existing**: Edit files and sync

## Troubleshooting

### Pages Not Creating

- Verify space key is correct
- Ensure you have permissions to create pages
- Check that `root_page` doesn't conflict with existing page

### Links Not Converting

- Use relative paths: `[link](../other.md)`
- Ensure target file exists
- Check file extension is `.md`

### Changes Not Detected

- ConSync uses content hashing to detect changes
- Whitespace-only changes may not trigger updates
- Use `--force` flag to update all pages

## Need Help?

Refer to the [Guides](../guides/index.md) section for more detailed information.
