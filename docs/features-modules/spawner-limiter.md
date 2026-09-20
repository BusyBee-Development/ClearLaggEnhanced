---
id: spawner-limiter
title: Spawner Limiter
sidebar_position: 3
---

# Spawner Limiter

Slows down mob spawner activation rates to reduce the entity-spawn load spawner farms put on a
server. Enabled by default. Config file: `module/spawner-limiter/config.yml`.

```yaml
spawn-delay-multiplier: 1.5

worlds: []
```

- `spawn-delay-multiplier` — multiplies the vanilla spawner delay. `1.5` means spawners take 50%
  longer between activations than vanilla default; `1.0` is vanilla behavior; values below `1.0`
  would speed spawners up (not recommended for a lag-prevention plugin, but supported).
- `worlds` — empty list applies the multiplier server-wide; populate it to restrict the limiter to
  specific world names, leaving spawners in unlisted worlds at vanilla rates.

This module works alongside — not instead of — [Mob Limiter](mob-limiter.md)'s per-chunk caps;
spawner output is still subject to those limits regardless of the delay multiplier here.
