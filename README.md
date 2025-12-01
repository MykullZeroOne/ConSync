# ConSync

ConSync is a Kotlin command-line application that synchronizes local Markdown documentation with Confluence. It reads markdown files from a local directory, preserves their hierarchy and links, and pushes them to Confluence via the Confluence REST API.

## Features

- **Markdown to Confluence Sync**: Converts local Markdown files to Confluence storage format and uploads them
- **Automatic Page Management**: Creates new pages if they don't exist, updates existing pages if they do
- **Hierarchy Preservation**: Translates local directory structure and markdown link relationships into Confluence page hierarchy
- **Configuration-Driven**: Uses a config file in your documentation directory to define space settings, table of contents, and sync behavior
- **Confluence REST API Integration**: All operations performed via the official Confluence API

## Installation

### Prerequisites

- JDK 17 or higher
- Gradle 8.x (or use the included Gradle wrapper)

### Build from Source

```bash
# Clone the repository
git clone https://github.com/yourusername/consync.git
cd consync

# Build the application
./gradlew build

# Create distributable
./gradlew installDist
```

The executable will be available at `build/install/consync/bin/consync`.

## Configuration

Create a `consync.yaml` configuration file in your markdown documentation directory:

```yaml
# Confluence connection settings
confluence:
  base_url: "https://your-domain.atlassian.net/wiki"
  username: "your-email@example.com"
  api_token: "${CONFLUENCE_API_TOKEN}"  # Use environment variable

# Space configuration
space:
  key: "DOCS"                    # Confluence space key
  name: "Documentation"          # Space display name

# Content settings
content:
  root_page: "Documentation Home" # Parent page for all synced content
  toc:
    enabled: true                 # Generate table of contents
    depth: 3                      # TOC heading depth
    position: "top"               # TOC position: top, bottom, or none

# Sync behavior
sync:
  delete_orphans: false           # Remove Confluence pages not in local files
  preserve_metadata: true         # Keep Confluence page metadata on update
  conflict_resolution: "local"    # local, remote, or ask

# File handling
files:
  include:
    - "**/*.md"
  exclude:
    - "**/node_modules/**"
    - "**/_drafts/**"
    - "**/README.md"              # Optionally exclude README
```

## Usage

### Basic Sync

```bash
# Sync markdown files from current directory
consync sync .

# Sync from a specific directory
consync sync /path/to/docs

# Dry run - preview changes without pushing
consync sync --dry-run /path/to/docs
```

### Commands

```bash
# Sync local markdown to Confluence
consync sync <directory>

# Validate configuration file
consync validate <directory>

# List pages that would be affected
consync status <directory>

# Pull current state from Confluence (compare only)
consync diff <directory>
```

### Options

| Option | Description |
|--------|-------------|
| `--dry-run` | Preview changes without pushing to Confluence |
| `--force` | Force update all pages regardless of change detection |
| `--verbose` | Enable detailed logging output |
| `--config <file>` | Specify alternate config file location |
| `--space <key>` | Override space key from config |

## Markdown Hierarchy

ConSync translates your local file structure into Confluence page hierarchy:

```
docs/
├── consync.yaml          # Configuration file
├── index.md              # Root page
├── getting-started/
│   ├── index.md          # "Getting Started" parent page
│   ├── installation.md   # Child page
│   └── quickstart.md     # Child page
└── guides/
    ├── index.md          # "Guides" parent page
    ├── basics.md
    └── advanced/
        ├── index.md      # "Advanced" parent page
        └── tips.md
```

**Hierarchy Rules:**
- Directories become parent pages (using `index.md` content if present)
- Files within directories become child pages
- Markdown links between files (`[link](../other.md)`) are converted to Confluence page links
- Relative paths are preserved as Confluence parent-child relationships

## Authentication

ConSync supports multiple authentication methods:

### API Token (Recommended for Cloud)

```bash
export CONFLUENCE_API_TOKEN="your-api-token"
consync sync /path/to/docs
```

### Personal Access Token (Data Center/Server)

```bash
export CONFLUENCE_PAT="your-personal-access-token"
consync sync /path/to/docs
```

## Development

### Build

```bash
./gradlew build
```

### Run Tests

```bash
./gradlew test
```

### Run from Source

```bash
./gradlew run --args="sync /path/to/docs"
```

## License

MIT License - see [LICENSE](LICENSE) for details.
