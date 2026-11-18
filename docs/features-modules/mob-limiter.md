---
id: mob-limiter
title: Mob Limiter
sidebar_position: 2
---

Caps how many mobs can exist in a single chunk, preventing runaway spawning (farms, spawners, or
natural spawning in unloaded-then-reloaded areas) from tanking performance. Enabled by default.
Config file: `module/mob-limiter/config.yml`.

```yaml
max-mobs-per-chunk: 50

# If set to true, mobs spawned from spawners will not be counted towards the chunk limit.
ignore-spawners: false

# Whether mobs that entity clearing keeps for a player count towards the limits.
count-kept-mobs: true

per-type-limits:
  enabled: true
  limits:
    ZOMBIE: 10
    SKELETON: 10
    CREEPER: 8
    SPIDER: 8
    ENDERMAN: 5
    VILLAGER: 15
    COW: 12
    SHEEP: 12
    PIG: 12
    CHICKEN: 15
    IRON_GOLEM: 3
```

- `max-mobs-per-chunk` — global cap on living entities per chunk, checked before any spawn is
  allowed to complete.
- `ignore-spawners` — when `true`, mobs that spawn from a mob spawner block don't count toward the
  chunk's limit (useful if you want farms to keep working while still capping natural spawns).
- `count-kept-mobs`: when `true` (the default), mobs that clears keep for a player still count, so
  breeding pens stay capped. Set it to `false` to leave them out of the count, as versions before
  26.10.2 did. See [What counts](#what-counts).
- `per-type-limits` — an additional, tighter cap for specific entity types, checked independently
  of the global limit. Any `EntityType` name not listed here isn't individually capped (only the
  global `max-mobs-per-chunk` applies to it).

## What counts

Every living mob counts, including ones [Entity Clearing](entity-clearing.md) keeps for players:
bred, tamed, named, leashed, ridden, whitelisted and passive mobs. Clears leave them alone, but
breeding, spawn eggs and natural spawns are still capped. A newborn over the cap isn't born.

With `count-kept-mobs: false`, the mobs kept because of something a player did are left out of the
count and can always spawn or be bred:

- named, tamed and bred mobs,
- leashed, ridden and saddled mobs,
- mobs in boats or minecarts, and mobs from buckets,
- armored mobs, when `protect-armored-entities` is on.

Each one follows its own toggle in `module/entity-clearing/config.yml`. Whitelisted and passive mobs
still count either way, because they are kept by type rather than by what a player did.

The protection toggles in `module/entity-clearing/config.yml` apply to the Mob Limiter even while
the Entity Clearing module is turned off.

Left out of the counts, and never blocked from spawning:

- players,
- entities with a protection tag (`CLE_PROTECTED` or `protected-entity-tags`),
- stacked entities, when `protect-stacked-entities` is on,
- MythicMobs and InfernalMobs mobs, Citizens NPCs and plugin pets, when their protection is on,
- ModernShowcase, ItemsAdder, Oraxen and Nexo entities, when their protection is on,
- entities another plugin protects through the [Developer API](../integrations/developer-api.md),
  unless that plugin asks for them to count.

If one of these checks fails for an entity, the Mob Limiter counts that entity, so an error can never
switch the limit off.

Configurable in-game via the Mob Limiter panel in the [Admin GUI](../core-reference/admin-gui.md).
