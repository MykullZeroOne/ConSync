# ConSync Distribution Guide

Complete guide for distributing ConSync via GitHub Releases, Homebrew, Scoop, and Chocolatey.

## Overview

ConSync supports multiple distribution methods:

- **GitHub Releases**: Direct downloads for all platforms
- **Homebrew**: macOS and Linux package manager
- **Scoop**: Windows package manager (user-friendly)
- **Chocolatey**: Windows package manager (official)

## Quick Reference

### For Users

**macOS/Linux (Homebrew):**
```bash
brew tap yourusername/consync
brew install consync
```

**Windows (Scoop):**
```powershell
scoop bucket add consync https://github.com/yourusername/scoop-consync
scoop install consync
```

**Windows (Chocolatey):**
```powershell
choco install consync
```

**Direct Download:**
```bash
# See: https://github.com/yourusername/consync/releases
```

## For Maintainers

### Creating a Release

#### Step 1: Prepare Release

```bash
# Update version numbers in all files
./scripts/prepare-release.sh 1.0.0
```

This updates:
- `pom.xml`
- `Formula/consync.rb` (Homebrew)
- `scoop/consync.json` (Scoop)
- `chocolatey/consync.nuspec` (Chocolatey)

#### Step 2: Create and Push Release

```bash
# Create release commit and tag, then push
./scripts/create-release.sh 1.0.0
```

This:
1. Commits version changes
2. Creates git tag `v1.0.0`
3. Pushes to GitHub
4. Triggers GitHub Actions

#### Step 3: GitHub Actions Builds Release

GitHub Actions automatically:
1. Builds ConSync
2. Runs tests
3. Creates release packages:
   - `consync-1.0.0.jar` (cross-platform JAR)
   - `consync-1.0.0-unix.tar.gz` (macOS/Linux installer)
   - `consync-1.0.0-windows.zip` (Windows installer)
4. Generates SHA256 checksums
5. Creates GitHub Release
6. Updates Homebrew formula (if tap configured)

#### Step 4: Update Package Managers

After GitHub Release is created, update package managers:

**Scoop:**
```bash
# Push updated manifest to scoop bucket
cd ../scoop-consync
cp ../consync/scoop/consync.json .
git add consync.json
git commit -m "Update consync to 1.0.0"
git push
```

**Chocolatey:**
```bash
# Package and push to Chocolatey
cd chocolatey
choco pack
choco push consync.1.0.0.nupkg --api-key YOUR_API_KEY
```

## Setting Up Distribution Channels

### 1. GitHub Releases

**Already configured!** The GitHub Actions workflow handles this automatically.

**Required:**
- GitHub repository with releases enabled
- Push access to repository

**Configuration:**
- `.github/workflows/release.yml` (already created)

**How it works:**
1. Push a tag: `git push origin v1.0.0`
2. GitHub Actions runs
3. Release appears in Releases section

### 2. Homebrew Setup

Homebrew uses "taps" (custom repositories) for third-party formulas.

#### Create a Homebrew Tap

```bash
# Create new repository: homebrew-consync
# (Must be named homebrew-[name])
gh repo create yourusername/homebrew-consync --public

# Clone it
git clone https://github.com/yourusername/homebrew-consync
cd homebrew-consync

# Create Formula directory
mkdir -p Formula

# Copy formula
cp ../consync/Formula/consync.rb Formula/

# Commit and push
git add Formula/consync.rb
git commit -m "Add consync formula"
git push
```

#### Users Install With:

```bash
brew tap yourusername/consync
brew install consync
```

#### Updating Formula

The GitHub Actions workflow can automatically update the formula if you:

1. **Create a GitHub token** with repo access
2. **Add as secret**: `TAP_GITHUB_TOKEN` in your consync repository
3. **Update workflow** with your tap repository name

Or manually update:

```bash
cd homebrew-consync

# Get SHA256 of release tarball
wget https://github.com/yourusername/consync/releases/download/v1.0.0/consync-1.0.0-unix.tar.gz
sha256sum consync-1.0.0-unix.tar.gz

# Update Formula/consync.rb with new version and SHA

git add Formula/consync.rb
git commit -m "Update consync to 1.0.0"
git push
```

### 3. Scoop Setup (Windows)

Scoop uses JSON manifests in git repositories.

#### Create a Scoop Bucket

```bash
# Create new repository: scoop-consync
gh repo create yourusername/scoop-consync --public

# Clone it
git clone https://github.com/yourusername/scoop-consync
cd scoop-consync

# Copy manifest
cp ../consync/scoop/consync.json .

# Update with actual SHA256
wget https://github.com/yourusername/consync/releases/download/v1.0.0/consync-1.0.0-windows.zip
sha256sum consync-1.0.0-windows.zip
# Update hash in consync.json

# Commit and push
git add consync.json
git commit -m "Add consync manifest"
git push
```

#### Users Install With:

```powershell
scoop bucket add consync https://github.com/yourusername/scoop-consync
scoop install consync
```

#### Updating Manifest

```bash
cd scoop-consync

# Get new SHA256
wget https://github.com/yourusername/consync/releases/download/v1.0.0/consync-1.0.0-windows.zip
sha256sum consync-1.0.0-windows.zip

# Update consync.json with new version and hash

git add consync.json
git commit -m "Update consync to 1.0.0"
git push
```

