---
id: entity-clearing
title: Entity Clearing
sidebar_position: 1
---

# Entity Clearing

The core module: periodically removes entities from the world to keep entity counts under
control, with extensive protection rules so you don't accidentally wipe out things players care
about. Config file: `module/entity-clearing/config.yml`.

## Interval

```yaml
interval: 300
```

Default/fallback time in seconds between automatic clears. Overridden by adaptive scheduling below
if enabled.

:::info Paused servers
On 1.21.2+ the vanilla `pause-when-empty-seconds` setting in `server.properties` pauses the server
once nobody is online (Paper and forks such as Leaf keep it). While the server is paused the
countdown holds, and it picks up where it left off when ticking resumes. No clears or warnings run
while paused.
:::

## Adaptive interval scheduling

Re-evaluated at the start of each clearing cycle. The highest matching threshold wins; if none
matches, the base `interval` above is used.

```yaml
adaptive-interval:
  enabled: false
  metric: "ENTITY_COUNT" # ENTITY_COUNT | PLAYER_COUNT
  tiers:
    - threshold: 0
      interval: 900
    - threshold: 5000
      interval: 750
    - threshold: 7000
      interval: 600
```

`ENTITY_COUNT` bases tiers on currently loaded entities in the configured worlds; `PLAYER_COUNT`
bases them on the online player count. With the defaults above: under 5,000 entities → clear every
15 minutes; 5,000–6,999 → every 12.5 minutes; 7,000+ → every 10 minutes.

## Performance gate

```yaml
performance-gate:
  enabled: false
  mspt-threshold: 45.0
  sustained-seconds: 300
```

When enabled, automatic clearing is **deferred while the server is healthy** — clears only fire
once average MSPT has stayed at or above `mspt-threshold` for `sustained-seconds`. This avoids
clearing on a schedule when there's nothing to gain from it.

:::caution Not supported on Folia
Folia's regionized architecture doesn't have a single server-wide MSPT figure the way Paper does,
so this feature is a no-op there. If you're on Folia, leave `performance-gate.enabled: false` and
rely on `adaptive-interval` instead. See [Folia Support](../advanced-optimization/folia-support.md).
:::

## Entity protection

Top-level toggles:

```yaml
protect-named-entities: true
protect-persistent-named-only: false
protect-tamed-entities: true
protect-armored-entities: false
protect-stacked-entities: false
whitelist-all-mobs: false
protect-passive-mobs: false
```

- `protect-named-entities` — skip any entity with a custom name (or a named dropped item).
- `protect-persistent-named-only` — when combined with the above, only protects named entities
  that are also marked persistent (survives unloading), rather than any named entity.
- `protect-tamed-entities` — skip tameable entities (wolves, cats, horses, etc.) that are tamed.
- `protect-armored-entities` — skip living entities wearing any armor piece.
- `protect-stacked-entities` — skip entities detected as stacked by a connected stacker plugin.
- `whitelist-all-mobs` — nuclear option: protect every `LivingEntity`, clearing only items/other
  non-living entities.
- `protect-passive-mobs` — protect every friendly mob (animals including happy ghasts, villagers
  and wandering traders, golems, fish, squid, bats, allays) while still clearing hostile ones.
  Hostile mobs that share a friendly type, such as hoglins and shulkers, are still cleared. The
  [Mob Limiter](mob-limiter.md)'s spawn caps still apply to these mobs, so enabling this can't
  let animal farms grow without limit. An easy alternative to listing every friendly type in
  `whitelist`.

### Extra protections

```yaml
extra-protections:
  mobs-in-boats: true
  mobs-in-minecarts: true
  leashed-mobs: true
  saddled-mobs: true
  ridden-mobs: true
  bucket-mobs: true
  mobs-from-breeding: true
  modern-showcase: true
  player-heads: true
  pets-module: true
  citizens-support: true
  mythic-mobs: false
  infernal-mobs: false
  grief-prevention-3d: false
  oraxen: false
  nexo: false
  items-adder: false

  protected-entity-tags: []
```

