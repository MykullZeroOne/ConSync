---
title: Sync Command
description: Reference for the sync command
weight: 1
---

# Sync Command

Synchronize local markdown files with Confluence.

## Usage

```bash
consync sync [OPTIONS]
```

## Options

| Option | Description | Default |
|--------|-------------|---------|
| `--dry-run` | Preview changes without syncing | false |
| `--force` | Force update all pages | false |
| `--config, -c` | Configuration file path | consync.yaml |

## Examples

### Basic Sync

```bash
consync sync
```

### Dry Run

```bash
consync sync --dry-run
```

### Force Update All

```bash
consync sync --force
```

### With Custom Config

```bash
consync sync --config /path/to/config.yaml
```

## Output

The sync command outputs:

1. Summary of planned actions
2. Progress for each page
3. Final summary with statistics

Example output:

```
Sync Plan Summary
=================
Space: DOCS
Actions:
  Create: 3
  Update: 2
  Delete: 0
  Skip: 10

Executing sync...
[CREATE] Getting Started (getting-started/index.md)
[CREATE] Quick Start (getting-started/quickstart.md)
[UPDATE] API Reference (api/index.md)

Sync completed: 3 created, 2 updated (1523ms)
```
