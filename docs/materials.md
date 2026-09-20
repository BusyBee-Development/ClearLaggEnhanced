---
id: materials
title: Materials
sidebar_position: 9
---

# Materials & Entity Type Names

Several config lists — most importantly `whitelist` and `item-whitelist` in
[Entity Clearing](features-modules/entity-clearing.md), and `limits-per-chunk` in
[Misc Entity Limiter](features-modules/misc-entity-limiter.md) — take **exact Bukkit enum names**,
not display names. Getting the name wrong means the entry silently does nothing (unmatched names
are simply never triggered).

## Two different enums, don't mix them up

- **`whitelist`** (Entity Clearing) and `limits-per-chunk` keys take **`EntityType`** names — what
  kind of *entity* it is (a mob, an item frame, a boat, a dropped-item entity, etc.).
- **`item-whitelist`** (Entity Clearing) takes **`Material`** names — what kind of *item* a dropped
  `Item` entity is carrying, e.g. protecting netherite ingots from being cleared even though the
  generic `Item` entity type itself isn't whitelisted.

## Real examples from the default config

Entity type names (`whitelist`):
```yaml
whitelist:
  - "VILLAGER"
  - "ZOMBIE_VILLAGER"
  - "IRON_GOLEM"
  - "ARMOR_STAND"
  - "TEXT_DISPLAY"
  - "ITEM_DISPLAY"
  - "BLOCK_DISPLAY"
  - "ITEM_FRAME"
  - "GLOW_ITEM_FRAME"
  - "LEASH_HITCH"
  - "LEASH_KNOT"
```

Material names (`item-whitelist`):
```yaml
item-whitelist:
  - "NETHERITE_INGOT"
```

## Finding the exact name you need

Bukkit/Paper's `Material` enum has roughly 1,000 entries and `EntityType` has around 130 — too
many to list here. The authoritative source is the Paper Javadocs:

- [`org.bukkit.Material`](https://jd.papermc.io/paper/1.20/org/bukkit/Material.html) — for dropped
  items (`item-whitelist`).
- [`org.bukkit.entity.EntityType`](https://jd.papermc.io/paper/1.20/org/bukkit/entity/EntityType.html) —
  for entity types (`whitelist`, `limits-per-chunk`).

Names are always `UPPER_SNAKE_CASE` and match the enum constant exactly, e.g. `GLOW_ITEM_FRAME`,
not `Glow Item Frame` or `glowitemframe`.

For Entity Clearing's `whitelist` and `item-whitelist` specifically, entries are uppercased
automatically before matching, so casing doesn't matter there. Other lists (like Misc Entity
Limiter's `limits-per-chunk` keys) aren't confirmed to normalize the same way — stick to uppercase
everywhere to match the plugin's own defaults and avoid surprises.