| Key | Protects |
|---|---|
| `mobs-in-boats` | Any entity currently riding a boat |
| `mobs-in-minecarts` | Any entity currently riding a minecart |
| `leashed-mobs` | Mobs on a lead (the fence knot itself is covered by `LEASH_KNOT` in the `whitelist`) |
| `saddled-mobs` | Mobs wearing a saddle (horses, donkeys, mules, camels, pigs, striders) or a harness (happy ghasts) |
| `ridden-mobs` | Any entity with a player riding it |
| `bucket-mobs` | Fish and axolotls a player released from a bucket |
| `mobs-from-breeding` | The baby **and both parents** whenever animals breed (tracked via a persistent data key). Only breeding that happens while the plugin is installed is tracked; older animals need another protection, e.g. `protect-passive-mobs` |
| `modern-showcase` | Entities registered as showcase items by [ModernShowcase](../integrations/modernshowcase.md) |
| `player-heads` | Dropped `PLAYER_HEAD` items |
| `pets-module` | Entities carrying `Pet`, `isPet`, or `MyPet` metadata (generic pet-plugin compatibility) |
| `citizens-support` | Entities carrying Citizens' `NPC` metadata |
| `mythic-mobs` | Entities with MythicMobs' `MythicMob` metadata — verified against MythicMobs' actual compatibility convention |
| `infernal-mobs` | Entities with InfernalMobs' `infernalMetadata` (plus legacy fallback keys) — verified against the plugin's source |
| `grief-prevention-3d` | Peaceful mobs (animals, villagers, golems, water mobs, etc., excluding hostile ones such as hoglins and shulkers) inside a claim, via [GriefPrevention3D](../integrations/griefprevention3d.md) |
| `oraxen` / `nexo` / `items-adder` | Entities carrying a guessed PersistentDataContainer key from these plugins — **best-effort, not verified** against their current internal formats. Most modern furniture from these plugins uses Display entities anyway, which are already covered by the global `whitelist` below regardless of these toggles. |
| `protected-entity-tags` | Any entity carrying one of these scoreboard tags. `CLE_PROTECTED` is always protected even if it isn't listed here |

Standard (non-3D) GriefPrevention is not covered by a dedicated toggle here.

### Protecting a single entity by hand

Add the `CLE_PROTECTED` scoreboard tag to any entity to protect it from clearing, with no config
change needed. The [Misc Entity Limiter](misc-entity-limiter.md) honours the same tag by default.

```
/tag @e[type=cow,limit=1,sort=nearest] add CLE_PROTECTED
```

### MythicMobs and InfernalMobs mobs

When `mythic-mobs` or `infernal-mobs` is `false`, that plugin's mobs are cleared like normal mobs,
with two differences. Their custom names don't count for `protect-named-entities`, because those
plugins name their own mobs. Type-based rules (`whitelist`, `protect-passive-mobs`,
`whitelist-all-mobs`) don't apply to them either. Protections that show a player owns the mob still
apply: tamed, leashed, ridden, saddled, in a boat or minecart, bred, or tagged. So a player's
tamed MythicMobs pet is kept even with `mythic-mobs: false`.

## Worlds, whitelist, item whitelist

```yaml
worlds: []

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

item-whitelist:
  - "NETHERITE_INGOT"
```

- `worlds` — empty list means all worlds are cleared; populate it to restrict clearing to specific
  world names.
- `whitelist` — entity type names (Bukkit `EntityType`) that are never cleared, regardless of any
  other setting. Villagers, golems, and all Display-entity types are protected by default.
  Entries are matched against exact `EntityType` names, so the plugin logs a warning on startup
  listing any entry it doesn't recognise (a typo such as `FURNANCE_MINECART`, or a mob that doesn't
  exist on your server version). Those entries protect nothing until fixed.
- `item-whitelist` — dropped-item `Material` names that are never cleared even if the `Item`
  entity itself would otherwise be eligible.

See [Materials](../materials.md) for help finding the right enum names.

## Notifications

```yaml
notifications:
  broadcast-times: [60, 30, 10, 5]
  types:
    - "ACTION_BAR"
  console-notifications: false
  sound:
    enabled: true
    name: "BLOCK_NOTE_BLOCK_PLING"
    volume: 1.0
    pitch: 1.2

  clear-complete:
    types:
      - "CHAT"
    console-notifications: false
    sound:
      enabled: true
      name: "ENTITY_PLAYER_LEVELUP"
      volume: 1.0
      pitch: 1.0
```

- `broadcast-times` — seconds-remaining values at which a countdown warning fires (only exact
  matches trigger, so pick values your interval will actually pass through).
- `types` — how the warning is delivered: `ACTION_BAR`, `CHAT`, or both listed together. Applies to
  the countdown warnings by default.
- `console-notifications` — also log the message to console.
- `sound` — plays to every online player when a notification fires. `name` is any
  [Bukkit `Sound`](https://jd.papermc.io/paper/1.20/org/bukkit/Sound.html) enum value.
- **`clear-complete`** — an optional **override block just for the "entities cleared" message**.
  Any field you omit here falls back to the top-level values above. In the default config, the
  clear-complete message is CHAT-only, doesn't log to console, and plays a different sound
  (`ENTITY_PLAYER_LEVELUP`) than the countdown warnings — so you can make "N entities cleared"
  feel distinct from "clearing in 10 seconds" without them sharing settings.

This override applies to **both** the automatic clear cycle and the manual `/lagg clear` command —
running `/lagg clear` plays the configured `clear-complete` sound to you (not broadcast to other
players), on top of the chat confirmation message.

The message text itself lives in `messages.yml` under `notifications.clear-complete` and
`warnings.entity-clear`, and can use the `{prefix}` placeholder to include your configured global
prefix — see [Message Customization](../configuration/message-customization.md).

## Status command

`/lagg clearstatus` (permission `CLE.clearstatus`) shows the module's live state: current
interval, time until next clear, performance-gate status if enabled, and adaptive-interval tier in
effect.
