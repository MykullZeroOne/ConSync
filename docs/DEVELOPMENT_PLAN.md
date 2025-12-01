# ConSync Development Plan

## Project Overview

ConSync is a Kotlin command-line application that synchronizes local Markdown documentation with Atlassian Confluence. The application reads markdown files from a local directory, converts them to Confluence storage format, and manages page creation/updates via the Confluence REST API.

## Technology Stack

| Component | Technology | Version | Purpose |
|-----------|------------|---------|---------|
| Language | Kotlin | 1.9.x | Primary development language |
| Build Tool | Maven | 3.9.x | Dependency management and build |
| CLI Framework | Clikt | 4.x | Command-line argument parsing |
| HTTP Client | OkHttp | 4.x | REST API communication |
| JSON Processing | Kotlinx Serialization | 1.6.x | JSON parsing/serialization |
| YAML Processing | SnakeYAML / kaml | 0.55.x | Configuration file parsing |
| Markdown Parser | CommonMark | 0.21.x | Markdown to AST parsing |
| Logging | SLF4J + Logback | 1.4.x | Application logging |
| Testing | JUnit 5 + MockK | 5.10.x / 1.13.x | Unit and integration testing |

## Architecture

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        CLI Layer (Clikt)                        │
│  ┌─────────┐ ┌──────────┐ ┌──────────┐ ┌────────┐ ┌──────────┐ │
│  │  sync   │ │ validate │ │  status  │ │  diff  │ │  config  │ │
│  └────┬────┘ └────┬─────┘ └────┬─────┘ └───┬────┘ └────┬─────┘ │
└───────┼───────────┼────────────┼───────────┼───────────┼───────┘
        │           │            │           │           │
        ▼           ▼            ▼           ▼           ▼
┌─────────────────────────────────────────────────────────────────┐
│                      Service Layer                              │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐ │
│  │   SyncService   │  │  ConfigService  │  │  StatusService  │ │
│  └────────┬────────┘  └────────┬────────┘  └────────┬────────┘ │
└───────────┼────────────────────┼────────────────────┼──────────┘
            │                    │                    │
            ▼                    ▼                    ▼
┌─────────────────────────────────────────────────────────────────┐
│                       Core Layer                                │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────────┐  │
│  │ MarkdownProc │  │ HierarchyMgr │  │ ConfluenceConverter  │  │
│  └──────────────┘  └──────────────┘  └──────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
            │
            ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Infrastructure Layer                         │
│  ┌─────────────────────┐  ┌─────────────────────────────────┐  │
│  │  ConfluenceClient   │  │       FileSystemReader          │  │
│  │  (REST API Client)  │  │   (Local File Operations)       │  │
│  └─────────────────────┘  └─────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

### Package Structure

```
com.consync/
├── Main.kt                          # Application entry point
├── cli/                             # CLI commands
│   ├── ConSyncCommand.kt            # Root command
│   ├── SyncCommand.kt               # Sync subcommand
│   ├── ValidateCommand.kt           # Config validation
│   ├── StatusCommand.kt             # Show sync status
│   └── DiffCommand.kt               # Show differences
├── config/                          # Configuration
│   ├── Config.kt                    # Config data classes
│   ├── ConfigLoader.kt              # YAML config loading
│   └── ConfigValidator.kt           # Config validation
├── core/                            # Core business logic
│   ├── markdown/                    # Markdown processing
│   │   ├── MarkdownParser.kt        # Parse MD to AST
│   │   ├── MarkdownDocument.kt      # Document model
│   │   └── LinkExtractor.kt         # Extract/resolve links
│   ├── hierarchy/                   # Hierarchy management
│   │   ├── HierarchyBuilder.kt      # Build page tree
│   │   ├── PageNode.kt              # Tree node model
│   │   └── HierarchyResolver.kt     # Resolve parent-child
│   └── converter/                   # Format conversion
│       ├── ConfluenceConverter.kt   # MD → Confluence storage
│       ├── StorageFormatBuilder.kt  # Build XHTML storage
│       └── MacroRenderer.kt         # Confluence macros
├── service/                         # Business services
│   ├── SyncService.kt               # Orchestrate sync
│   ├── PageService.kt               # Page CRUD operations
│   ├── DiffService.kt               # Compute differences
│   └── ValidationService.kt         # Validate configs/content
├── client/                          # External integrations
│   ├── confluence/                  # Confluence API
│   │   ├── ConfluenceClient.kt      # API client interface
│   │   ├── ConfluenceClientImpl.kt  # HTTP implementation
│   │   ├── model/                   # API models
│   │   │   ├── Page.kt
│   │   │   ├── Space.kt
│   │   │   ├── Content.kt
│   │   │   └── SearchResult.kt
│   │   └── exception/               # API exceptions
│   │       └── ConfluenceException.kt
│   └── filesystem/                  # File system
│       ├── FileReader.kt            # Read local files
│       └── FileWatcher.kt           # Watch for changes
├── model/                           # Domain models
│   ├── SyncPlan.kt                  # Planned sync actions
│   ├── SyncResult.kt                # Sync operation result
│   ├── PageMetadata.kt              # Page metadata
│   └── SyncState.kt                 # Current sync state
└── util/                            # Utilities
    ├── Logging.kt                   # Logging helpers
    ├── PathUtils.kt                 # Path manipulation
    └── HashUtils.kt                 # Content hashing
```

