---
id: message-customization
title: Message Customization
sidebar_position: 3
---

# Message Customization

Every player-facing message lives in `plugins/ClearLaggEnhanced/messages.yml`, formatted with
[MiniMessage](https://docs.advntr.dev/minimessage/format.html) (`<green>`, `<bold>`, `<#hexcode>`,
etc.). Legacy `&`-color codes and `&#hex`/`&x&h&e&x` sequences are also accepted and automatically
converted to MiniMessage on load, so you can paste in messages from older ClearLagg configs
without reformatting them by hand.

## Global prefix

```yaml
prefix: "<gray>[</gray><gold>ClearLagg</gold><gray>]</gray>"
```

Change this to whatever branding you want. It doesn't apply itself automatically to every message
— insert it explicitly with the `{prefix}` placeholder wherever you want it to show:

```yaml
clear-complete: "{prefix} <green>✓</green> <green>Cleared <white>{count}</white> entities in <gray>{time}ms</gray></green>"
```

`{prefix}` currently appears in `notifications.clear-complete` and `warnings.entity-clear` by
default. A few messages elsewhere (the AFK notifications, for example) hardcode their own inline
bracket text instead of referencing the global prefix — feel free to swap those to `{prefix}` too
if you want everything consistent.

Editing `prefix` and running `/lagg reload` updates it everywhere `{prefix}` is used immediately —
no restart needed, since messages are re-parsed from the live config on every send.

## Placeholders

Every message can use `{key}`-style placeholders specific to that message — for example
`{count}`, `{time}`, `{seconds}`, `{x}`, `{z}`, `{player}`. Check the default value of the message
you're editing to see which placeholders it receives; removing a placeholder from your custom text
is fine, but referencing one that doesn't exist for that message just prints the literal `{key}`
text.

If [PlaceholderAPI](../integrations/placeholderapi.md) is installed, its placeholders (`%...%`)
also resolve inside these messages.

## Structure overview

| Section | Covers |
|---|---|
| `notifications.*` | General notices: no-permission, reload results, entity-clearing complete, mob/spawner limiter events, packet limiter, AFK |
| `commands.*` | `/lagg help` text, clear-status output, unknown-subcommand error |
| `warnings.*` | The entity-clearing countdown warning |
| `next-clear.*` | `/lagg next` output |
| `chunkfinder.*` | `/lagg chunkfinder` output |
| `performance.*` | `/lagg tps`/`/lagg ram` output, lag-snapshot notices |
| `gui.*` | Chat-input prompts used by the Admin GUI's editable fields |
| `errors.*` | Generic error text |
| `update-notifier` | The update-available broadcast (a list of lines; the plugin prepends `prefix` to the first line automatically for this one specific message) |

## Per-message notification behavior (Entity Clearing)

The entity-clearing "clear-complete" message additionally has its own **notification-behavior**
override — separate from the message *text* covered here — controlling which channel it's sent
on, whether it logs to console, and what sound plays. That lives in
`module/entity-clearing/config.yml`, not `messages.yml`. See
[Entity Clearing → Notifications](../features-modules/entity-clearing.md#notifications).

## Applying changes

`/lagg reload` after any edit. No restart required.
