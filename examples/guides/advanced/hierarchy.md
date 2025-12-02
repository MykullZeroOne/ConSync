# Hierarchy Management

Learn how ConSync translates directory structures into Confluence page hierarchies.

## Overview

ConSync automatically creates hierarchical relationships between Confluence pages based on your local directory structure. Understanding these rules helps you organize documentation effectively.

## Basic Hierarchy Rules

### Rule 1: Directories Become Parent Pages

Each directory in your structure becomes a parent page in Confluence:

```
docs/
├── guides/           → "Guides" (parent page)
│   ├── guide1.md    → "Guide 1" (child of Guides)
│   └── guide2.md    → "Guide 2" (child of Guides)
```

### Rule 2: Files Become Child Pages

Markdown files within a directory become children of that directory's page:

```
docs/
├── api/              → "API" (parent page)
│   ├── users.md     → "Users" (child of API)
│   ├── products.md  → "Products" (child of API)
│   └── orders.md    → "Orders" (child of API)
```

### Rule 3: Index Files Provide Directory Content

Files named `index.md` provide the content for directory pages:

```
docs/
├── guides/
│   ├── index.md     → Content for "Guides" page
│   ├── guide1.md    → "Guide 1" (sibling of index)
│   └── guide2.md    → "Guide 2" (sibling of index)
```

**Without `index.md`:**
- Directory gets a generated page with just the title
- Content is empty except for auto-generated table of contents

**With `index.md`:**
- Directory page uses content from `index.md`
- Other `.md` files in the directory become siblings

### Rule 4: Root Level Files

Files at the root level become top-level pages:

```
docs/
├── index.md         → "Documentation Home" (top level)
├── overview.md      → "Overview" (top level)
└── guides/
    └── guide1.md    → "Guide 1" (child of Guides)
```

## Complex Hierarchies

### Multi-Level Nesting

ConSync supports unlimited nesting depth:

```
docs/
├── index.md                          → Documentation Home
├── getting-started/                  → Getting Started
│   ├── index.md                      →   (Getting Started content)
│   ├── installation.md               →   Installation
│   └── quickstart.md                 →   Quickstart
├── guides/                           → Guides
│   ├── index.md                      →   (Guides content)
│   ├── basic/                        →   Basic
│   │   ├── index.md                  →     (Basic content)
│   │   ├── tutorial-1.md             →     Tutorial 1
│   │   └── tutorial-2.md             →     Tutorial 2
│   └── advanced/                     →   Advanced
│       ├── index.md                  →     (Advanced content)
│       ├── patterns.md               →     Patterns
│       └── best-practices.md         →     Best Practices
└── reference/                        → Reference
    ├── index.md                      →   (Reference content)
    ├── api.md                        →   API
    └── cli.md                        →   CLI
```

**Resulting Confluence hierarchy:**

```
Documentation Home
├── Getting Started
│   ├── Installation
│   └── Quickstart
├── Guides
│   ├── Basic
│   │   ├── Tutorial 1
│   │   └── Tutorial 2
│   └── Advanced
│       ├── Patterns
│       └── Best Practices
└── Reference
    ├── API
    └── CLI
```

### Empty Directories

Directories without `index.md` get auto-generated placeholder content:

```
docs/
└── guides/          → "Guides" page with auto-generated content
    └── guide1.md    → "Guide 1"
```

**Auto-generated content:**

```
# Guides

This page contains:

- Guide 1
```

## Page Titles

ConSync determines page titles using the `title_source` configuration:

### From Filename (Default)

```yaml
content:
  title_source: filename
```

Converts filename to title:
- `getting-started.md` → "Getting Started"
- `api-reference.md` → "API Reference"
- `faq.md` → "FAQ"

### From Frontmatter

```yaml
content:
  title_source: frontmatter
```

Uses `title` field from YAML frontmatter:

```markdown
---
title: "Custom Page Title"
---

# Content starts here
```

### From First Heading

```yaml
content:
  title_source: first_heading
```

Uses the first H1 heading:

```markdown
# This Becomes The Title

Content starts here...
```

## Link Preservation

ConSync automatically converts markdown links to Confluence page links.

### Relative Links

```markdown
<!-- In getting-started/installation.md -->
See the [Quickstart](quickstart.md) guide.

<!-- Converts to Confluence page link -->
```

### Parent Directory Links

```markdown
<!-- In guides/basic/tutorial-1.md -->
Return to [Guides](../index.md).

<!-- Converts to parent page link -->
```

### Cross-Section Links

```markdown
<!-- In guides/guide1.md -->
Check the [API Reference](../reference/api.md).

<!-- Converts to correct Confluence page link -->
```

### Root Links

```markdown
<!-- In any nested page -->
Back to [Home](/index.md).

<!-- Converts to root page link -->
```

