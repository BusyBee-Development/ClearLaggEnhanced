---
id: main-configuration
title: Main Configuration
sidebar_position: 1
---

# Main Configuration

`plugins/ClearLaggEnhanced/config.yml` controls two things: the database connection, and which
modules are active. Everything else lives in per-module config files — see
[Module Configuration](module-configuration.md).

## Module toggles

```yaml
modules:
  entity-clearing: true
  mob-limiter: true
  spawner-limiter: true
  misc-entity-limiter: true
  chunk-finder: true
  performance: true
  packet-limiter: false
  afk: false

  # 3rd Party Plugins
  wildstacker: false
  rosestacker: false
  modernshowcase: false
  griefprevention3d: false
```

This `modules:` block is the **only** runtime source of truth for whether a module is
enabled/disabled — any legacy `enabled` key you might find inside an individual
`module/<name>/config.yml` file is ignored at runtime and kept only for backward compatibility on
disk. Toggle modules here, via `/lagg admin` (right-click a module icon), or by editing this file
directly and running `/lagg reload`.

## Database settings

```yaml
database:
  enabled: true
  type: "sqlite"
  file: "clearlagg.db"
  mysql:
    host: "localhost"
    port: 3306
    database: "clearlagg"
    username: "root"
    password: "password"
    ssl-mode: "PREFERRED"
    properties: {}
  pool:
    maximum-pool-size: 10
    minimum-idle: 2
    connection-timeout-ms: 10000
    idle-timeout-ms: 600000
    max-lifetime-ms: 1800000
    keepalive-time-ms: 0
    validation-timeout-ms: 5000
```

Full breakdown, including MySQL setup and what each pool setting does: [Database Setup](database-setup.md).

## Applying changes

Hand-edited this file? Run `/lagg reload` (`CLE.reload`) rather than restarting. Note that
**switching `database.type` changes where data is stored going forward, but does not migrate
existing data** between SQLite and MySQL — back up first if you're switching backends on a live
server.
