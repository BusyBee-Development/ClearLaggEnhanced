---
id: database-setup
title: Database Setup
sidebar_position: 4
---

# Database Setup

ClearLaggEnhanced persists state via SQLite (default, zero setup) or MySQL, connected through
[HikariCP](https://github.com/brettwooldridge/HikariCP) connection pooling either way. Configured
entirely in the root `config.yml`.

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

## SQLite (default)

Nothing to configure — `database.file` (relative to the plugin's data folder unless you give an
absolute path) is created automatically. Good for single-server setups.

## MySQL

Set `database.type: "mysql"` and fill in `database.mysql.*`:

| Key | Purpose |
|---|---|
| `host` / `port` | MySQL server address |
| `database` | Schema name (must already exist) |
| `username` / `password` | Credentials |
| `ssl-mode` | e.g. `PREFERRED`, `REQUIRED`, `DISABLED` — passed through to the JDBC driver |
| `properties` | Free-form map of extra JDBC connection properties, merged into the connection URL |

Good for networks that want centralized clear/performance state across multiple servers sharing
one database.

## Connection pool tuning

| Key | Default | Purpose |
|---|---|---|
| `maximum-pool-size` | 10 | Hard cap on concurrent connections |
| `minimum-idle` | 2 | Connections kept open even when idle (must not exceed `maximum-pool-size` when using MySQL — validated on load) |
| `connection-timeout-ms` | 10000 | How long to wait for a connection before failing |
| `idle-timeout-ms` | 600000 | How long an idle connection is kept before being closed |
| `max-lifetime-ms` | 1800000 | Maximum lifetime of a connection before it's recycled |
| `keepalive-time-ms` | 0 (disabled) | Interval for keepalive pings on otherwise-idle connections |
| `validation-timeout-ms` | 5000 | Timeout for the pool's connection-validity check |

Defaults are conservative and fine for most servers. Networks running many plugins against the
same MySQL instance may want to lower `maximum-pool-size` per-plugin to stay under the database
server's total connection limit.

## Switching backends

Changing `database.type` changes where new data is written going forward — it does **not**
migrate existing data between SQLite and MySQL automatically. Back up `clearlagg.db` (or your
MySQL schema) before switching on a live server.

## What actually uses the database right now

The connection pool is initialized and validated at startup regardless — an invalid `database.*`
config (bad MySQL credentials, a blank SQLite path, etc.) will prevent the plugin from enabling, so
it's worth getting right even though no built-in module currently reads or writes through it for
day-to-day operation. It's wired up as shared infrastructure for upcoming features rather than
something any module depends on today. If your server has no reason to connect out to MySQL, the
SQLite default is the simpler choice.

## Disabling persistence

`database.enabled: false` skips connecting entirely. Since nothing currently depends on it at
runtime, this is safe to do if you'd rather not stand up a database connection at all.
