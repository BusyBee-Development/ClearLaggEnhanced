---
id: migration-guide
title: Migration Guide
sidebar_position: 3
---

# Migration Guide

ClearLaggEnhanced is a from-scratch rewrite inspired by the original **ClearLagg** by
[bob7l](https://github.com/bob7l) — it does not read or auto-migrate config files from ClearLagg,
its forks, or any other lag-prevention plugin. There's no automatic converter; treat this as a
fresh install and rebuild your settings intentionally rather than expecting a drop-in config swap.

## Before you switch

1. **Note your current whitelist/protection rules.** Whatever entities/items your old plugin
   protects from clearing, translate them into ClearLaggEnhanced's `whitelist` and `item-whitelist`
   in `module/entity-clearing/config.yml` — see [Entity Clearing](../features-modules/entity-clearing.md)
   and [Materials](../materials.md) for exact naming.
2. **Note your clear interval and any time-of-day rules.** ClearLaggEnhanced supports a fixed
   `interval` and count-based `adaptive-interval` tiers (entity count or player count) — it does
   not have day/night or event-based interval rules. If your old setup relied on those, the closest
   equivalent here is `adaptive-interval` reacting to entity count instead.
3. **Check which permission nodes your staff/ranks already have.** ClearLaggEnhanced uses its own
   `CLE.*` namespace (plus separate `clearlag.*` nodes for Packet Limiter specifically — see
   [Commands & Permissions](../core-reference/commands-permissions.md)). Old permission nodes from
   another plugin won't carry over.
4. **Disable and remove the old plugin** before installing this one if both register similar
   commands/aliases (`/clearlag`, `/cl`, etc.) — running two entity-clearing plugins simultaneously
   means they'll both act on the same entities independently, which is redundant at best and
   conflicting at worst.

## After switching

1. Install per [Installation](../getting-started/installation.md), let it generate default
   configs.
2. Rebuild your whitelist, intervals, and protection toggles from what you noted above.
3. Re-grant permissions to your staff ranks under the new `CLE.*` nodes.
4. Test with `/lagg clear` and watch chat/console for the expected protection behavior before
   relying on the automatic cycle unattended.

## Upgrading between ClearLaggEnhanced versions

This is the one migration path that *is* automatic — see
[Installation → Updating](../getting-started/installation.md#updating). New config keys are merged
in automatically; nothing to do manually.
