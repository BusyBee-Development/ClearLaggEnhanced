---
id: chunk-finder
title: Chunk Finder
sidebar_position: 5
---

Scans loaded chunks and reports which ones have unusually high entity counts, so you can find the
source of lag (a runaway farm, an item-drop pile, an unattended mob grinder) instead of guessing.
Enabled by default. Config file: `module/chunk-finder/config.yml`.

```yaml
radius: 10
entity-threshold: 50
```

- `radius` — how many chunks out from the player running the scan to check, in every direction.
  The default of `10` checks a 21 × 21 chunk square around them.
- `entity-threshold` — minimum entity count for a chunk to be reported as "laggy." Every entity in
  the chunk counts: mobs, dropped items, armor stands, item frames, players and so on.

Run it in-game with `/lagg chunkfinder` (permission `CLE.chunkfinder`), or use the dedicated panel
in the [Admin GUI](../core-reference/admin-gui.md). It needs a player, because it scans around
where you stand; the console can't run it. Results list the 10 busiest chunks over the threshold,
busiest first, with their chunk coordinates and how far away they are. Only loaded chunks are
checked, and each one is counted on the thread that owns it, so the scan is safe on Folia.