**Note:** Scoop has `autoupdate` configured, so it can self-update using the SHA256 file from releases.

### 4. Chocolatey Setup (Windows)

Chocolatey is the official Windows package manager.

#### Prerequisites

1. **Create Chocolatey account**: https://community.chocolatey.org/
2. **Get API key**: From your account page
3. **Install Chocolatey**: https://chocolatey.org/install

#### Package and Publish

```bash
cd consync/chocolatey

# Update version and SHA in files
# - consync.nuspec
# - tools/chocolateyinstall.ps1

# Get SHA256
wget https://github.com/yourusername/consync/releases/download/v1.0.0/consync-1.0.0-windows.zip
sha256sum consync-1.0.0-windows.zip

# Update hash in tools/chocolateyinstall.ps1

# Pack
choco pack

# Test locally
choco install consync -source .

# Push to Chocolatey
choco push consync.1.0.0.nupkg --api-key YOUR_API_KEY

# Wait for moderation approval (first package only)
```

#### Users Install With:

```powershell
choco install consync
```

#### Updating Package

```bash
cd consync/chocolatey

# Update version in consync.nuspec and tools/chocolateyinstall.ps1
# Update SHA256 in tools/chocolateyinstall.ps1

choco pack
choco push consync.1.0.1.nupkg --api-key YOUR_API_KEY
```

**Note:** Chocolatey packages are moderated. First submission requires approval.

## Release Checklist

- [ ] Update version in all files
- [ ] Update CHANGELOG.md
- [ ] Test build locally: `mvn clean package`
- [ ] Run tests: `mvn test`
- [ ] Run prepare script: `./scripts/prepare-release.sh X.Y.Z`
- [ ] Review changes: `git diff`
- [ ] Run create release script: `./scripts/create-release.sh X.Y.Z`
- [ ] Wait for GitHub Actions to complete
- [ ] Verify GitHub Release was created
- [ ] Test downloads work
- [ ] Update Homebrew formula (if not automated)
- [ ] Update Scoop manifest
- [ ] Update and push Chocolatey package
- [ ] Announce release (optional)

## Testing Distributions

### Test Homebrew Formula

```bash
# Test formula locally
brew install --build-from-source Formula/consync.rb

# Test from tap
brew tap yourusername/consync
brew install consync

# Verify
consync --version
```

### Test Scoop Manifest

```powershell
# Test local manifest
scoop install scoop/consync.json

# Test from bucket
scoop bucket add consync https://github.com/yourusername/scoop-consync
scoop install consync

# Verify
consync --version
```

### Test Chocolatey Package

```powershell
# Test local package
choco install consync -source chocolatey

# Test from repository
choco install consync

# Verify
consync --version
```

## Automation Tips

### Auto-update Homebrew

Add to `.github/workflows/release.yml` (already included):

```yaml
- name: Update Homebrew Formula
  # Automatically updates formula in homebrew-consync tap
```

Requires: `TAP_GITHUB_TOKEN` secret

### Auto-update Scoop

Scoop's `autoupdate` feature (already configured in `consync.json`):

```json
"autoupdate": {
    "url": "https://github.com/yourusername/consync/releases/download/v$version/consync-$version-windows.zip",
    "hash": {
        "url": "$url.sha256"
    }
}
```

Run in scoop-consync repository:

```bash
# Updates manifest automatically
scoop checkver consync -u
```

### Auto-update Chocolatey

Currently manual, but can be automated with CI:

```yaml
# .github/workflows/update-chocolatey.yml
- name: Update Chocolatey
  run: |
    cd chocolatey
    choco pack
    choco push consync.$VERSION.nupkg --api-key ${{ secrets.CHOCO_API_KEY }}
```

## Distribution Metrics

Monitor downloads:

- **GitHub Releases**: Insights → Traffic → Releases
- **Homebrew**: `brew info consync` shows installs
- **Chocolatey**: Package page shows download count
- **Scoop**: No built-in metrics

## Troubleshooting

### GitHub Actions fails

Check:
- Java version in workflow
- Maven build succeeds locally
- All required secrets are set

### Homebrew formula fails

Check:
- SHA256 matches release tarball
- URL is correct
- Dependencies are correct

### Scoop install fails

Check:
- JSON syntax is valid
- URL and hash are correct
- Windows .zip structure matches `extract_dir`

### Chocolatey package rejected

Common issues:
- Missing or incorrect dependencies
- License file not found
- Icon URL not accessible
- Package doesn't install silently

## Support

For distribution issues:

- **Homebrew**: https://docs.brew.sh/
- **Scoop**: https://github.com/ScoopInstaller/Scoop
- **Chocolatey**: https://docs.chocolatey.org/

## Next Steps

1. **Create your first release**: `./scripts/create-release.sh 1.0.0`
2. **Set up Homebrew tap**: Follow "Homebrew Setup" section
3. **Set up Scoop bucket**: Follow "Scoop Setup" section
4. **Publish to Chocolatey**: Follow "Chocolatey Setup" section
5. **Announce**: Blog, Twitter, Reddit, etc.