## Implementation Phases

### Phase 1: Project Setup and Core Infrastructure

**Objective**: Establish project structure, build configuration, and basic infrastructure.

**Tasks**:
1. Initialize Maven project with Kotlin configuration
2. Configure dependencies (Clikt, OkHttp, kotlinx-serialization, etc.)
3. Set up logging infrastructure (SLF4J + Logback)
4. Create basic CLI structure with Clikt
5. Implement configuration loading (YAML parsing)
6. Create domain models for configuration

**Deliverables**:
- Working Maven build
- CLI skeleton with help output
- Configuration file loading
- Unit tests for config parsing

### Phase 2: Confluence API Client

**Objective**: Implement full Confluence REST API integration.

**Tasks**:
1. Design Confluence client interface
2. Implement authentication (API token, PAT)
3. Implement core API operations:
   - Get space information
   - Search for pages (CQL)
   - Get page by ID/title
   - Create new page
   - Update existing page
   - Get page children
   - Get page ancestors
4. Implement error handling and retries
5. Add rate limiting support

**Deliverables**:
- Fully functional Confluence API client
- Support for Cloud and Data Center
- Comprehensive error handling
- Integration tests (with mocks)

**Key API Endpoints**:
```
GET  /wiki/rest/api/space/{spaceKey}
GET  /wiki/rest/api/content?spaceKey={key}&title={title}
GET  /wiki/rest/api/content/{id}
POST /wiki/rest/api/content
PUT  /wiki/rest/api/content/{id}
GET  /wiki/rest/api/content/{id}/child/page
GET  /wiki/rest/api/content/search?cql={query}
```

### Phase 3: Markdown Processing

**Objective**: Parse markdown files and extract structure/links.

**Tasks**:
1. Implement markdown file discovery (glob patterns)
2. Parse markdown to AST using CommonMark
3. Extract frontmatter metadata (if present)
4. Extract and catalog internal links
5. Build document model with metadata
6. Handle special markdown elements:
   - Code blocks (with language hints)
   - Tables
   - Images (local and remote)
   - Task lists

**Deliverables**:
- Markdown parser with full CommonMark support
- Link extraction and resolution
- Document model with metadata
- Unit tests for various markdown formats

### Phase 4: Hierarchy Management

**Objective**: Build and manage page hierarchy from local file structure.

**Tasks**:
1. Scan directory structure
2. Build hierarchical tree from directories/files
3. Resolve `index.md` files as directory pages
4. Map local links to hierarchy relationships
5. Detect hierarchy changes from previous sync
6. Generate page titles from filenames/frontmatter

**Deliverables**:
- Hierarchy builder from file system
- Parent-child relationship resolver
- Hierarchy diff detection
- Unit tests for hierarchy scenarios

### Phase 5: Confluence Storage Format Conversion

**Objective**: Convert markdown AST to Confluence storage format (XHTML).

**Tasks**:
1. Implement AST visitor for Confluence conversion
2. Map markdown elements to Confluence equivalents:
   - Headings → `<h1>` - `<h6>`
   - Paragraphs → `<p>`
   - Code blocks → `<ac:structured-macro ac:name="code">`
   - Tables → `<table>` with Confluence classes
   - Links → `<ac:link>` or `<a>`
   - Images → `<ac:image>` or `<img>`
   - Lists → `<ul>`, `<ol>`, `<li>`
   - Task lists → `<ac:task-list>`
3. Generate Table of Contents macro
4. Handle internal page links
5. Handle image attachments

