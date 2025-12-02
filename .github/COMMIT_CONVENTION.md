# Commit Message Convention

ConSync follows the [Conventional Commits](https://www.conventionalcommits.org/) specification for commit messages.

## Format

```
<type>[optional scope]: <description>

[optional body]

[optional footer(s)]
```

## Types

| Type | Description | Example |
|------|-------------|---------|
| `feat` | New feature | `feat: add support for custom templates` |
| `fix` | Bug fix | `fix: resolve link conversion issue` |
| `docs` | Documentation only | `docs: update installation guide` |
| `style` | Code style (formatting, missing semicolons) | `style: format according to ktlint` |
| `refactor` | Code refactoring | `refactor: simplify markdown parser logic` |
| `perf` | Performance improvement | `perf: optimize page sync algorithm` |
| `test` | Adding or updating tests | `test: add tests for hierarchy builder` |
| `build` | Build system or dependencies | `build: update Maven dependencies` |
| `ci` | CI configuration | `ci: add automated release workflow` |
| `chore` | Other changes (no production code) | `chore: update .gitignore` |
| `revert` | Revert a previous commit | `revert: feat: add custom templates` |

## Breaking Changes

Add `!` after type or `BREAKING CHANGE:` in footer:

```
feat!: remove support for Java 11

BREAKING CHANGE: Java 17 is now required
```

## Scopes

Use scopes to indicate which part of the codebase is affected:

| Scope | Description |
|-------|-------------|
| `cli` | Command-line interface |
| `config` | Configuration handling |
| `sync` | Sync engine |
| `markdown` | Markdown processing |
| `confluence` | Confluence API client |
| `hierarchy` | Hierarchy management |
| `installer` | Installation scripts |
| `docs` | Documentation |

## Examples

### Feature

```
feat(sync): add dry-run mode

Allows users to preview changes before syncing to Confluence.
Adds --dry-run flag to sync command.
```

### Bug Fix

```
fix(markdown): correctly parse nested lists

Previously, nested lists were flattened during conversion.
Now maintains proper nesting structure.

Fixes #42
```

### Breaking Change

```
feat(config)!: change default config file name

BREAKING CHANGE: Default config file changed from
.consync.yaml to consync.yaml
```

### Documentation

```
docs: add quickstart guide

Added comprehensive quickstart guide for new users.
Includes installation and first sync walkthrough.
```

### Multiple Changes

```
feat: add support for frontmatter

- Parse YAML frontmatter from markdown files
- Use frontmatter title if available
- Strip frontmatter from content by default

Closes #15
```

## Why Conventional Commits?

1. **Automated Changelog**: Generate changelog from commit messages
2. **Semantic Versioning**: Automatically determine version bumps
3. **Better History**: Standardized, searchable commit history
4. **Release Notes**: Auto-generate release notes from commits
5. **PR Labels**: Automatically label PRs based on commits

## Tools

### Commitizen (Interactive)

Install Commitizen for interactive commit messages:

```bash
npm install -g commitizen cz-conventional-changelog

# Use instead of git commit
git cz
```

### Commitlint (Validation)

Validate commit messages:

```bash
npm install -g @commitlint/cli @commitlint/config-conventional

# Check commit message
echo "feat: add feature" | commitlint
```

## Quick Reference

```bash
# Feature
git commit -m "feat: add new sync mode"

# Bug fix
git commit -m "fix: resolve authentication error"

# Breaking change
git commit -m "feat!: change config format"

# With scope
git commit -m "feat(cli): add verbose flag"

# With body
git commit -m "feat: add caching" -m "Implements in-memory cache for API responses"

# Close issue
git commit -m "fix: resolve sync error" -m "Fixes #123"
```

## Changelog Generation

Commits are automatically categorized:

| Commit Type | Changelog Section |
|-------------|-------------------|
| `feat` | Added |
| `fix` | Fixed |
| `perf` | Changed |
| `refactor` | Changed |
| `docs` | Changed (Documentation) |
| Breaking changes | Breaking Changes |

## Automated Workflows

When you commit with conventional commits:

1. **PR Auto-labeling**: PRs are automatically labeled based on commits
2. **Release Drafter**: Draft releases are updated with categorized changes
3. **Changelog Generation**: CHANGELOG.md can be auto-generated
4. **Version Bumping**: Semantic version is automatically determined
   - `feat` → minor version bump (1.0.0 → 1.1.0)
   - `fix` → patch version bump (1.0.0 → 1.0.1)
   - `BREAKING CHANGE` → major version bump (1.0.0 → 2.0.0)

## Tips

- **Be concise**: Keep subject line under 72 characters
- **Use imperative**: "add feature" not "added feature"
- **Reference issues**: Use "Fixes #123" or "Closes #123"
- **Explain why**: Use body to explain why, not what (code shows what)
- **One commit per change**: Keep commits focused
- **Test before commit**: Ensure code works

## Bad Examples

```bash
# Too vague
git commit -m "update code"

# Wrong tense
git commit -m "added feature"

# No type
git commit -m "support for templates"

# Too long subject
git commit -m "feat: this is a very long commit message that explains everything in the subject line instead of using the body"
```

## Good Examples

```bash
# Clear and concise
git commit -m "feat: add template support"

# With scope
git commit -m "fix(sync): handle network timeout"

# With body
git commit -m "refactor: simplify parser logic" -m "Extracted helper methods and reduced complexity"

# Breaking change
git commit -m "feat!: require Java 17" -m "BREAKING CHANGE: Java 11 is no longer supported"
```

## Resources

- [Conventional Commits](https://www.conventionalcommits.org/)
- [Keep a Changelog](https://keepachangelog.com/)
- [Semantic Versioning](https://semver.org/)
- [Commitizen](https://github.com/commitizen/cz-cli)
- [Commitlint](https://commitlint.js.org/)
