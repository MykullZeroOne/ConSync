# Release Automation Guide

Complete guide for automating release notes and changelog generation.

## Overview

ConSync uses multiple automation tools to capture changes and generate release notes:

1. **Conventional Commits** - Standardized commit messages
2. **CHANGELOG.md** - Manually maintained changelog
3. **Release Drafter** - Automated draft releases from PRs
4. **Scripts** - Generate release notes from commits or changelog

## Methods

### Method 1: Conventional Commits (Recommended)

**Best for**: Capturing changes as you code

Write standardized commit messages that can be automatically parsed:

```bash
# Format: <type>[scope]: <description>
git commit -m "feat: add template support"
git commit -m "fix(sync): resolve timeout issue"
git commit -m "docs: update quickstart guide"
```

**Commit types**:
- `feat`: New feature → Added section
- `fix`: Bug fix → Fixed section
- `docs`: Documentation → Changed section
- `refactor`: Code refactoring → Changed section
- `perf`: Performance → Changed section
- `test`: Tests → (not in changelog)
- `chore`: Maintenance → (not in changelog)

**Generate changelog from commits**:

```bash
# Generate changelog entries from commits since last tag
./scripts/generate-changelog-from-commits.sh

# Copy output to CHANGELOG.md
```

See [COMMIT_CONVENTION.md](../.github/COMMIT_CONVENTION.md) for full guide.

### Method 2: CHANGELOG.md (Manual)

**Best for**: Detailed, curated release notes

