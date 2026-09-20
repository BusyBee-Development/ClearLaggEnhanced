---
id: performance-monitor
title: Performance Monitor
sidebar_position: 6
---

# Performance Monitor

Tracks live TPS and memory usage and powers the `/lagg tps`, `/lagg ram`, and the PlaceholderAPI
performance placeholders. Enabled by default. Config file: `module/performance/config.yml`.

```yaml
update-interval: 20
```

- `update-interval` — how often (in ticks) the module refreshes its internal TPS/memory snapshot.
  `20` ticks = once per second. Lower values give more responsive readings at a small CPU cost;
  there's rarely a reason to go below 20.

## Commands

- `/lagg tps` (`CLE.tps`) — current TPS, color-coded by health.
- `/lagg ram` (`CLE.ram`) — detailed memory usage (used/max/percentage).

## Snapshot notifications

Players with `CLE.performance.snapshot` receive a notification whenever a performance snapshot is
triggered and saved.

## PlaceholderAPI

This module backs 4 of the plugin's 7 PlaceholderAPI placeholders (`tps`, `memory_used`,
`memory_max`, `memory_percentage`) — see [PlaceholderAPI](../integrations/placeholderapi.md).

Use the Performance panel in the [Admin GUI](../core-reference/admin-gui.md) for a live in-game
view without running commands.