## Advanced Patterns

### Flat Structure with Hierarchy

Create flat file structure but hierarchical pages:

**Directory structure:**

```
docs/
├── 01-overview.md
├── 02-getting-started.md
├── 03-guides-basic.md
├── 04-guides-advanced.md
└── 05-reference.md
```

**Use categories in frontmatter:**

```markdown
---
title: "Basic Tutorial"
parent: "Guides"
---
```

*(Note: This requires custom frontmatter processing - future feature)*

### Mixed Hierarchy

Combine files and directories at same level:

```
docs/
├── index.md         → Documentation Home
├── overview.md      → Overview (sibling of Home)
├── guides/          → Guides (sibling of Home)
│   └── guide1.md
└── reference/       → Reference (sibling of Home)
    └── api.md
```

### Shared Content

Reference shared content from multiple locations:

```markdown
<!-- In multiple files -->
{{include: ../shared/common-intro.md}}
```

*(Note: Includes require preprocessor - not native to ConSync)*

### Navigation Structure

Create navigation pages with links:

```markdown
<!-- guides/index.md -->
# Guides

## Basic Guides
- [Tutorial 1](basic/tutorial-1.md)
- [Tutorial 2](basic/tutorial-2.md)

## Advanced Guides
- [Patterns](advanced/patterns.md)
- [Best Practices](advanced/best-practices.md)
```

## Configuration Examples

### Standard Hierarchy

```yaml
content:
  root_page: "Documentation"
  title_source: filename

files:
  index_file: "index.md"
  include:
    - "**/*.md"
```

### Flat Documentation

```yaml
content:
  root_page: "All Pages"
  title_source: first_heading

files:
  # No index files needed
  include:
    - "*.md"
  exclude:
    - "**/index.md"
```

### Deep Nesting

```yaml
content:
  root_page: "Root"
  title_source: frontmatter

files:
  index_file: "README.md"  # Use README.md as index
  include:
    - "**/*.md"
  exclude:
    - "**/drafts/**"
```

## Best Practices

### 1. Use Descriptive Directory Names

Good:
```
docs/
├── getting-started/
├── user-guides/
├── api-reference/
└── troubleshooting/
```

Bad:
```
docs/
├── section1/
├── section2/
├── misc/
└── other/
```

### 2. Provide Index Files

Always create `index.md` for directories:

```markdown
<!-- guides/index.md -->
# Guides

Welcome to the guides section.

## Available Guides

This section includes:
- [Basic Guide](basic.md)
- [Advanced Guide](advanced.md)

## Prerequisites

Before reading these guides...
```

### 3. Limit Nesting Depth

Aim for 3-4 levels maximum:

```
docs/              # Level 0
├── section/       # Level 1
│   ├── sub/       # Level 2
│   │   └── page/  # Level 3 (max)
```

Deeper nesting becomes hard to navigate.

### 4. Use Consistent Naming

Choose a naming convention:

**Kebab case (recommended):**
```
getting-started.md
api-reference.md
user-guide.md
```

**Snake case:**
```
getting_started.md
api_reference.md
user_guide.md
```

**Camel case:**
```
gettingStarted.md
apiReference.md
userGuide.md
```

### 5. Mirror Confluence Structure

If migrating from Confluence, match the existing hierarchy:

**Existing Confluence:**
```
Space
├── Getting Started
│   ├── Installation
│   └── Quickstart
└── Guides
    └── Advanced
```

**Local structure:**
```
docs/
├── getting-started/
│   ├── installation.md
│   └── quickstart.md
└── guides/
    └── advanced.md
```

## Troubleshooting Hierarchy Issues

### Pages in Wrong Location

**Problem:** Page appears as sibling instead of child

**Cause:** File is in wrong directory

**Solution:** Move file to correct directory:
```bash
mv wrong-location.md correct-parent/
```

### Missing Parent Pages

**Problem:** Directory doesn't create page

**Cause:** No `index.md` and ConSync skipping empty directories

**Solution:** Create `index.md`:
```bash
echo "# Section Name" > section/index.md
```

### Circular References

**Problem:** Links create circular navigation

**Cause:** Bidirectional links between pages

**Solution:** Create hierarchical navigation:
- Child links to parent: ✓
- Parent links to child: ✓
- Siblings link to each other: ✓
- Child links to grandparent: Use sparingly

### Duplicate Titles

**Problem:** Multiple pages with same title

**Cause:** Duplicate filenames or title configuration

**Solution:**
- Rename files to be unique
- Use frontmatter with unique titles
- Organize into different directories

## Next Steps

- Learn about [Authentication](authentication.md) options
- Review [Configuration Guide](../configuration.md)
- Check [Usage Guide](../usage.md) for sync commands
