---
id: chunk-finder
title: Chunk Finder
sidebar_position: 5
---

# Chunk Finder

Scans loaded chunks and reports which ones have unusually high entity counts, so you can find the
source of lag (a runaway farm, an item-drop pile, an unattended mob grinder) instead of guessing.
Enabled by default. Config file: `module/chunk-finder/config.yml`.

```yaml
radius: 10
entity-threshold: 50
```

- `radius` — how many chunks out from the command executor (or a specified location) to scan.
- `entity-threshold` — minimum entity count for a chunk to be reported as "laggy."

Run it with `/lagg chunkfinder` (permission `CLE.chunkfinder`), or use the dedicated panel in the
[Admin GUI](../core-reference/admin-gui.md) for a visual list of results.
