---
id: stacker-plugins
title: Stacker Plugins
sidebar_position: 2
---

# Stacker Plugins (WildStacker / RoseStacker)

ClearLaggEnhanced doesn't include its own entity-stacking feature — instead it hooks into
[WildStacker](https://www.spigotmc.org/resources/wildstacker.62789/) or
[RoseStacker](https://www.spigotmc.org/resources/rosestacker.94170/) if either is installed, so
Entity Clearing understands stacks rather than treating a stack of 50 as 50 separate clearable
entities.

Both integrations are off by default (`modules.wildstacker` / `modules.rosestacker` in
`config.yml`) and only do anything once the matching plugin is actually installed — enable the one
matching whichever stacker you run.

## What the hook does

- Detects whether an entity or dropped item is currently part of a stack (`stack size > 1`) via
  each plugin's own API (`WildStackerAPI` / `RoseStackerAPI`).
- If `protect-stacked-entities: true` in
  [Entity Clearing](../features-modules/entity-clearing.md#entity-protection), stacked entities
  are skipped by the clear entirely.
- If clearing does remove a stacked entity/item, it's removed through the stacker plugin's own
  removal API (`stack.remove()`), not a raw Bukkit `entity.remove()` — so the stacker plugin's own
  bookkeeping stays consistent instead of leaving orphaned stack state behind.

## Which one to use

Only enable the integration matching the stacker plugin you actually run. Running both WildStacker
and RoseStacker simultaneously isn't a supported combination for most servers regardless of this
plugin — pick one stacker plugin and enable its matching integration here.

Toggle either from `config.yml` or via the Integrations panel in the
[Admin GUI](../core-reference/admin-gui.md).
