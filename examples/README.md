# ConSync Example Documentation

This directory contains comprehensive example documentation that demonstrates how to structure and organize documentation for use with ConSync.

## Purpose

This example serves two key purposes:

1. **Documentation for ConSync**: Complete guide on how to use ConSync, including installation, configuration, and usage
2. **Example Structure**: Demonstrates best practices for organizing documentation that will be synced to Confluence

## What's Included

### Documentation Content

- **[index.md](index.md)**: Main documentation homepage with overview and navigation
- **[getting-started/](getting-started/)**: Installation and quickstart guides for new users
- **[guides/](guides/)**: Detailed guides covering configuration, usage, and advanced topics
- **[reference/](reference/)**: Complete reference documentation for all configuration options

### Configuration

- **[consync.yaml](consync.yaml)**: Example configuration file with all available options documented

## Directory Structure

```
examples/
├── README.md                          # This file
├── consync.yaml                       # Example configuration
├── index.md                           # Documentation home
├── getting-started/
│   ├── index.md                       # Getting started overview
│   ├── installation.md                # Installation guide
│   └── quickstart.md                  # 5-minute quickstart
├── guides/
│   ├── index.md                       # Guides overview
│   ├── configuration.md               # Configuration guide
│   ├── usage.md                       # Usage guide
│   └── advanced/
│       ├── index.md                   # Advanced topics overview
│       ├── hierarchy.md               # Hierarchy management
│       └── authentication.md          # Authentication details
└── reference/
    ├── index.md                       # Reference overview
    └── configuration-options.md       # Complete config reference
```

## Using This Example

### As Documentation

Read through the markdown files to learn how to use ConSync:

1. Start with [index.md](index.md) for an overview
2. Follow [getting-started/installation.md](getting-started/installation.md) to install
3. Try the [getting-started/quickstart.md](getting-started/quickstart.md)
4. Explore [guides/](guides/) for detailed information

### As a Template

Use this structure as a template for your own documentation:

1. **Copy the structure**: Use the same directory layout for your docs
2. **Adapt the content**: Replace with your own documentation
3. **Keep the config**: Modify [consync.yaml](consync.yaml) for your needs
4. **Maintain links**: Use relative links like `[link](../other.md)`

### To Test ConSync

Sync this example to Confluence to see ConSync in action:

```bash
# From the ConSync project root
cd examples

# Update consync.yaml with your Confluence details
vim consync.yaml

# Test with dry run
java -jar ../target/consync.jar sync --dry-run .

# Sync to Confluence
java -jar ../target/consync.jar sync .
```

## Key Features Demonstrated

### Hierarchy

- **Multi-level nesting**: Demonstrates up to 3 levels of hierarchy
- **Index files**: Each section has an `index.md` for overview content
- **Logical organization**: Grouped by topic (getting-started, guides, reference)

### Linking

- **Relative links**: All links use relative paths (e.g., `[link](../guides/usage.md)`)
- **Cross-section links**: Links between different documentation sections
- **Index references**: Links to section index pages

### Content Organization

- **Progressive disclosure**: Start simple (getting-started), increase complexity (guides, advanced)
- **Clear navigation**: Each page links to related pages
- **Table of contents**: Enabled in configuration for easy navigation

### Configuration

- **Comprehensive**: Shows all available configuration options
- **Well-commented**: Every option explained with comments
- **Environment variables**: Demonstrates secure credential handling

## Documentation Hierarchy

When synced to Confluence, this structure creates:

```
ConSync Documentation (root)
├── Getting Started
│   ├── Installation
│   └── Quickstart
├── Guides
│   ├── Configuration
│   ├── Usage
│   └── Advanced
│       ├── Hierarchy Management
│       └── Authentication
└── Reference
    └── Configuration Options
```

## Best Practices Demonstrated

1. **Clear Structure**: Logical organization by topic and complexity
2. **Index Files**: Overview content for each section
3. **Relative Links**: Portable links that work locally and in Confluence
4. **Descriptive Names**: Filenames clearly indicate content
5. **Consistent Depth**: Maximum 3-4 levels of nesting
6. **Navigation**: Each page links to related content
7. **Configuration**: Complete, documented config file
8. **Security**: Environment variables for credentials

## Customizing for Your Docs

To adapt this structure for your own documentation:

1. **Replace content**: Keep the structure, change the content
2. **Adjust sections**: Add/remove sections based on your needs
3. **Update config**: Modify `consync.yaml` for your Confluence instance
4. **Maintain patterns**: Keep the linking and organization patterns
5. **Add assets**: Include images, diagrams as needed

## Tips for Using This Example

- **Read the guides**: They contain valuable information about ConSync features
- **Follow the structure**: It's designed to work well with ConSync
- **Test locally**: Preview in a markdown viewer before syncing
- **Use dry-run**: Always test with `--dry-run` first
- **Version control**: Keep your docs in Git for change tracking

## Further Reading

- [ConSync README](../README.md): Project overview and development info
- [Configuration Reference](reference/configuration-options.md): All config options
- [Usage Guide](guides/usage.md): Commands and workflows
- [Advanced Topics](guides/advanced/index.md): Complex scenarios

## Questions?

If you have questions about ConSync or this example:

1. Read through the [guides/](guides/) section
2. Check the [reference/](reference/) documentation
3. Review the main [README](../README.md)
4. Open an issue on GitHub
