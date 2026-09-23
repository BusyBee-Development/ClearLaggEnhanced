---
id: common-issues
title: Common Issues
sidebar_position: 2
---

# Common Issues

This page covers issues distinct from step-by-step debugging — for a diagnostic walkthrough, see
[Troubleshooting](../advanced-optimization/troubleshooting.md).

## "Message not found: <path>" appears in chat/console

`messages.yml` is missing a key the plugin expected — most likely from a hand-edit that deleted a
line rather than changed its value. Delete the affected key entirely (not just its value) and run
`/lagg reload`; the migrator will re-add it from the default with its default text. See
[Message Customization](../configuration/message-customization.md).

## A module I enabled in the Admin GUI didn't actually turn on

Right-clicking a module icon writes to `modules:` in `config.yml` immediately and takes effect
without a reload — but if the module has a required dependency (ProtocolLib for Packet Limiter, a
stacker plugin for the stacker integrations, etc.) that isn't installed, the module is toggled
"on" in config but the Admin GUI will still show it as unavailable via the dependency-missing
warning in its lore. Install the dependency first.

## Config values reverted after an update

They shouldn't — the migrator preserves every value you've set and only adds genuinely new keys.
If something looks reverted, check the `backups/` subfolder next to the affected config file; a
timestamped pre-migration backup is saved on every migration, so you can diff against it to see
exactly what changed.

## Sound/action bar/chat notifications aren't behaving how I configured them

Check whether you're editing the right block. The entity-clearing countdown warnings and the
"entities cleared" message can have **different** notification behavior — the latter has its own
optional override block (`notifications.clear-complete` in `module/entity-clearing/config.yml`)
that falls back to the top-level `notifications.*` settings for any field you don't override. See
[Entity Clearing → Notifications](../features-modules/entity-clearing.md#notifications).

## "Timed out while waiting for loaded chunks list from the main thread" every few minutes

This appeared on older versions when the server used vanilla's `pause-when-empty-seconds`
(1.21.2+, kept by Paper and forks such as Leaf). Once the server paused with nobody online, the
clear countdown kept running, and each clear timed out waiting on the paused server. It was
harmless but noisy. Current versions hold the countdown while the server is paused, so update the
plugin. Setting `pause-when-empty-seconds=-1` in `server.properties` also stops it, but isn't
needed after updating.

## Prefix isn't showing on a message

The global `prefix` in `messages.yml` isn't auto-prepended to every message — it only appears
where a message explicitly includes the `{prefix}` placeholder. See
[Message Customization](../configuration/message-customization.md).
