---
id: requirements
title: Requirements
sidebar_position: 3
---

# Requirements

| | Requirement |
|---|---|
| **Minecraft version** | 1.20 – 1.21.11 (also tested on the 26.1–26.2 line) |
| **Server software** | Paper, Spigot, Purpur, or **Folia** |
| **Java** | 21 or higher |
| **API version** | Built against `paper-api 1.20.1-R0.1-SNAPSHOT` (`api-version: 1.20` in `plugin.yml`) |

## Optional dependencies

None of these are required — ClearLaggEnhanced runs standalone. Install them only if you want the
matching integration or module:

| Plugin | Unlocks |
|---|---|
| [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) | 7 live placeholders — see [PlaceholderAPI integration](../integrations/placeholderapi.md) |
| [WildStacker](https://www.spigotmc.org/resources/wildstacker.62789/) or [RoseStacker](https://www.spigotmc.org/resources/rosestacker.94170/) | Stacked-entity awareness so clearing doesn't wipe out a full stack at once — see [Stacker Plugins](../integrations/stacker-plugins.md) |
| ModernShowcase | Protects showcase entities from clearing — see [ModernShowcase integration](../integrations/modernshowcase.md) |
| GriefPrevention or GriefPrevention3D | Protects peaceful mobs inside player claims — see [GriefPrevention3D integration](../integrations/griefprevention3d.md) |
| [ProtocolLib](https://www.spigotmc.org/resources/protocollib.1997/) | Required if you enable the **Packet Limiter** module — see [Packet Limiter](../features-modules/packet-limiter.md) |

## Database

ClearLaggEnhanced needs somewhere to persist state and defaults to a bundled **SQLite** file — no
setup required. MySQL is supported as a drop-in alternative for networks that want centralized
storage. See [Database Setup](../configuration/database-setup.md) for both options.

## Folia note

ClearLaggEnhanced is built on Paper's native `io.papermc.paper.threadedregions.scheduler` API
(shipped in paper-api since 1.20.1), so it runs correctly on regular Paper, Folia, and Folia forks
alike without a third-party scheduler shim. One caveat: the Entity Clearing module's
**MSPT performance gate** is not supported on Folia due to its regionized architecture — see
[Folia Support](../advanced-optimization/folia-support.md) for the full picture.
