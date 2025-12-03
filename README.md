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
- Maven 3.9.x or higher (for building from source)

### Option 1: Automated Installer (Recommended)

The easiest way to install ConSync with a single command:

```bash
# Clone the repository
git clone https://github.com/MykullZeroOne/consync.git
cd consync

# Run the installer (builds, installs, and configures credentials)
./install.sh
```

The installer will:
- ✓ Build ConSync from source
- ✓ Install to `/usr/local/bin` (or `~/.consync-app`)
- ✓ Set up authentication (API token or PAT)
- ✓ Create a `consync` command you can run from anywhere
- ✓ Secure your credentials with proper permissions

After installation, simply run:

```bash
consync sync --dry-run /path/to/docs
```

**To uninstall:**

```bash
./uninstall.sh
```

### Option 2: Quick Setup (Alias Only)

If you prefer a minimal setup with just an alias:

```bash
cd consync
./scripts/quick-setup.sh
```

This adds an alias to your shell profile without copying files.

### Option 3: Manual Build and Run

Build from source and run directly:

```bash
# Clone the repository
git clone https://github.com/MykullZeroOne/consync.git
cd consync

# Build the application
mvn clean package

# The fat JAR will be at target/consync.jar
```

**Run the application:**

```bash
# Using the fat JAR
java -jar target/consync.jar --help

# Or run directly with Maven
mvn exec:java -Dexec.args="--help"

# Create an alias for convenience
alias consync='java -jar /path/to/consync/target/consync.jar'
```

**Set credentials manually:**

```bash
# For Confluence Cloud
export CONFLUENCE_USERNAME="your-email@example.com"
export CONFLUENCE_API_TOKEN="your-api-token"

# For Confluence Data Center/Server
export CONFLUENCE_PAT="your-personal-access-token"
```

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
