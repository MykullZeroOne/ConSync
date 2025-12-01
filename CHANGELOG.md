# Changelog

All notable changes to ConSync will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Project documentation and README with feature overview
- Development plan document (`docs/DEVELOPMENT_PLAN.md`)
- This changelog file

### Planned
- Maven project setup with Kotlin configuration
- CLI framework using Clikt
- Configuration file parsing (YAML)
- Confluence REST API client
- Markdown parsing with CommonMark
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

### 2024-XX-XX - Project Initialization
- [x] Created project repository
- [x] Initial README with project overview
- [x] Development plan documentation
- [x] Changelog setup

### Next Steps
- [ ] Phase 1: Maven project setup
- [ ] Phase 2: Confluence API client
- [ ] Phase 3: Markdown processing
- [ ] Phase 4: Hierarchy management
- [ ] Phase 5: Storage format conversion
- [ ] Phase 6: Sync engine
- [ ] Phase 7: CLI implementation
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