Maintain `CHANGELOG.md` following [Keep a Changelog](https://keepachangelog.com/) format:

```markdown
## [Unreleased]

### Added
- New feature X
- New feature Y

### Fixed
- Bug fix Z

## [1.0.0] - 2024-01-15

### Added
- Initial release
```

**Workflow**:

1. **As you code**: Add entries to `[Unreleased]` section
   ```bash
   # Edit CHANGELOG.md, add under [Unreleased]
   ```

2. **When releasing**: Move unreleased changes to version section
   ```bash
   ./scripts/update-changelog.sh 1.1.0
   ```

3. **Generate release notes**: Extract section for GitHub release
   ```bash
   ./scripts/generate-release-notes.sh 1.1.0
   # Creates release-notes.md
   ```

### Method 3: Release Drafter (Automated)

**Best for**: Pull request-based workflow

GitHub Action that automatically drafts releases from PRs.

**Setup** (already configured):

- `.github/workflows/release-drafter.yml` - Workflow
- `.github/release-drafter.yml` - Configuration

**How it works**:

1. **Label PRs**: Add labels like `feature`, `fix`, `documentation`
2. **Auto-labels**: PRs are auto-labeled based on branch names
   - Branch `feat/new-feature` → label `feature`
   - Branch `fix/bug` → label `fix`
3. **Draft updated**: Release draft is automatically updated
4. **Publish**: When ready, publish the draft release

**Auto-labeling rules**:

| Branch Pattern | Label |
|----------------|-------|
| `feat/*` or `feature/*` | `feature` |
| `fix/*` or `hotfix/*` | `fix` |
| `docs/*` | `documentation` |
| `chore/*` | `chore` |

**Manual labels**:

- `breaking` - Breaking changes
- `major` - Major version bump
- `minor` - Minor version bump (features)
- `patch` - Patch version bump (fixes)

**View draft**:

Go to: https://github.com/MykullZeroOne/consync/releases

## Complete Workflow

### Option A: Conventional Commits + Automated Release

```bash
# 1. Make changes and commit with conventional commits
git add .
git commit -m "feat: add caching support"
git commit -m "fix: resolve link conversion bug"

# 2. Generate changelog from commits
./scripts/generate-changelog-from-commits.sh > changelog-draft.md

# 3. Review and edit changelog-draft.md

# 4. Copy to CHANGELOG.md under [Unreleased]
# (Edit CHANGELOG.md manually)

# 5. Prepare release
./scripts/prepare-release.sh 1.1.0
# This:
# - Updates version in all files
# - Builds and tests
# - Doesn't touch CHANGELOG (you already updated it)

# 6. Update CHANGELOG with version section
./scripts/update-changelog.sh 1.1.0

# 7. Generate release notes
./scripts/generate-release-notes.sh 1.1.0

# 8. Review release-notes.md

# 9. Create release
./scripts/create-release.sh 1.1.0
# This uses release-notes.md for the GitHub release
```

### Option B: Manual CHANGELOG + Scripts

```bash
# 1. As you develop, update CHANGELOG.md
vim CHANGELOG.md
# Add entries under [Unreleased]

# 2. When ready to release
./scripts/update-changelog.sh 1.1.0
# Moves [Unreleased] to [1.1.0]

# 3. Prepare release
./scripts/prepare-release.sh 1.1.0

# 4. Generate release notes from CHANGELOG
./scripts/generate-release-notes.sh 1.1.0

# 5. Create release
./scripts/create-release.sh 1.1.0
```

### Option C: Release Drafter (PR-based)

```bash
# 1. Create feature branch
git checkout -b feat/new-feature

# 2. Make changes and commit
git add .
git commit -m "Add new feature"

# 3. Push and create PR
git push -u origin feat/new-feature
gh pr create --title "feat: add new feature"

# 4. PR is auto-labeled as "feature"

# 5. Release draft is updated automatically

# 6. When ready to release:
# - Go to GitHub Releases
# - Review draft release
# - Edit if needed
# - Publish release

# 7. Or use CLI:
gh release create v1.1.0 --generate-notes
```

## Script Reference

### `generate-changelog-from-commits.sh`

Generates changelog entries from git commits.

```bash
# Generate from commits since last tag
./scripts/generate-changelog-from-commits.sh

# Save to file
./scripts/generate-changelog-from-commits.sh > changelog-draft.md

# Generate from specific range
git log v1.0.0..HEAD --pretty=format:"%s" | ...
```

**Output**: Categorized changelog entries

### `update-changelog.sh`

Updates CHANGELOG.md for a new version.

```bash
# Move [Unreleased] to [1.1.0]
./scripts/update-changelog.sh 1.1.0
```

**Does**:
- Moves unreleased changes to version section
- Adds release date
- Creates new empty [Unreleased] section
- Updates version links

### `generate-release-notes.sh`

Extracts release notes from CHANGELOG.md.

```bash
# Extract notes for version
./scripts/generate-release-notes.sh 1.1.0

# Creates: release-notes.md
```

**Output**: Formatted release notes with installation instructions

### `prepare-release.sh`

Prepares all files for release.

```bash
./scripts/prepare-release.sh 1.1.0
```

**Does**:
- Updates pom.xml
- Updates Homebrew formula
- Updates Scoop manifest
- Updates Chocolatey package
- Builds and tests

### `create-release.sh`

Complete release process.

```bash
./scripts/create-release.sh 1.1.0
```

**Does**:
- Calls `prepare-release.sh`
- Commits changes
- Creates git tag
- Pushes to GitHub
- Triggers GitHub Actions

## GitHub Actions Integration

Update `.github/workflows/release.yml` to use generated release notes:

```yaml
- name: Create Release
  uses: softprops/action-gh-release@v1
  with:
    tag_name: v${{ steps.get_version.outputs.VERSION }}
    name: ConSync v${{ steps.get_version.outputs.VERSION }}
    body_path: release-notes.md  # Generated by script
    files: |
      release/consync-*.jar
      # ...
```

Or use CHANGELOG.md directly:

```yaml
- name: Extract changelog
  run: |
    ./scripts/generate-release-notes.sh $VERSION

- name: Create Release
  uses: softprops/action-gh-release@v1
  with:
    body_path: release-notes.md
```

## Tips

### Keep CHANGELOG Updated

Add a git hook to remind about CHANGELOG:

```bash
# .git/hooks/pre-commit
#!/bin/bash

# Check if CHANGELOG.md was updated
if ! git diff --cached --name-only | grep -q "CHANGELOG.md"; then
    echo "⚠️  Reminder: Update CHANGELOG.md"
    echo ""
    echo "Press Enter to continue or Ctrl+C to cancel"
    read
fi
```

### Use Commitizen

Interactive commit messages:

```bash
npm install -g commitizen cz-conventional-changelog

# Use instead of git commit
git cz
```

### Lint Commit Messages

Validate commits in CI:

```yaml
# .github/workflows/commitlint.yml
- uses: wagoid/commitlint-github-action@v5
```

### Automatic Version Bumping

Determine version from commits:

```bash
# Scan commits for version bump
if git log --pretty=%s $LAST_TAG..HEAD | grep -q "feat"; then
    VERSION_BUMP="minor"
elif git log --pretty=%s $LAST_TAG..HEAD | grep -q "fix"; then
    VERSION_BUMP="patch"
else
    VERSION_BUMP="patch"
fi
```

## Best Practices

1. **Commit often**: Small, focused commits are easier to categorize
2. **Use scopes**: `feat(sync):` is better than `feat:`
3. **Update CHANGELOG**: Don't rely only on generated changelogs
4. **Review releases**: Always review auto-generated release notes
5. **Test scripts**: Run scripts on a feature branch first
6. **Keep history clean**: Squash messy commits before merging

## Examples

### Full Release with Conventional Commits

```bash
# Feature branch
git checkout -b feat/caching

# Commits
git commit -m "feat(sync): add in-memory cache"
git commit -m "test(sync): add cache tests"
git commit -m "docs: document caching configuration"

# Merge to main
git checkout main
git merge feat/caching

# Generate changelog
./scripts/generate-changelog-from-commits.sh > temp.md
# Review temp.md and add to CHANGELOG.md

# Release
./scripts/update-changelog.sh 1.1.0
./scripts/create-release.sh 1.1.0
```

### Manual CHANGELOG Workflow

```bash
# Edit as you go
vim CHANGELOG.md
# Add:
# ### Added
# - In-memory caching for API responses
# - Configuration option for cache TTL

# When ready
./scripts/update-changelog.sh 1.1.0
./scripts/generate-release-notes.sh 1.1.0
cat release-notes.md  # Review
./scripts/create-release.sh 1.1.0
```

## Troubleshooting

### No changelog entries generated

**Problem**: Script doesn't find any commits to categorize

**Solution**: Ensure commits follow conventional commit format

```bash
# Bad
git commit -m "add feature"

# Good
git commit -m "feat: add feature"
```

### Release notes empty

**Problem**: No section found in CHANGELOG.md for version

**Solution**: Update CHANGELOG.md first

```bash
./scripts/update-changelog.sh 1.1.0
./scripts/generate-release-notes.sh 1.1.0
```

### Wrong categorization

**Problem**: Commits categorized incorrectly

**Solution**: Edit CHANGELOG.md manually or use better commit types

## Resources

- [Conventional Commits](https://www.conventionalcommits.org/)
- [Keep a Changelog](https://keepachangelog.com/)
- [Semantic Versioning](https://semver.org/)
- [Release Drafter](https://github.com/release-drafter/release-drafter)
- [Commitizen](https://github.com/commitizen/cz-cli)
