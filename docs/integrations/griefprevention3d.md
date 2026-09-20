---
id: griefprevention3d
title: GriefPrevention3D
sidebar_position: 4
---

# GriefPrevention3D

Protects peaceful mobs standing inside a player's claim from
[Entity Clearing](../features-modules/entity-clearing.md). Off by default
(`modules.griefprevention3d` in `config.yml`).

## What it detects as present

The integration activates if a plugin registered under the name **`GriefPrevention3D`** or the
standard **`GriefPrevention`** is installed and enabled — either name satisfies it. It then hooks
into the claim-lookup API via reflection against GriefPrevention's own classes
(`me.ryanhamshire.GriefPrevention.*`). In practice this means it works against the standard
GriefPrevention plugin (and any fork that kept that same package structure); if you're running a
claims plugin that happens to also be named "GriefPrevention3D" but uses a genuinely different
internal API, confirm the [Admin GUI](../core-reference/admin-gui.md) shows the integration as
**enabled** (not just "dependency present") before relying on it — the Integrations panel will flag
a missing/incompatible dependency in its lore.

## What "protected" means

When enabled, entities are protected from clearing if they're:

- A "peaceful" type — `Animals`, `WaterMob`, `AbstractVillager`, `Golem`, `Allay`, `Ambient`, or
  `NPC` — **and**
- Standing inside a claim at the time of the clear.

Hostile mobs inside claims are **not** protected by this integration — it's specifically scoped to
peaceful/farm-adjacent mobs players are likely to have intentionally placed or bred inside their
base.

Controlled by `extra-protections.grief-prevention-3d` in
`module/entity-clearing/config.yml` (default `false`) plus the module toggle itself.
