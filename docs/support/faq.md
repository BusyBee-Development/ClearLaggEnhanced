---
id: faq
title: FAQ
sidebar_position: 1
---

# FAQ

**Does this work on Spigot, or only Paper?**
Paper, Spigot, Purpur, and Folia are all supported. See [Requirements](../getting-started/requirements.md).

**Do I need to restart the server after editing a config?**
No — `/lagg reload` (permission `CLE.reload`) applies config changes without restarting.

**Will updating the plugin wipe my config?**
No. New keys are merged in automatically, your existing values are preserved, and a timestamped
backup is saved first. See [Installation → Updating](../getting-started/installation.md#updating).

**Can I run this alongside another entity-clearing plugin?**
Not recommended — see [Migration Guide](../advanced-optimization/migration-guide.md). Two plugins
independently clearing the same entities is redundant and can produce confusing double-counted
behavior.

**Does it migrate my config from the original ClearLagg or another fork?**
No automatic migration exists. See [Migration Guide](../advanced-optimization/migration-guide.md)
for a manual checklist.

**Why isn't Oraxen/Nexo/ItemsAdder protection working reliably?**
Those three specifically are best-effort/unverified detection — see the note in
[Entity Clearing](../features-modules/entity-clearing.md#extra-protections) and
[Troubleshooting](../advanced-optimization/troubleshooting.md#an-entity-i-expected-to-be-protected-got-cleared-anyway).

**Does the performance gate work on Folia?**
No — see [Folia Support](../advanced-optimization/folia-support.md). Use `adaptive-interval`
instead on Folia.

**Do I need MySQL?**
No, SQLite is the default and requires no setup. See [Database Setup](../configuration/database-setup.md)
for when MySQL is worth switching to.

**Is this plugin open source?**
No — the source is publicly viewable on [GitHub](https://github.com/BusyBee-Development/ClearLaggEnhanced),
but it's licensed "All Rights Reserved" by BusyBee Development, not under an open-source license.
Redistribution, modification for public distribution, and reverse engineering all require prior
written consent. See the repository's `LICENSE` file for exact terms.
