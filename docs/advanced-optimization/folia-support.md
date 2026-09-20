---
id: folia-support
title: Folia Support
sidebar_position: 2
---

# Folia Support

ClearLaggEnhanced runs natively on [Folia](https://github.com/PaperMC/Folia) — this isn't a
compatibility shim bolted on top of a Bukkit-scheduler plugin. All internal scheduling goes through
Paper's native region-based scheduler API
(`io.papermc.paper.threadedregions.scheduler.*`), which has shipped in `paper-api` since 1.20.1 and
runs correctly on regular Paper, Folia, and Folia forks (e.g. Canvas) alike. `folia-supported:
true` is declared in `plugin.yml`, and the plugin detects Folia at runtime by checking for the
presence of `io.papermc.paper.threadedregions.RegionizedServer`.

In practice, that means: global/repeating tasks run on the global region scheduler, per-location
work runs on the region scheduler for that location, and per-entity work runs on that entity's own
scheduler — the correct target for each kind of work rather than a single global scheduler that
would be wrong on Folia.

## The one thing that doesn't work on Folia

[Entity Clearing](../features-modules/entity-clearing.md)'s **MSPT performance gate**
(`performance-gate.enabled` in `module/entity-clearing/config.yml`) is **not supported on Folia**.
Folia's regionized architecture doesn't have a single server-wide MSPT figure the way a normal
Paper server does — each region ticks independently — so there's no single number for the gate to
compare against a threshold.

If you enable it on Folia anyway, `/lagg clearstatus` will report the performance gate as
unavailable rather than silently pretending to work. Use `adaptive-interval` instead if you want
clear frequency to respond to server load on Folia — it works identically there since it's based
on entity/player counts, not MSPT.

## Everything else

Every other module — Mob Limiter, Spawner Limiter, Misc Entity Limiter, Chunk Finder, Performance
Monitor, Packet Limiter, AFK Optimization — functions the same on Folia as on regular Paper.
[AFK Optimization](../features-modules/afk-optimization.md) specifically requires Paper or Folia
(simulation distance isn't a vanilla Spigot API), so Folia servers get full support there too.
