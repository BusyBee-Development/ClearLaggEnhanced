---
id: afk-optimization
title: AFK Optimization
sidebar_position: 8
---

# AFK Optimization

Reduces a player's **simulation distance** while they're AFK, cutting the CPU cost of simulating
chunks around an inactive player. **Disabled by default.** Requires Paper or Folia (simulation
distance is a Paper API concept, not available on vanilla Spigot). Config file: `module/afk/config.yml`.

```yaml
# Time in seconds of inactivity before a player is considered AFK.
afk-threshold: 300

# Simulation distance for AFK players (number of chunks).
# Lower values save more resources.
afk-simulation-distance: 2

# Restore the player's simulation distance to the server default when they return.
restore-default-distance: true

# Should a message be sent to the player when their simulation distance is adjusted?
notify-player: false
```

- `afk-threshold` — seconds of no movement, chat, interaction, or command use before a player is
  considered AFK. Activity is tracked per-player and resets the timer on movement (block-level, not
  every micro-movement), chat messages, block/entity interaction, and command use.
- `afk-simulation-distance` — the reduced simulation distance applied once a player crosses the
  threshold. Only lowers it if the player's current simulation distance is actually higher than
  this value (won't raise it).
- `restore-default-distance` — restores the player's original simulation distance the moment
  they're active again.
- `notify-player` — sends `notifications.afk.distance-lowered` / `notifications.afk.distance-restored`
  (see [Message Customization](../configuration/message-customization.md)) when the change happens.

The check runs every 5 seconds (100 ticks) across all online players, not continuously — expect up
to a 5-second delay between crossing the threshold and the simulation distance actually dropping.
