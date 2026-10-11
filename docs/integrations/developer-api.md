---
id: developer-api
title: Developer API
sidebar_position: 6
---

Other plugins can hook into ClearLaggEnhanced to keep their own entities out of a clear, react to
clears, or start one. Everything lives in the `net.busybee.clearlaggenhanced.api` package. That
package is the only part of the jar that is API; the rest can change between versions.

## Setup

Add ClearLaggEnhanced to your `plugin.yml` so it loads first:

```yaml
softdepend: [ClearLaggEnhanced]
```

Compile against the plugin jar and do not shade it. With Maven:

```xml
<repository>
    <id>busybee</id>
    <url>https://repo.busybeedev.net/releases</url>
</repository>

<dependency>
    <groupId>net.busybee.clearlaggenhanced</groupId>
    <artifactId>ClearLaggEnhanced</artifactId>
    <version>VERSION</version>
    <scope>provided</scope>
    <exclusions>
        <exclusion>
            <groupId>*</groupId>
            <artifactId>*</artifactId>
        </exclusion>
    </exclusions>
</dependency>
```

With Gradle:

```kotlin
repositories {
    maven("https://repo.busybeedev.net/releases")
}

dependencies {
    compileOnly("net.busybee.clearlaggenhanced:ClearLaggEnhanced:VERSION") { isTransitive = false }
}
```

Replace `VERSION` with a version listed in the
[BusyBee repository](https://repo.busybeedev.net/#/releases/net/busybee/clearlaggenhanced/ClearLaggEnhanced)
(26.10.1 and later).

Get the API from `onEnable` or later:

```java
ClearLaggEnhancedAPI api = ClearLaggEnhancedAPI.get();
```

`get()` throws `IllegalStateException` when ClearLaggEnhanced is not enabled. With a soft
dependency, check `Bukkit.getPluginManager().isPluginEnabled("ClearLaggEnhanced")` first and keep
the API calls in their own class. The instance stays valid across `/lagg reload`.

## Protecting your entities

Register an `EntityProtection` and return `true` for anything that belongs to you:

```java
NamespacedKey key = new NamespacedKey(this, "pet");

ClearLaggEnhancedAPI.get().registerProtection(this, entity ->
        entity.getPersistentDataContainer().has(key, PersistentDataType.BYTE));
```

A protected entity is kept by [Entity Clearing](../features-modules/entity-clearing.md) (automatic
clears and `/lagg clear`) and by the
[Misc Entity Limiter](../features-modules/misc-entity-limiter.md) sweep. Like every other protected
entity, it also stops counting towards the [Mob Limiter](../features-modules/mob-limiter.md).

- The check runs for every candidate entity on the thread that owns it (on Folia, its region
  thread). Keep it cheap, never block, and only touch the entity you are given.
- If the check throws, the entity is kept and one warning is logged.
- Registrations are removed when your plugin is disabled. `unregisterProtection` and
  `unregisterProtections(plugin)` remove them earlier.

No code needed for simple cases: Entity Clearing always keeps an entity that carries the
`CLE_PROTECTED` scoreboard tag.

`api.isProtected(entity)` tells you whether a clear would keep an entity, counting the server
owner's protection settings and whitelist as well as every registered check. Call it on the thread
that owns the entity.

## Events

Both events are fired off the main thread.

| Event | When | Cancellable |
|---|---|---|
| `EntityClearEvent` | Before a clear removes anything | Yes |
| `EntityClearCompleteEvent` | After a clear has finished | No |

```java
@EventHandler
public void onClear(EntityClearEvent event) {
    if (event.getCause() == ClearCause.AUTOMATIC && bossFightRunning()) {
        event.setCancelled(true);
    }
}

@EventHandler
public void onClearComplete(EntityClearCompleteEvent event) {
    ClearResult result = event.getResult();
    getLogger().info("Cleared " + result.cleared() + " entities in " + result.durationMillis() + "ms");
}
```

`getCause()` is `AUTOMATIC` (the clear timer), `COMMAND` (`/lagg clear`) or `API`. A cancelled
automatic clear waits for its next interval; its countdown warnings have already been broadcast by
then. `EntityClearCompleteEvent` is not fired for a cancelled clear.

## Starting a clear

```java
ClearLaggEnhancedAPI.get().clearEntities().thenAccept(result -> {
    if (result.completed()) {
        getLogger().info("Cleared " + result.cleared() + ", kept " + result.skipped());
    }
});
```

This runs the same clear as `/lagg clear` without broadcasting the result. It is safe to call from
any thread, and the future completes off the main thread, so schedule back before touching the
world. `ClearResult` carries `cleared`, `skipped`, `chunks` and `durationMillis`, plus a status:

| Status | Meaning |
|---|---|
| `COMPLETED` | The clear ran |
| `ALREADY_RUNNING` | Another clear was still running |
| `CANCELLED` | A plugin cancelled the `EntityClearEvent` |
| `UNAVAILABLE` | The Entity Clearing module is off |

## Other methods

| Method | Returns |
|---|---|
| `getSecondsUntilNextClear()` | Seconds until the next automatic clear, or `-1` when automatic clearing is not running |
| `isModuleEnabled(String)` | Whether a module is on, by folder name such as `entity-clearing` or `mob-limiter` |
