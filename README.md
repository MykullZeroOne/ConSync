# ConSync

ConSync is a Kotlin command-line application that synchronizes local Markdown documentation with Confluence. It reads markdown files from a local directory, preserves their hierarchy and links, and pushes them to Confluence via the Confluence REST API.

## Features

- **Markdown to Confluence Sync**: Converts local Markdown files to Confluence storage format and uploads them
- **Automatic Page Management**: Creates new pages if they don't exist, updates existing pages if they do
- **Hierarchy Preservation**: Translates local directory structure and markdown link relationships into Confluence page hierarchy
- **Configuration-Driven**: Uses a config file in your documentation directory to define space settings, table of contents, and sync behavior
- **Confluence REST API Integration**: All operations performed via the official Confluence API

## Installation

> **See the [complete installation guide](INSTALLATION.md) for detailed instructions, troubleshooting, and all installation options.**

### Prerequisites

- **Java 21 or higher** is required to run ConSync

### Quick Install

#### Windows (Scoop)

```powershell
scoop bucket add consync https://github.com/MykullZeroOne/scoop-consync
scoop install consync
```

#### Direct Download (All Platforms)

Download from [GitHub Releases](https://github.com/MykullZeroOne/ConSync/releases/latest):
- **Windows**: `consync-0.1.0-windows.zip` (includes installer)
- **macOS/Linux**: `consync-0.1.0-unix.tar.gz` (includes installer)
- **Universal JAR**: `consync-0.1.0.jar` (run with `java -jar`)

All downloads include SHA256 checksums for verification.

#### Build from Source

```bash
# Clone the repository
git clone https://github.com/MykullZeroOne/consync.git
cd consync

# Build with Maven
mvn clean package

# The fat JAR will be at target/consync.jar
java -jar target/consync.jar --help
```

### Authentication Setup

ConSync requires authentication credentials:

**Confluence Cloud:**
```bash
export CONFLUENCE_USERNAME="your-email@example.com"
export CONFLUENCE_API_TOKEN="your-api-token"
```

**Confluence Data Center/Server:**
```bash
export CONFLUENCE_PAT="your-personal-access-token"
```

See [INSTALLATION.md](INSTALLATION.md#authentication-setup) for detailed authentication setup instructions.

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
  preserve_labels: true           # Keep Confluence page labels on update
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

See `src/main/resources/consync-example.yaml` for a complete configuration example with all options.

## Usage

### Basic Sync

```bash
# Sync markdown files from current directory
java -jar consync.jar sync .

# Sync from a specific directory
java -jar consync.jar sync /path/to/docs

# Dry run - preview changes without pushing
java -jar consync.jar sync --dry-run /path/to/docs
```

### Commands

```bash
# Sync local markdown to Confluence
consync sync <directory>

# Validate configuration file
consync validate <directory>

# Show current sync status
consync status <directory>

# Show differences between local and Confluence
consync diff <directory>
```

### Global Options

| Option | Description |
|--------|-------------|
| `-v, --verbose` | Enable verbose output |
| `-d, --debug` | Enable debug output |
| `--version` | Show version information |
| `--help` | Show help message |

### Sync Options

| Option | Description |
|--------|-------------|
| `-n, --dry-run` | Preview changes without pushing to Confluence |
| `-f, --force` | Force update all pages regardless of change detection |
| `-c, --config <file>` | Specify alternate config file location |
| `-s, --space <key>` | Override space key from config |

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
java -jar consync.jar sync /path/to/docs
```

### Personal Access Token (Data Center/Server)

```bash
export CONFLUENCE_PAT="your-personal-access-token"
java -jar consync.jar sync /path/to/docs
```

## Development

### Build

```bash
mvn clean package
```

### Run Tests

```bash
mvn test
```

### Run from Source

```bash
mvn exec:java -Dexec.args="sync /path/to/docs"
```

### Project Structure

```
src/
├── main/
│   ├── kotlin/com/consync/
│   │   ├── Main.kt              # Application entry point
│   │   ├── cli/                 # CLI commands (Clikt)
│   │   ├── config/              # Configuration handling
│   │   ├── core/                # Core business logic
│   │   ├── service/             # Business services
│   │   ├── client/              # External integrations
│   │   ├── model/               # Domain models
│   │   └── util/                # Utilities
│   └── resources/
│       ├── logback.xml          # Logging configuration
│       └── consync-example.yaml # Example config
└── test/
    └── kotlin/com/consync/      # Unit tests
```

## License

MIT License - see [LICENSE](LICENSE) for details.