**Deliverables**:
- Full markdown to Confluence converter
- Support for all common markdown elements
- TOC generation
- Unit tests with sample conversions

**Confluence Storage Format Examples**:
```xml
<!-- Code block -->
<ac:structured-macro ac:name="code">
  <ac:parameter ac:name="language">kotlin</ac:parameter>
  <ac:plain-text-body><![CDATA[fun main() { println("Hello") }]]></ac:plain-text-body>
</ac:structured-macro>

<!-- Internal link -->
<ac:link>
  <ri:page ri:content-title="Page Title" ri:space-key="SPACE"/>
  <ac:plain-text-link-body><![CDATA[Link Text]]></ac:plain-text-link-body>
</ac:link>

<!-- TOC macro -->
<ac:structured-macro ac:name="toc">
  <ac:parameter ac:name="maxLevel">3</ac:parameter>
</ac:structured-macro>
```

### Phase 6: Sync Engine

**Objective**: Implement the core synchronization logic.

**Tasks**:
1. Implement sync plan generation:
   - Detect new pages (create)
   - Detect modified pages (update)
   - Detect deleted pages (optionally remove)
   - Detect moved pages (update parent)
2. Implement content hashing for change detection
3. Store sync state locally (`.consync/state.json`)
4. Implement dry-run mode
5. Implement force sync mode
6. Handle conflicts and errors gracefully

**Deliverables**:
- Sync plan generator
- State persistence
- Dry-run support
- Conflict detection
- Comprehensive sync tests

**Sync State File Format**:
```json
{
  "version": 1,
  "lastSync": "2024-01-15T10:30:00Z",
  "pages": {
    "getting-started/index.md": {
      "confluenceId": "123456",
      "contentHash": "sha256:abc123...",
      "lastModified": "2024-01-15T10:30:00Z",
      "version": 5
    }
  }
}
```

### Phase 7: CLI Commands Implementation

**Objective**: Implement all CLI commands with full functionality.

**Tasks**:
1. **sync command**: Full sync with all options
2. **validate command**: Config and content validation
3. **status command**: Show current sync state
4. **diff command**: Show what would change
5. Implement progress indicators
6. Implement verbose output mode
7. Add color output support

**Deliverables**:
- All CLI commands fully functional
- Progress indicators
- Colored output
- Help documentation

### Phase 8: Testing and Documentation

**Objective**: Comprehensive testing and user documentation.

**Tasks**:
1. Unit tests for all components (>80% coverage)
2. Integration tests with Confluence API mocks
3. End-to-end tests with test Confluence instance
4. Performance testing with large doc sets
5. Write user documentation
6. Create example configurations
7. Document troubleshooting guides

**Deliverables**:
- Comprehensive test suite
- User documentation
- Example configurations
- Troubleshooting guide

### Phase 9: Packaging and Distribution

**Objective**: Create distributable packages.

**Tasks**:
1. Configure Maven assembly plugin for fat JAR
2. Create native binaries with GraalVM (optional)
3. Create distribution archives (zip, tar.gz)
4. Set up GitHub releases
5. Create Homebrew formula (optional)
6. Docker image (optional)

**Deliverables**:
- Fat JAR distribution
- Native binaries (optional)
- Distribution archives
- Installation documentation

## Key Components Detail

### Configuration Schema

```yaml
# consync.yaml - Full schema
confluence:
  base_url: string          # Required: Confluence base URL
  username: string          # Required for Cloud: Email address
  api_token: string         # Required for Cloud: API token (supports env vars)
  pat: string               # Required for DC/Server: Personal access token
  timeout: integer          # Optional: Request timeout in seconds (default: 30)
  retry_count: integer      # Optional: Retry attempts (default: 3)

space:
  key: string               # Required: Space key
  root_page_id: string      # Optional: Root page ID (alternative to title)
  root_page_title: string   # Optional: Root page title

content:
  title_source: enum        # Optional: filename, frontmatter, first_heading (default: filename)
  toc:
    enabled: boolean        # Optional: Enable TOC (default: true)
    depth: integer          # Optional: TOC depth (default: 3)
    position: enum          # Optional: top, bottom (default: top)
  frontmatter:
    strip: boolean          # Optional: Remove frontmatter from output (default: true)
    use_title: boolean      # Optional: Use frontmatter title (default: true)

sync:
  delete_orphans: boolean   # Optional: Delete pages not in source (default: false)
  update_unchanged: boolean # Optional: Update even if unchanged (default: false)
  preserve_labels: boolean  # Optional: Keep existing labels (default: true)
  state_file: string        # Optional: State file location (default: .consync/state.json)

files:
  include:                  # Optional: Include patterns (default: ["**/*.md"])
    - string
  exclude:                  # Optional: Exclude patterns
    - string
  index_file: string        # Optional: Directory index filename (default: index.md)

logging:
  level: enum               # Optional: debug, info, warn, error (default: info)
  file: string              # Optional: Log file path
```

