---
id: module-configuration
title: Module Configuration
sidebar_position: 2
---

# Module Configuration

Each module has its own config file under `plugins/ClearLaggEnhanced/module/<module-name>/config.yml`:

| Module | Path |
|---|---|
| [Entity Clearing](../features-modules/entity-clearing.md) | `module/entity-clearing/config.yml` |
| [Mob Limiter](../features-modules/mob-limiter.md) | `module/mob-limiter/config.yml` |
| [Spawner Limiter](../features-modules/spawner-limiter.md) | `module/spawner-limiter/config.yml` |
| [Misc Entity Limiter](../features-modules/misc-entity-limiter.md) | `module/misc-entity-limiter/config.yml` |
| [Chunk Finder](../features-modules/chunk-finder.md) | `module/chunk-finder/config.yml` |
| [Performance Monitor](../features-modules/performance-monitor.md) | `module/performance/config.yml` |
| [Packet Limiter](../features-modules/packet-limiter.md) | `module/packet-limiter/config.yml` |
| [AFK Optimization](../features-modules/afk-optimization.md) | `module/afk/config.yml` |

Enabling/disabling a module is **not** done in these files — that's controlled entirely by the
`modules:` block in the root `config.yml` (see [Main Configuration](main-configuration.md)). Any
`enabled` key you see inside a module's own `config.yml` is a legacy leftover kept on disk for
compatibility and is ignored at runtime.

## Editing

Two ways to change a module's settings:

1. **Hand-edit** the module's `config.yml`, then `/lagg reload` (`CLE.reload`).
2. **In-game**, via `/lagg admin` → left-click the module icon → edit values through the module's
   own GUI panel (click an item, type the new value in chat when prompted). See
   [Admin GUI](../core-reference/admin-gui.md).

## Adding new keys after an update

Updating the plugin can introduce new config keys for a module. On next load, the built-in
migrator adds any missing keys (with their default values and comments) to your existing file
without touching what you've already set, and saves a timestamped backup first — see
[Installation → Updating](../getting-started/installation.md#updating).
