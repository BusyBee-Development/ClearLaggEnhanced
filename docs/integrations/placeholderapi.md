---
id: placeholderapi
title: PlaceholderAPI
sidebar_position: 1
---

# PlaceholderAPI

If [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) is installed,
ClearLaggEnhanced registers the `clearlaggenhanced` expansion automatically — no config toggle
needed, it just activates when PlaceholderAPI is present.

| Placeholder | Returns |
|---|---|
| `%clearlaggenhanced_tps%` | Current server TPS, 2 decimal places |
| `%clearlaggenhanced_memory_used%` | Used memory in MB |
| `%clearlaggenhanced_memory_max%` | Maximum memory in MB |
| `%clearlaggenhanced_memory_percentage%` | Memory usage as a percentage, 1 decimal place |
| `%clearlaggenhanced_entities_total%` | Total loaded entities across the server |
| `%clearlaggenhanced_next_clear%` | Seconds until the next automatic entity clear |
| `%clearlaggenhanced_next_clear_formatted%` | Human-readable time until next clear, e.g. `5m 20s` |

The `tps`/`memory_*`/`entities_total` placeholders are backed by the
[Performance Monitor](../features-modules/performance-monitor.md) module; `next_clear*` are backed
by [Entity Clearing](../features-modules/entity-clearing.md). If the corresponding module is
disabled, these placeholders return `0`/`0.00`/`0s` rather than erroring.
