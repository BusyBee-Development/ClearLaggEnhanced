---
id: admin-gui
title: Admin GUI
sidebar_position: 2
---

# Admin GUI

`/lagg admin` (permission `CLE.admin`, default: op) opens an in-game inventory menu listing every
registered module. The menu size grows automatically to fit however many modules you have enabled
— it's not a fixed layout.

## Using it

- **Left-click** a module icon to open that module's own configuration GUI (sliders/toggles for
  its settings, editable without touching YAML).
- **Right-click** a module icon to toggle it enabled/disabled on the spot. This writes straight to
  the `modules:` block in `config.yml` — no reload needed, and no config file editing required.
- A green name means the module is enabled; red means disabled. If a module's required dependency
  (e.g. ProtocolLib for Packet Limiter, or a stacker plugin for the stacker integrations) isn't
  installed, its icon shows a **"Dependency missing"** warning in the lore and the integration
  won't function even if toggled on.
- The **Reload Config** button (bottom of the menu) runs the same reload as `/lagg reload`.

## What's inside

Every module gets its own icon: Entity Clearing, Mob Limiter, Spawner Limiter, Misc Entity
Limiter, Chunk Finder, Performance, Packet Limiter, AFK Optimization, plus an **Integrations**
entry (sorted last) that opens the [Integrations sub-menu](#integrations-sub-menu).

Per-module GUIs let you edit that module's core numbers in-game — for example, the Entity Clearing
GUI lets you toggle the clear interval, adaptive-interval settings, and the named/tamed/stacked
protection flags by clicking an item and typing a new value in chat when prompted.

## Integrations sub-menu

A dedicated menu listing the four integrations with real dedicated hooks: **RoseStacker**,
**WildStacker**, **ModernShowcase**, and **GriefPrevention3D**. Click an icon to toggle that
integration on/off. Same dependency-missing warning behavior applies here — toggling an
integration on doesn't do anything until the target plugin is actually installed and enabled.

See [Stacker Plugins](../integrations/stacker-plugins.md), [ModernShowcase](../integrations/modernshowcase.md),
and [GriefPrevention3D](../integrations/griefprevention3d.md) for what each integration actually does.
