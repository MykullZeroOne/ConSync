# ConSync Documentation

Welcome to ConSync, a powerful command-line tool for synchronizing local Markdown documentation with Atlassian Confluence.

## What is ConSync?

ConSync bridges the gap between your local documentation workflow and Confluence's collaborative platform. Write your documentation in Markdown using your favorite editor, organize it in a directory structure that makes sense, and let ConSync handle the synchronization to Confluence.

## Key Features

- **Markdown-First Workflow**: Write documentation in plain Markdown files
- **Hierarchy Preservation**: Your directory structure becomes Confluence page hierarchy
- **Smart Link Conversion**: Markdown links automatically convert to Confluence page links
- **Bidirectional Sync**: Keep local and remote content in sync
- **Conflict Resolution**: Built-in strategies for handling conflicts
- **Configuration-Driven**: Flexible YAML configuration for all settings

## Quick Example

```bash
# Your local documentation structure
docs/
├── consync.yaml
├── index.md
├── getting-started/
│   ├── installation.md
│   └── quickstart.md
└── guides/
    └── configuration.md

# Sync to Confluence
java -jar consync.jar sync docs/

# Result: Hierarchical pages in Confluence
# Documentation Home (index.md)
# ├── Getting Started (folder)
# │   ├── Installation
# │   └── Quickstart
# └── Guides (folder)
#     └── Configuration
```

## Getting Started

New to ConSync? Start here:

1. [Installation](getting-started/installation.md) - Install and set up ConSync
2. [Quickstart](getting-started/quickstart.md) - Sync your first documentation
3. [Configuration](guides/configuration.md) - Configure ConSync for your needs

## Documentation Sections

### [Getting Started](getting-started/index.md)
Everything you need to get up and running with ConSync.

### [Guides](guides/index.md)
Detailed guides on using ConSync features and best practices.

### [Reference](reference/index.md)
Complete reference documentation for configuration options and command-line interface.

## Use Cases

ConSync is perfect for:

- **Engineering Teams**: Maintain documentation in Git alongside code
- **Technical Writers**: Use local tools while publishing to Confluence
- **Open Source Projects**: Keep public docs in sync with internal Confluence
- **Documentation as Code**: Apply version control, CI/CD, and review processes to documentation

## Prerequisites

- Java 17 or higher
- Access to a Confluence instance (Cloud or Data Center)
- Confluence API token or Personal Access Token

## Need Help?

- Check the [Guides](guides/index.md) section for detailed how-tos
- See [Configuration Reference](reference/configuration-options.md) for all options
- Review [Advanced Topics](guides/advanced/index.md) for complex scenarios

## About This Documentation

This documentation itself serves as an example of how to structure documentation for ConSync. The directory structure, linking patterns, and configuration file demonstrate best practices for organizing your own documentation.
