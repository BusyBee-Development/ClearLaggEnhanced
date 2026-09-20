---
id: mob-limiter
title: Mob Limiter
sidebar_position: 2
---

# Mob Limiter

Caps how many mobs can exist in a single chunk, preventing runaway spawning (farms, spawners, or
natural spawning in unloaded-then-reloaded areas) from tanking performance. Enabled by default.
Config file: `module/mob-limiter/config.yml`.

```yaml
max-mobs-per-chunk: 50

# If set to true, mobs spawned from spawners will not be counted towards the chunk limit.
ignore-spawners: false

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
- `per-type-limits` — an additional, tighter cap for specific entity types, checked independently
  of the global limit. Any `EntityType` name not listed here isn't individually capped (only the
  global `max-mobs-per-chunk` applies to it).

Configurable in-game via the Mob Limiter panel in the [Admin GUI](../core-reference/admin-gui.md).
