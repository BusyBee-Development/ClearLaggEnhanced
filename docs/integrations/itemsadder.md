---
id: itemsadder
title: ItemsAdder
sidebar_position: 5
---

If [ItemsAdder](https://itemsadder.devs.beer/) is installed, ClearLaggEnhanced can keep its
furniture and custom entities out of [Entity Clearing](../features-modules/entity-clearing.md) and
the [Misc Entity Limiter](../features-modules/misc-entity-limiter.md) sweep. The sweep only honours
it while the Entity Clearing module is enabled.

## Turning it on

The protection is off by default. Enable it in `module/entity-clearing/config.yml`:

```yaml
extra-protections:
  items-adder: true
```

Then run `/lagg reload`. There is no separate module toggle in `config.yml`.

## How it works

For each entity a clear would remove, ClearLaggEnhanced asks ItemsAdder's own API whether the
entity is a piece of furniture or a custom entity, and keeps it if so. Nothing is guessed from
entity data, so it follows whatever your installed ItemsAdder version reports.

- It only applies while ItemsAdder is installed and enabled. With the toggle on and no ItemsAdder,
  nothing changes.
- If ItemsAdder cannot answer for an entity, that entity is kept and one warning is logged.
- If the installed ItemsAdder version is not compatible with the hook, the protection switches
  itself off and says so in the console. Entity clearing keeps running.

Like every other protected entity, ItemsAdder entities also stop counting towards the
[Mob Limiter](../features-modules/mob-limiter.md).
