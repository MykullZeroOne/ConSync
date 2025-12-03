# Getting Started with ConSync

This section will help you get ConSync installed and running quickly.

## Overview

ConSync is a command-line tool that synchronizes Markdown files to Confluence. The basic workflow is:

1. Write documentation in Markdown files
2. Organize files in a directory structure
3. Create a `consync.yaml` configuration file
4. Run `consync sync` to push to Confluence

## What You'll Learn

This section covers:

- [Installation](installation.md) - Installing ConSync and its prerequisites
- [Quickstart](quickstart.md) - Your first sync in 5 minutes

## Prerequisites

Before you begin, ensure you have:

- **Java 21 or higher** - ConSync requires Java 21+
- **Confluence Access** - Either Cloud or Data Center/Server
- **API Credentials** - API token (Cloud) or Personal Access Token (Server)

No need to build from source - automated installers are available for Windows, macOS, and Linux!

## Basic Concepts

### Markdown Files

ConSync works with standard Markdown files (`.md`). You can use any Markdown editor or IDE.

### Directory Structure

Your directory structure translates to Confluence hierarchy:

```
docs/
├── index.md          → Documentation Home (root page)
├── section1/         → Section 1 (parent page)
│   ├── page1.md     → Page 1 (child of Section 1)
│   └── page2.md     → Page 2 (child of Section 1)
└── section2/         → Section 2 (parent page)
    └── page3.md     → Page 3 (child of Section 2)
```

### Configuration File

The `consync.yaml` file in your documentation directory controls all behavior:

- Confluence connection details
- Space and page settings
- Sync behavior
- File inclusion/exclusion

### Authentication

ConSync supports two authentication methods:

- **API Token** - For Confluence Cloud
- **Personal Access Token (PAT)** - For Data Center/Server

See [Installation](installation.md) for details on obtaining credentials.

## Next Steps

Ready to install ConSync? Head to the [Installation](installation.md) guide.

Already installed? Jump to the [Quickstart](quickstart.md) to sync your first documentation.
