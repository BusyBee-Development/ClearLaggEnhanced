---
id: performance-monitor
title: Performance Monitor
sidebar_position: 6
---

Tracks live TPS and memory usage and powers the `/lagg tps`, `/lagg ram`, and the PlaceholderAPI
performance placeholders. Enabled by default. Config file: `module/performance/config.yml`.

```yaml
update-interval: 20
```

- `update-interval` — not used yet; changing it has no effect. TPS and memory are read live each
  time a command or placeholder asks for them, and the module's own check (entity total and the
  low-TPS snapshot below) runs every 5 seconds.

## Commands

- `/lagg tps` (`CLE.tps`) — current TPS, color-coded by health.
- `/lagg ram` (`CLE.ram`) — detailed memory usage (used/max/percentage).

## Snapshot notifications

When TPS drops below 17, the module saves a lag snapshot to
`plugins/ClearLaggEnhanced/snapshots/snapshot-<date>_<time>.txt`, at most once every 5 minutes. It
records TPS, memory, the entity total, online players and the 10 busiest loaded chunks.

Players with `CLE.performance.snapshot` receive a notification whenever a performance snapshot is
triggered and saved.

## PlaceholderAPI

This module backs 4 of the plugin's 7 PlaceholderAPI placeholders (`tps`, `memory_used`,
`memory_max`, `memory_percentage`) — see [PlaceholderAPI](../integrations/placeholderapi.md).

Use the Performance panel in the [Admin GUI](../core-reference/admin-gui.md) for a live in-game
view without running commands.
