---
title: Markdown Formatting
description: Supported markdown features in ConSync
weight: 1
---

# Markdown Formatting Guide

ConSync supports CommonMark with GFM extensions.

## Text Formatting

- **Bold text** using `**bold**`
- *Italic text* using `*italic*`
- `Inline code` using backticks
- ~~Strikethrough~~ using `~~text~~`

## Links

Internal links to other pages:
- [Back to Home](../index.md)
- [Configuration Guide](../getting-started/configuration.md)

External links:
- [Confluence Documentation](https://confluence.atlassian.com)

## Images

Images are uploaded as attachments:

![Example Image](./images/example.png)

## Code Blocks

### JavaScript

```javascript
function greet(name) {
    console.log(`Hello, ${name}!`);
}
```

### Python

```python
def greet(name):
    print(f"Hello, {name}!")
```

### SQL

```sql
SELECT * FROM users WHERE active = true;
```

## Blockquotes

> This is a blockquote.
> It can span multiple lines.

> **Note**: Important information here!

## Lists

### Unordered List

- Item 1
- Item 2
  - Nested item 2.1
  - Nested item 2.2
- Item 3

### Ordered List

1. First step
2. Second step
3. Third step

## Tables

| Column 1 | Column 2 | Column 3 |
|----------|:--------:|---------:|
| Left     | Center   | Right    |
| aligned  | aligned  | aligned  |