### Confluence API Client Interface

```kotlin
interface ConfluenceClient {
    // Space operations
    suspend fun getSpace(spaceKey: String): Space

    // Page operations
    suspend fun getPage(pageId: String, expand: List<String> = emptyList()): Page
    suspend fun getPageByTitle(spaceKey: String, title: String): Page?
    suspend fun createPage(page: CreatePageRequest): Page
    suspend fun updatePage(pageId: String, page: UpdatePageRequest): Page
    suspend fun deletePage(pageId: String)

    // Hierarchy operations
    suspend fun getChildPages(pageId: String): List<Page>
    suspend fun getAncestors(pageId: String): List<Page>
    suspend fun movePage(pageId: String, newParentId: String)

    // Search
    suspend fun search(cql: String, limit: Int = 25): SearchResult

    // Attachments
    suspend fun uploadAttachment(pageId: String, file: File, comment: String?): Attachment
    suspend fun getAttachments(pageId: String): List<Attachment>
}
```

### Sync Service Flow

```
┌─────────────┐
│ Load Config │
└──────┬──────┘
       ▼
┌─────────────────┐
│ Scan Local Files│
└──────┬──────────┘
       ▼
┌─────────────────┐
│ Build Hierarchy │
└──────┬──────────┘
       ▼
┌─────────────────┐
│ Load Sync State │
└──────┬──────────┘
       ▼
┌─────────────────────┐
│ Fetch Confluence    │
│ Current State       │
└──────┬──────────────┘
       ▼
┌─────────────────┐
│ Generate Diff   │
└──────┬──────────┘
       ▼
┌─────────────────┐     ┌─────────────────┐
│ Create Sync Plan│────►│ Display Plan    │ (dry-run)
└──────┬──────────┘     └─────────────────┘
       ▼
┌─────────────────┐
│ Execute Plan    │
│ (Create/Update) │
└──────┬──────────┘
       ▼
┌─────────────────┐
│ Save Sync State │
└──────┬──────────┘
       ▼
┌─────────────────┐
│ Report Results  │
└─────────────────┘
```

## Error Handling Strategy

| Error Type | Handling Strategy |
|------------|-------------------|
| Network timeout | Retry with exponential backoff (3 attempts) |
| 401 Unauthorized | Fail fast with clear auth error message |
| 403 Forbidden | Report permission issue, skip page, continue |
| 404 Not Found | Handle gracefully (page may have been deleted) |
| 409 Conflict | Report version conflict, offer resolution options |
| 429 Rate Limited | Back off and retry with delay |
| Parse errors | Report file and location, skip file, continue |
| Config errors | Fail fast with validation message |

## Testing Strategy

### Unit Tests
- Config parsing and validation
- Markdown parsing and conversion
- Hierarchy building
- Sync plan generation
- Content hashing

### Integration Tests
- Confluence API client (with WireMock)
- Full sync workflow (with mock API)
- File system operations

### End-to-End Tests
- Full sync against test Confluence instance
- Various directory structures
- Edge cases (special characters, large files)

## Performance Considerations

1. **Parallel Processing**: Process multiple files concurrently
2. **Incremental Sync**: Only process changed files
3. **Content Hashing**: Quick change detection without API calls
4. **Connection Pooling**: Reuse HTTP connections
5. **Batch Operations**: Group API calls where possible
6. **Lazy Loading**: Load file contents only when needed

## Security Considerations

1. **Credential Storage**: Support environment variables, never store in config
2. **Token Handling**: Mask tokens in logs
3. **HTTPS Only**: Enforce HTTPS for API communication
4. **Input Validation**: Sanitize all user inputs
5. **Dependency Security**: Regular dependency updates

## Future Enhancements (Out of Scope for v1)

- Watch mode for automatic sync on file changes
- Bidirectional sync (pull from Confluence)
- Attachment optimization (skip unchanged)
- Multiple space support
- Template support
- Plugin system for custom converters
- GUI companion app
