---
id: performance-guide
title: Performance Guide
sidebar_position: 1
---

# Performance Guide

Practical tuning starting points. None of this is a substitute for watching your own server's
`/lagg tps` and `/lagg ram` output after changing anything — these are starting points, not rules.

## Small survival server (few players, modest builds)

- [Entity Clearing](../features-modules/entity-clearing.md): defaults (`interval: 300`) are
  usually fine. Leave `adaptive-interval` and `performance-gate` disabled — there's rarely enough
  entity churn for them to matter.
- [Mob Limiter](../features-modules/mob-limiter.md) / [Spawner Limiter](../features-modules/spawner-limiter.md):
  defaults are conservative enough to not need touching.
- Leave [Packet Limiter](../features-modules/packet-limiter.md) and
  [AFK Optimization](../features-modules/afk-optimization.md) disabled unless you have a specific
  problem they solve.

## Larger survival / semi-vanilla server (farms, redstone, many players)

- Turn on `adaptive-interval` in Entity Clearing with `metric: "ENTITY_COUNT"` so clear frequency
  scales with actual load instead of a fixed timer — see the worked example in
  [Entity Clearing](../features-modules/entity-clearing.md#adaptive-interval-scheduling).
- Tighten [Mob Limiter](../features-modules/mob-limiter.md)'s `per-type-limits` for whatever mobs
  your farms produce in bulk (zombies, skeletons, etc.) rather than relying only on the global cap.
- Turn on [AFK Optimization](../features-modules/afk-optimization.md) if you have players who
  routinely leave clients open AFK (auto-farms, fishing bots, etc.) — this alone can meaningfully
  cut simulation load with zero gameplay downside for genuinely inactive players.
- If you're getting reports of item/mob dupers or crasher clients, enable
  [Packet Limiter](../features-modules/packet-limiter.md) (requires ProtocolLib).

## Network / high player count

- Consider **MySQL** over SQLite if you're centralizing multiple servers — see
  [Database Setup](../configuration/database-setup.md). (Note: as of this writing no built-in
  module actually persists shared state through the database yet — see that page's caveat — so
  this is a forward-looking choice, not something with an immediate payoff.)
- Enable the `performance-gate` on Entity Clearing (non-Folia servers only — see
  [Folia Support](folia-support.md)) so clears don't fire on a fixed schedule when the server is
  already healthy, and instead fire when MSPT is actually elevated.
- Use `worlds:` scoping (available on Entity Clearing, Spawner Limiter, and Misc Entity Limiter) to
  exclude worlds that don't need aggressive limiting — a creative/build world usually doesn't need
  the same entity pressure as a survival world.

## Diagnosing an active lag spike

1. `/lagg tps` and `/lagg ram` for the immediate numbers.
2. `/lagg chunkfinder` to find which chunks are carrying unusually high entity counts — see
   [Chunk Finder](../features-modules/chunk-finder.md).
3. `/lagg clearstatus` to check whether Entity Clearing's performance gate (if enabled) is
   currently deferring clears, and how long until the next one fires regardless.
