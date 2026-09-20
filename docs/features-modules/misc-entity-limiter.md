---
id: misc-entity-limiter
title: Misc Entity Limiter
sidebar_position: 4
---

# Misc Entity Limiter

Caps non-mob entities per chunk — armor stands, boats, minecarts, item frames, paintings, leash
hitches — the kind of clutter that piles up around builds and farms without being a "mob" at all.
Enabled by default. Config file: `module/misc-entity-limiter/config.yml`.

```yaml
limits-per-chunk:
  ARMOR_STAND: -1
  BOAT: 5
  CHEST_BOAT: 5
  MINECART: 5
  ITEM_FRAME: -1
  GLOW_ITEM_FRAME: -1
  LEASH_HITCH: -1
  PAINTING: 5

protect:
  named: true
  citizens: true
  tags:
    - "CLE_PROTECTED"

sweep:
  interval-ticks: 100
  max-chunks-per-tick: 20

notify:
  admins-permission: "CLE.admin"
  throttle-seconds: 60

worlds: []
```

- `limits-per-chunk` — per-entity-type cap. `-1` means unlimited (armor stands, item frames, glow
  item frames, and leash hitches are unlimited by default since they're commonly used decoratively
  in large numbers); boats, chest boats, minecarts, and paintings default to a cap of 5 per chunk.
- `protect.named` — skip named entities the same way Entity Clearing does.
- `protect.citizens` — skip Citizens NPC entities.
- `protect.tags` — scoreboard tags that exempt an entity from limiting.
- `sweep.interval-ticks` / `sweep.max-chunks-per-tick` — the enforcement sweep runs in
  tick-batched passes (not all at once) to avoid a single-tick stall on large worlds; tune
  `max-chunks-per-tick` down on bigger servers if you see hitching during sweeps.
- `notify.admins-permission` — permission node required to receive admin notifications when this
  module removes entities (default `CLE.admin`; this is a separate config-driven permission check,
  distinct from any hardcoded node).
- `notify.throttle-seconds` — minimum gap between admin notifications, to avoid spamming ops during
  a busy sweep.
- `worlds` — empty means all worlds; populate to scope the limiter to specific world names.
