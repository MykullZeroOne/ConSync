# Changelog

All notable changes to ConSync will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Project documentation and README with feature overview
- Development plan document (`docs/DEVELOPMENT_PLAN.md`)
- This changelog file
- **Phase 1: Project Setup (Complete)**
  - Maven project configuration (`pom.xml`) with Kotlin 1.9.22
  - CLI framework using Clikt 4.2.1
  - CLI commands: `sync`, `validate`, `status`, `diff`
  - Configuration data classes with kotlinx.serialization
  - YAML configuration loader using kaml
  - Configuration validator with comprehensive checks
  - Logback logging configuration
  - Example configuration file (`consync-example.yaml`)
  - Unit tests for ConfigLoader and ConfigValidator

- **Phase 2: Confluence API Client (Complete)**
  - Full Confluence REST API client interface (`ConfluenceClient`)
  - OkHttp-based implementation (`ConfluenceClientImpl`)
  - API model classes:
    - `Page`, `Space`, `Attachment` models
    - `CreatePageRequest`, `UpdatePageRequest` DTOs
    - `SearchResult`, `ContentResponse` for API responses
  - Authentication support:
    - Basic Auth with API token (Confluence Cloud)
    - Bearer token with PAT (Confluence Data Center/Server)
  - Comprehensive exception hierarchy:
    - `AuthenticationException` (401)
    - `ForbiddenException` (403)
    - `NotFoundException` (404)
    - `ConflictException` (409)
    - `RateLimitException` (429)
    - `ServerException` (5xx)
    - `NetworkException`, `MaxRetriesExceededException`
  - Retry logic with exponential backoff
  - Request/response logging interceptor
  - Factory class for easy client creation
  - Unit tests and WireMock integration tests

- **Phase 3: Markdown Processing (Complete)**
  - `MarkdownDocument` model with comprehensive metadata:
    - Relative/absolute paths, content, frontmatter
    - Title, links, images, headings
    - Content hash for change detection
  - `FileScanner` for markdown file discovery:
    - Glob pattern matching for include/exclude
    - Recursive directory scanning
    - Hidden directory filtering
  - `FrontmatterParser` for YAML frontmatter:
    - Standard fields: title, description, tags, author, date
    - Custom properties support
    - Frontmatter stripping
  - `MarkdownParser` with CommonMark:
    - GFM tables, task lists, autolinks extensions
    - Configurable title source (filename, frontmatter, first heading)
    - Heading extraction with anchor generation
    - Content hashing (SHA-256)
  - `LinkExtractor` for internal links:
    - Internal/external link detection
    - Relative path resolution
    - Image extraction
    - Link graph building
  - Comprehensive unit tests for all components

- **Phase 4: Hierarchy Management (Complete)**
  - `PageNode` tree structure model:
    - Parent/child relationships
    - Depth, ancestors, descendants traversal
    - Virtual nodes for directories without index.md
    - Sorting by weight and title
    - Path and ID-based lookup
  - `HierarchyBuilder` to construct page tree:
    - Directory structure to tree conversion
    - index.md files as directory pages
    - Virtual node creation for empty directories
    - Weight-based sorting
    - Confluence ID preservation from frontmatter
  - `HierarchyResolver` for relationships:
    - Internal link resolution to page nodes
    - Common ancestor finding
    - Relative path computation
    - Hierarchy validation
    - Broken link detection
    - Backlink building
    - Statistics calculation
  - Comprehensive unit tests for all hierarchy components

- **Phase 5: Storage Format Conversion (Complete)**
  - `StorageFormatBuilder` for XHTML generation:
    - XML escaping and CDATA handling
    - HTML element builders (headings, paragraphs, lists, tables)
    - Inline formatting (bold, italic, code, links, images)
    - Confluence table classes
  - `MacroRenderer` for Confluence macros:
    - Code block macro with syntax highlighting
    - Table of contents macro
    - Panel macros (info, note, warning, tip)
    - Internal page links (`ac:link`)
    - Attachment and external images
    - Task list macro
    - Status and expand macros
    - Language mapping for code blocks
  - `ConfluenceConverter` (CommonMark AST visitor):
    - Full markdown to storage format conversion
    - Headings, paragraphs, blockquotes
    - Bold, italic, inline code
    - Fenced and indented code blocks
    - Ordered and unordered lists
    - GFM tables
    - Task lists → Confluence task macro
    - External links and images
    - Local images as attachments
    - Configurable TOC insertion
    - Page link resolution support
  - Comprehensive unit tests for all components

