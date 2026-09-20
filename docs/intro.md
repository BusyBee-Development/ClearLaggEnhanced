---
id: intro
title: Introduction
sidebar_position: 1
---

# ClearLaggEnhanced

The definitive ClearLagg successor for 1.20+. ClearLaggEnhanced is a modern, high-performance lag
prevention plugin for servers running **Paper**, **Spigot**, **Purpur**, or **Folia** — built
around intelligent entity management, granular per-chunk limiters, real-time performance
monitoring, and an in-game Admin GUI for every module.

**Special thanks** to **bob7l**, the original developer of ClearLagg, whose pioneering work
inspired this enhanced version.

## What's in this guide

- **[Getting Started](getting-started/quick-start.md)** — install the plugin and run your first clear in under five minutes.
- **[Core Reference](core-reference/commands-permissions.md)** — every command, permission node, and the Admin GUI.
- **[Features & Modules](features-modules/entity-clearing.md)** — all eight modules in detail: what each does and every config option.
- **[Configuration](configuration/main-configuration.md)** — the module-toggle system, message customization, and database setup.
- **[Integrations](integrations/placeholderapi.md)** — PlaceholderAPI, the stacker plugins, ModernShowcase, GriefPrevention3D.
- **[Advanced Optimization](advanced-optimization/performance-guide.md)** — tuning tips, Folia specifics, migrating from other ClearLagg forks, and troubleshooting.
- **[Support](support/faq.md)** — FAQ, common issues, and how to get help.
- **[Materials](materials.md)** — how to find the right Bukkit material/entity names for whitelist entries.

## Key features

- **Automatic Entity Clearing** with adaptive interval scheduling and an MSPT-based performance gate.
- **Eight independent modules** — Entity Clearing, Mob Limiter, Spawner Limiter, Misc Entity Limiter, Chunk Finder, Performance Monitor, Packet Limiter, AFK Optimization — each toggleable on its own.
- **Real Folia support** — built on Paper's native region-scheduler API, not a bolted-on compatibility shim.
- **Interactive Admin GUI** for configuring and monitoring every module in-game, without editing YAML.
- **MySQL or SQLite persistence** via HikariCP connection pooling.
- **Plugin integrations** for WildStacker, RoseStacker, ModernShowcase, and GriefPrevention3D.
- **PlaceholderAPI support** with 7 live placeholders.

## Quick links

- **Source code:** [github.com/BusyBee-Development/ClearLaggEnhanced](https://github.com/BusyBee-Development/ClearLaggEnhanced)
- **Report an issue / request a feature:** [GitHub Issues](https://github.com/BusyBee-Development/ClearLaggEnhanced/issues)
- **Download:** [Modrinth](https://modrinth.com/plugin/clearlaggenhanced)
- **Discord:** [discord.gg/abdm29q7af](https://discord.gg/abdm29q7af)

Ready to go? Start with the **[Quick Start](getting-started/quick-start.md)** guide.
