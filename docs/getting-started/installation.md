---
id: installation
title: Installation
sidebar_position: 2
---

# Installation

1. Check the [Requirements](requirements.md) — Java 21+, Paper/Spigot/Purpur/Folia 1.20+.
2. Download the latest jar from [Modrinth](https://modrinth.com/plugin/clearlaggenhanced).
3. Drop the jar into your server's `plugins/` folder.
4. Start (or restart) the server. On first run, ClearLaggEnhanced generates its full config
   structure under `plugins/ClearLaggEnhanced/`:
   - `config.yml` — module enable/disable toggles and database settings.
   - `messages.yml` — every player-facing message, plus the global `prefix`.
   - `module/<module-name>/config.yml` — one file per module with that module's own settings.
5. Edit whichever config files you need (see [Configuration](../configuration/main-configuration.md)).
6. Apply changes without restarting: `/lagg reload` (permission `CLE.reload`).

## Verifying it's working

Run `/lagg help` in-game or console. You should see the full subcommand list. Then:

- `/lagg tps` — confirms the Performance module is reading live server stats.
- `/lagg admin` — opens the [Admin GUI](../core-reference/admin-gui.md), which lists every module
  and its current enabled/disabled state.

## Updating

Replace the jar with the new version and restart. Every config file (`config.yml`, `messages.yml`,
and each module's `config.yml`) goes through an automatic migrator on load: it keeps every value
you've already set, adds any new keys the update introduced (with their default values and
comments), and drops a timestamped backup in a `backups/` subfolder next to the file before
touching anything. You don't need to delete and regenerate your configs on update.

## Uninstalling

Remove the jar from `plugins/` and restart. Your `plugins/ClearLaggEnhanced/` data folder (configs
and the SQLite/MySQL database) is left on disk — delete it manually if you want a completely clean
removal.