- **Phase 6: Sync Engine (Complete)**
  - Sync model classes:
    - `SyncAction` with types: CREATE, UPDATE, DELETE, MOVE, SKIP
    - `SyncPlan` for planned sync operations with summary/details
    - `SyncState` for persistent page tracking (JSON serialization)
    - `SyncResult` and `ActionResult` for execution results
    - `PageState` for individual page state tracking
  - `SyncStateManager` for state persistence:
    - Load/save state from `.consync/state.json`
    - Space key validation
    - Version compatibility checking
    - State file reset capability
  - `DiffService` for change detection:
    - New page detection (CREATE)
    - Content hash comparison (UPDATE)
    - Title change detection (UPDATE)
    - Orphan detection (DELETE)
    - Parent change detection (MOVE)
    - Force update mode
    - Action sorting (creates before updates, deletes last)
  - `SyncExecutor` for plan execution:
    - Create pages via Confluence API
    - Update pages with version management
    - Delete orphaned pages
    - Move pages to new parents
    - State updates after each action
    - Error handling with partial state save
  - `SyncService` orchestrator:
    - Full sync workflow integration
    - Local file scanning
    - Hierarchy building
    - Plan generation
    - Dry-run mode support
    - Status reporting
    - State reset capability
  - Comprehensive unit tests for all sync components

### Planned
- CLI implementation enhancements
- End-to-end testing
- Packaging and distribution

---

## Version History

### [0.1.0] - Unreleased (In Development)

**Target**: Initial release with core functionality

#### Planned Features
- **CLI Commands**
  - `sync` - Synchronize local markdown to Confluence
  - `validate` - Validate configuration file
  - `status` - Show current sync status
  - `diff` - Preview changes without syncing

- **Configuration**
  - YAML-based configuration (`consync.yaml`)
  - Confluence connection settings
  - Space and root page configuration
  - File include/exclude patterns
  - TOC generation options

- **Markdown Processing**
  - CommonMark-compliant parsing
  - Code block conversion with syntax highlighting
  - Table conversion
  - Image handling (local and remote)
  - Internal link resolution

- **Confluence Integration**
  - REST API client for Cloud and Data Center
  - API token and PAT authentication
  - Page CRUD operations
  - Hierarchy management

- **Sync Features**
  - Create new pages
  - Update existing pages
  - Preserve page hierarchy
  - Content hash-based change detection
  - Dry-run mode
  - State persistence

---

## Development Log

### 2024-XX-XX - Phase 1: Project Setup
- [x] Created project repository
- [x] Initial README with project overview
- [x] Development plan documentation
- [x] Changelog setup
- [x] Maven pom.xml with Kotlin and all dependencies
- [x] Project directory structure
- [x] Main.kt entry point
- [x] CLI commands with Clikt (sync, validate, status, diff)
- [x] Configuration data classes (Config.kt)
- [x] ConfigLoader for YAML parsing
- [x] ConfigValidator for validation
- [x] Logback logging configuration
- [x] Unit tests for configuration

### 2024-XX-XX - Phase 2: Confluence API Client
- [x] ConfluenceClient interface with all operations
- [x] ConfluenceClientImpl with OkHttp
- [x] API model classes (Page, Space, Attachment, etc.)
- [x] Request/response DTOs
- [x] Exception hierarchy for error handling
- [x] Basic Auth support (Cloud)
- [x] PAT/Bearer token support (Data Center)
- [x] Retry logic with exponential backoff
- [x] ConfluenceClientFactory for easy instantiation
- [x] Unit tests for serialization
- [x] WireMock integration tests

### 2024-XX-XX - Phase 3: Markdown Processing
- [x] MarkdownDocument model classes
- [x] FileScanner for markdown discovery
- [x] FrontmatterParser for YAML frontmatter
- [x] MarkdownParser with CommonMark
- [x] LinkExtractor for internal links
- [x] Heading extraction and anchor generation
- [x] Content hashing (SHA-256)
- [x] Unit tests for all markdown components

### 2024-XX-XX - Phase 4: Hierarchy Management
- [x] PageNode tree structure model
- [x] HierarchyBuilder for tree construction
- [x] HierarchyResolver for relationships
- [x] Virtual nodes for directories without index.md
- [x] Link resolution and backlinks
- [x] Broken link detection
- [x] Hierarchy validation and statistics
- [x] Unit tests for all hierarchy components

### 2024-XX-XX - Phase 5: Storage Format Conversion
- [x] StorageFormatBuilder for XHTML generation
- [x] MacroRenderer for Confluence macros
- [x] ConfluenceConverter with CommonMark visitor
- [x] Code block conversion with language mapping
- [x] Table conversion with Confluence classes
- [x] Task list to Confluence task macro
- [x] Image handling (attachment and external)
- [x] TOC macro insertion
- [x] Unit tests for all converter components

### 2024-XX-XX - Phase 6: Sync Engine
- [x] SyncAction model with action types
- [x] SyncPlan model with summary and details
- [x] SyncState model with JSON serialization
- [x] SyncResult and ActionResult models
- [x] PageState for page tracking
- [x] SyncStateManager for state persistence
- [x] DiffService for change detection
- [x] SyncExecutor for plan execution
- [x] SyncService orchestrator
- [x] Dry-run mode support
- [x] Force sync mode
- [x] Orphan deletion support
- [x] Unit tests for all sync components

### Next Steps
- [ ] Phase 7: CLI implementation enhancements
- [ ] Phase 8: Testing
- [ ] Phase 9: Packaging

---

## Release Checklist

For each release, ensure:
- [ ] All tests passing
- [ ] Documentation updated
- [ ] CHANGELOG updated
- [ ] Version bumped in `pom.xml`
- [ ] Git tag created
- [ ] Release artifacts built
