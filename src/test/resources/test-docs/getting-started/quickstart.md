---
title: Quick Start
description: Quick start guide for ConSync
weight: 2
---

# Quick Start Guide

Get ConSync running in 5 minutes!

## Step 1: Create Configuration

Create a `consync.yaml` file in your documentation directory:

```yaml
confluence:
  base_url: https://your-domain.atlassian.net/wiki
  username: ${CONFLUENCE_USERNAME}
  api_token: ${CONFLUENCE_API_TOKEN}

space:
  key: DOCS
```

## Step 2: Run Validation

Validate your configuration:

```bash
java -jar consync.jar validate
```

## Step 3: Preview Changes

Run a dry-run to see what would be synced:

```bash
java -jar consync.jar sync --dry-run
```

## Step 4: Sync!

Sync your documentation:

```bash
java -jar consync.jar sync
```

## Task List Example

- [x] Install ConSync
- [x] Create configuration
- [ ] Connect to Confluence
- [ ] Sync first page
