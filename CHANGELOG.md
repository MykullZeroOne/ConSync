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

### Planned
- Markdown to Confluence storage format conversion
- Page hierarchy management
- Sync engine with create/update/delete operations
- Dry-run mode for previewing changes
- State persistence for incremental syncs

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

### Next Steps
- [ ] Phase 4: Hierarchy management
- [ ] Phase 5: Storage format conversion
- [ ] Phase 6: Sync engine
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
