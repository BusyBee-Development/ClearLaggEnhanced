---
id: troubleshooting
title: Troubleshooting
sidebar_position: 4
---

# Troubleshooting

## "Modules and the database are inactive" after startup

If console logs `Failed to initialize core services` followed by a note that modules/database are
inactive until `/lagg reload` succeeds, this means startup hit a bad config value — most commonly
an invalid `database.*` setting in `config.yml` (bad MySQL host/credentials, an out-of-range pool
size, etc. — see [Database Setup](../configuration/database-setup.md)'s validation rules). The
plugin deliberately does **not** crash on this; commands stay available so you can fix the config
without restarting. Fix the offending value and run `/lagg reload`.

## An integration shows "Dependency missing" in the Admin GUI

The target plugin isn't installed, isn't enabled, or (for
[GriefPrevention3D](../integrations/griefprevention3d.md) specifically) doesn't expose the API this
integration hooks into via reflection. Toggling the integration on in ClearLaggEnhanced doesn't
make it function until the actual dependency is present and enabled.

## Packet Limiter doesn't do anything

Confirm [ProtocolLib](https://www.spigotmc.org/resources/protocollib.1997/) is installed — Packet
Limiter is a hard requirement for it, not optional. Also confirm `modules.packet-limiter: true` in
`config.yml` (it's `false` by default).

## An admin isn't getting Packet Limiter violation notifications

Packet Limiter uses its own `clearlag.admin` permission node, **not** `CLE.admin` — this is a known
inconsistency in the current permission scheme. Grant `clearlag.admin` explicitly. See
[Commands & Permissions](../core-reference/commands-permissions.md).

## An entity I expected to be protected got cleared anyway

Work through this order:

1. Check whether it matches `whitelist` (entity type) or `item-whitelist` (dropped item material)
   in `module/entity-clearing/config.yml` — see [Entity Clearing](../features-modules/entity-clearing.md)
   and [Materials](../materials.md) for exact naming.
2. Check the relevant `protect-*` and `extra-protections.*` toggle is actually `true`.
3. If you're relying on `extra-protections.oraxen` / `.nexo` / `.items-adder`: these are
   **best-effort, unverified** detection against a guessed internal key for those plugins, and may
   simply not match your installed version. Most modern furniture from these plugins uses Display
   entities anyway, which are already covered by the global `whitelist`, independent of these
   toggles — check whether the entity is a Display entity first.
4. If you're relying on `extra-protections.mythic-mobs` or `.infernal-mobs`: these ARE verified
   against those plugins' actual metadata conventions, so if protection isn't working, double-check
   the toggle is `true` and the mob was actually spawned by that plugin (not a vanilla mob that
   happens to look similar).
5. `protect-stacked-entities` only works if a [stacker plugin integration](../integrations/stacker-plugins.md)
   is installed, enabled, and actually reports the entity as stacked (stack size > 1) at clear time.

## The MSPT performance gate isn't blocking clears

If you're on Folia, this is expected — see [Folia Support](folia-support.md#the-one-thing-that-doesnt-work-on-folia).
On regular Paper/Spigot/Purpur, check `/lagg clearstatus` for the gate's live MSPT reading against
your configured threshold and sustained-window progress.

## Still stuck

See [Getting Help](../support/getting-help.md).
