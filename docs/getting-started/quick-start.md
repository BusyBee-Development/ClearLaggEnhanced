---
id: quick-start
title: Quick Start
sidebar_position: 1
---

# Quick Start

Get ClearLaggEnhanced running with sane defaults in about five minutes.

## 1. Install

Drop the jar in `plugins/` and start your server. See [Installation](installation.md) if you want
the full walkthrough.

## 2. Check what's enabled by default

Open `plugins/ClearLaggEnhanced/config.yml`. The `modules:` block controls which of the eight
modules are active — this is the **only** runtime source of truth for enable/disable state:

```yaml
modules:
  entity-clearing: true
  mob-limiter: true
  spawner-limiter: true
  misc-entity-limiter: true
  chunk-finder: true
  performance: true
  packet-limiter: false
  afk: false

  # 3rd Party Plugins
  wildstacker: false
  rosestacker: false
  modernshowcase: false
  griefprevention3d: false
```

Five modules ship on by default: Entity Clearing, Mob Limiter, Spawner Limiter, Misc Entity
Limiter, Chunk Finder, and Performance. Packet Limiter and AFK Optimization are off by default
(the former needs ProtocolLib, the latter you'll want to tune per-server first). The four
integration modules only do anything once the matching plugin is installed.

## 3. Try the core commands

```
/lagg help          # full subcommand list
/lagg tps            # current server TPS
/lagg ram             # memory usage
/lagg clear           # manually clear entities right now
/lagg next             # time until the next automatic clear
/lagg admin            # open the in-game Admin GUI
```

Full command/permission reference: [Commands & Permissions](../core-reference/commands-permissions.md).

## 4. Tune Entity Clearing to your server

The defaults clear entities every 5 minutes (`interval: 300` in
`module/entity-clearing/config.yml`) with a sensible whitelist (villagers, item frames, display
entities, etc. are protected). If that's too aggressive or too relaxed, see
[Entity Clearing](../features-modules/entity-clearing.md) for the full option set — adaptive
intervals, the MSPT performance gate, and per-entity protection toggles.

## 5. Open the Admin GUI

`/lagg admin` (permission `CLE.admin`, default: op) gives you a menu of every enabled module.
Left-click a module to configure it in-game; right-click to toggle it on/off without touching a
config file. See [Admin GUI](../core-reference/admin-gui.md).

## 6. Reload after editing configs

Any time you hand-edit a YAML file, apply it with `/lagg reload` instead of restarting.

## Where to go next

- [Requirements](requirements.md) if you haven't confirmed your server meets them.
- [Main Configuration](../configuration/main-configuration.md) for the database and module-toggle system.
- [Performance Guide](../advanced-optimization/performance-guide.md) for tuning recommendations by server size.
