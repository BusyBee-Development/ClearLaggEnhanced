---
id: commands-permissions
title: Commands & Permissions
sidebar_position: 1
---

# Commands & Permissions

Everything runs under a single root command, `/lagg`, with subcommands. No separate commands are
registered per module.

**Aliases:** `/clearlagg`, `/clearlag`, `/cl`, `/cle` — all equivalent to `/lagg`.

## Commands

| Command | Description | Permission | Default |
|---|---|---|---|
| `/lagg help` | Display the help menu | `CLE.help` | everyone |
| `/lagg clear` | Manually clear entities now | `CLE.clear` | op |
| `/lagg clearstatus` | View detailed entity clearing module status | `CLE.clearstatus` | op |
| `/lagg next` | Show time until the next automatic clear | `CLE.next` | everyone |
| `/lagg tps` | Display current server TPS | `CLE.tps` | op |
| `/lagg ram` | Display detailed server memory usage statistics | `CLE.ram` | op |
| `/lagg chunkfinder` | Locate laggy chunks with high entity counts | `CLE.chunkfinder` | op |
| `/lagg admin` | Open the [Admin GUI](admin-gui.md) | `CLE.admin` | op |
| `/lagg reload` | Reload plugin configuration | `CLE.reload` | op |

`CLE.help` and `CLE.next` default to **everyone**, not just op — those two are considered safe for
regular players to check.

## Permission nodes

All permissions live under the `CLE.*` namespace **except** the Packet Limiter module, which uses
its own lowercase `clearlag.*` nodes (see the callout below).

```yaml
permissions:
  CLE.*:
    description: All ClearLaggEnhanced permissions
    children:
      CLE.help: true
      CLE.clear: true
      CLE.clearstatus: true
      CLE.next: true
      CLE.tps: true
      CLE.ram: true
      CLE.chunkfinder: true
      CLE.admin: true
      CLE.reload: true
      CLE.performance.snapshot: true
```

| Node | Grants |
|---|---|
| `CLE.*` | Every permission below |
| `CLE.help` | `/lagg help` (default: everyone) |
| `CLE.clear` | `/lagg clear` |
| `CLE.clearstatus` | `/lagg clearstatus` |
| `CLE.next` | `/lagg next` (default: everyone) |
| `CLE.tps` | `/lagg tps` |
| `CLE.ram` | `/lagg ram` |
| `CLE.chunkfinder` | `/lagg chunkfinder` |
| `CLE.admin` | `/lagg admin`, opening the Admin GUI |
| `CLE.reload` | `/lagg reload` |
| `CLE.performance.snapshot` | Receive notifications when a performance snapshot is triggered and saved |

## ⚠️ Packet Limiter uses different permission nodes

The Packet Limiter module (see [Packet Limiter](../features-modules/packet-limiter.md)) checks
`clearlag.admin` (for violation notifications) and `clearlag.packetlimit.bypass` (to exempt a
player from rate limiting) — **not** `CLE.admin` or any other `CLE.*` node. This is a known
inconsistency in the current permission scheme; if you rely on Packet Limiter, grant
`clearlag.admin`/`clearlag.packetlimit.bypass` explicitly rather than assuming `CLE.*` covers it.

## Misc Entity Limiter admin notifications

The Misc Entity Limiter module's throttled admin notifications use whatever permission is
configured in that module's `notify.admins-permission` key (default: `CLE.admin`) — see
[Misc Entity Limiter](../features-modules/misc-entity-limiter.md).
