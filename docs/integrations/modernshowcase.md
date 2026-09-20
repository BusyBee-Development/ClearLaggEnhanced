---
id: modernshowcase
title: ModernShowcase
sidebar_position: 3
---

# ModernShowcase

If [ModernShowcase](https://modrinth.com/plugin/modernshowcase) is installed and the
`modernshowcase` module is enabled in `config.yml`, ClearLaggEnhanced protects its showcase
entities from being swept up by [Entity Clearing](../features-modules/entity-clearing.md).

## How it works

Detection is entity-metadata based: any entity carrying ModernShowcase's own `ModernShowcase`
metadata key is recognized as a showcase entity. This only kicks in when
`extra-protections.modern-showcase: true` in `module/entity-clearing/config.yml` (the default), and
only while the ModernShowcase plugin is actually installed and enabled.

Toggle the integration itself from `config.yml`'s `modules.modernshowcase` key or the Integrations
panel in the [Admin GUI](../core-reference/admin-gui.md).
