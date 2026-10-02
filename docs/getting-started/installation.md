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

The new keys are inserted into your file as text, so everything you wrote stays exactly as it was:
your values, quotes, comments, spacing and key order. A file with nothing new to add is not
touched and gets no backup. Changing a setting from the Admin GUI works the same way and rewrites
only that one setting.

Entries you delete from a list of your own are not put back. That covers `limits-per-chunk` in the
Misc Entity Limiter, `per-type-limits.limits` in the Mob Limiter, and the optional
`notifications.clear-complete` override in Entity Clearing.

### If a file has a YAML mistake

A file the plugin can't parse is never rewritten, replaced or reset, and one mistake doesn't switch
anything off. Each time a file loads cleanly (or you change a setting from the Admin GUI) the
plugin keeps a copy of it as `backups/<file>.last-good` next to the file. If the file later has a
mistake, the plugin carries on with that copy: everything keeps running on your own settings as
they were before the bad edit.

The console shows which file it is and the line of the mistake, and `/lagg reload` tells whoever
ran it. Until you fix the file and run `/lagg reload`, edits you made to it since the last working
copy are not in effect.

Because that could include a whitelist entry you were adding when the mistake went in, nothing is
removed automatically while one of these files is broken:

- Entity Clearing's `config.yml`: automatic clears are on hold. `/lagg clear` still works.
- Entity Clearing's or the Misc Entity Limiter's `config.yml`: the Misc Entity Limiter sweep is on
  hold. Its caps still block new placements.

The one case with nothing to fall back on is a file that was already broken the first time this
version loaded it. Then the default settings are used, except for Entity Clearing and the Misc
Entity Limiter, which stay off until their file is fixed: their defaults could remove things your
file protects.

## Uninstalling

Remove the jar from `plugins/` and restart. Your `plugins/ClearLaggEnhanced/` data folder (configs
and the SQLite/MySQL database) is left on disk — delete it manually if you want a completely clean
removal.
