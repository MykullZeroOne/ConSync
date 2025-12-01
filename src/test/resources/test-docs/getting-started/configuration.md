---
title: Configuration
description: ConSync configuration reference
weight: 3
---

# Configuration Reference

ConSync uses a YAML configuration file.

## Configuration File Location

ConSync looks for configuration in the following locations:

1. `./consync.yaml` (current directory)
2. `./consync.yml`
3. Path specified with `--config` flag

## Full Configuration Example

```yaml
confluence:
  base_url: https://your-domain.atlassian.net/wiki
  username: your.email@company.com
  api_token: ${CONFLUENCE_API_TOKEN}
  timeout: 30
  retry_count: 3

space:
  key: DOCS
  root_page_id: "123456"

content:
  title_source: frontmatter
  toc:
    enabled: true
    depth: 3
    position: top

sync:
  delete_orphans: false
  state_file: .consync/state.json

files:
  include:
    - "**/*.md"
  exclude:
    - "**/node_modules/**"
    - "**/.git/**"
  index_file: index.md

logging:
  level: info
```

## Environment Variables

All configuration values support environment variable expansion:

| Variable | Description |
|----------|-------------|
| `${CONFLUENCE_API_TOKEN}` | Your Confluence API token |
| `${CONFLUENCE_USERNAME}` | Your Confluence username |
