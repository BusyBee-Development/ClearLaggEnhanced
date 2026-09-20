---
id: packet-limiter
title: Packet Limiter
sidebar_position: 7
---

# Packet Limiter

Protects the server from packet spam and crasher clients by rate-limiting how many packets a
player can send per second. **Disabled by default** and **requires [ProtocolLib](https://www.spigotmc.org/resources/protocollib.1997/)** —
enabling it without ProtocolLib installed won't do anything (the Admin GUI will flag it with a
"Dependency missing" warning). Config file: `module/packet-limiter/config.yml`.

```yaml
# Maximum packets per second a player can send before being flagged.
max-packets-per-second: 150

# How many violations (seconds exceeding max-pps) before the player is kicked.
kick-threshold: 10

# Duration in seconds to block a player from sending packets after a minor violation.
block-duration: 5

# Should admins with 'clearlag.admin' permission be notified of violations?
notify-admins: true

# Should the player be notified when they are being throttled?
notify-player: true

# Packet types to monitor (standard set for movement/interaction)
# Do NOT change unless you know what you are doing.
monitored-packets:
  - "POSITION"
  - "POSITION_LOOK"
  - "LOOK"
  - "ARM_ANIMATION"
  - "BLOCK_DIG"
  - "BLOCK_PLACE"
  - "USE_ENTITY"
  - "USE_ITEM"
  - "WINDOW_CLICK"
  - "ENTITY_ACTION"
  - "HELD_ITEM_SLOT"
  - "VEHICLE_MOVE"
  - "STEER_VEHICLE"
  - "ABILITIES"
  - "SETTINGS"
  - "TAB_COMPLETE"
  - "CHAT"
  - "KEEP_ALIVE"
```

- `max-packets-per-second` — threshold above which a second counts as a violation.
- `kick-threshold` — consecutive/accumulated violations before the player is kicked outright.
- `block-duration` — after a violation, the player's packets are dropped (not processed) for this
  many seconds rather than immediately escalating to a kick.
- `notify-admins` / `notify-player` — who gets told when a violation happens.
- `monitored-packets` — the ~18 packet categories being rate-counted. Leave this alone unless you
  specifically understand which packet types you want to exempt or add.

## ⚠️ Permission nodes

Unlike every other module, Packet Limiter does **not** use the `CLE.*` permission namespace:

- `clearlag.admin` — required to receive violation notifications (independent of `CLE.admin`).
- `clearlag.packetlimit.bypass` — exempts a player from rate limiting entirely.

Grant these explicitly if you use this module — see the callout in
[Commands & Permissions](../core-reference/commands-permissions.md).
