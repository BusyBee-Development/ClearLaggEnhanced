# ClearLaggEnhanced

A modern, high-performance lag prevention plugin for Minecraft servers running Paper, Spigot, and Folia. Designed to help server owners maintain optimal server performance through intelligent entity management, advanced lag prevention systems, and real-time performance monitoring.

**✨ Special Thanks:** To **bob7l**, the original developer of ClearLagg, whose pioneering work inspired this enhanced version.

## ✨ Features

### Core Systems
- **Automatic Entity Clearing** - Remove entities at configurable intervals with smart whitelisting
- **Smart Protection System** - Protect named entities, tamed animals, and custom-tagged entities
- **Advanced Lag Prevention** - Five specialized modules to target different lag sources
- **Real-Time Monitoring** - Live TPS and memory tracking with color-coded indicators
- **Interactive Admin GUI** - Graphical interface for easy configuration and monitoring
- **Folia Support** - Full compatibility with Folia's regionized threading system
- **Performance Database** - Optimized SQLite/MySQL storage with HikariCP connection pooling

### Lag Prevention Modules

| Module                  | Description                        | Key Features                                   |
|-------------------------|------------------------------------|------------------------------------------------|
| **Mob Limiter**         | Controls entity spawning per chunk | Global + per-type limits, auto-optimization    |
| **Spawner Limiter**     | Controls spawner activation rates  | Delay multipliers, mob cap integration         |
| **Misc Entity Limiter** | Manages non-mob entities           | Armor stands, boats, item frames, etc.         |

---

## 📦 Requirements

- **Minecraft Version:** 1.20 – 1.21.9
- **Server Software:** Paper, Spigot, or Folia
- **Java Version:** 17 or higher
- **Optional Dependencies:**
  - PlaceholderAPI (for placeholder support in other plugins)

---

## 🚀 Installation

1. Download the latest `ClearLaggEnhanced.jar` from [Modrinth](https://modrinth.com/plugin/clearlaggenhanced) or [GitHub Releases](https://github.com/BusyBee-Development/ClearLaggEnhanced/releases)
2. Place the JAR file in your server's `plugins` folder
3. Restart your server (do not use `/reload`)
4. Configure the plugin by editing `plugins/ClearLaggEnhanced/config.yml`
5. Customize messages in `plugins/ClearLaggEnhanced/messages.yml`
6. Reload the configuration with `/lagg reload`

---

## 🎮 Commands

All commands use the base command `/lagg` with the following aliases: `/clearlagg`, `/clearlag`, `/cl`, `/cle`

| Command             | Description                          | Permission        | Default     |
|---------------------|--------------------------------------|-------------------|-------------|
| `/lagg help`        | Display the help menu                | `CLE.help`        | All players |
| `/lagg clear`       | Manually clear entities now          | `CLE.clear`       | OP only     |
| `/lagg next`        | Show time until next automatic clear | `CLE.next`        | All players |
| `/lagg tps`         | Display current server TPS           | `CLE.tps`         | OP only     |
| `/lagg ram`         | Show memory usage information        | `CLE.ram`         | OP only     |
| `/lagg chunkfinder` | Find laggy chunks near you           | `CLE.chunkfinder` | OP only     |
| `/lagg admin`       | Open the admin GUI (players only)    | `CLE.admin`       | OP only     |
| `/lagg reload`      | Reload plugin configuration          | `CLE.reload`      | OP only     |

---

## 🔐 Permissions

| Permission        | Description                       | Default |
|-------------------|-----------------------------------|---------|
| `CLE.*`           | All ClearLaggEnhanced permissions | OP      |
| `CLE.help`        | Access to help command            | True    |
| `CLE.clear`       | Access to manual entity clearing  | OP      |
| `CLE.next`        | Access to next clear timer        | True    |
| `CLE.tps`         | Access to TPS monitoring          | OP      |
| `CLE.ram`         | Access to memory information      | OP      |
| `CLE.chunkfinder` | Access to laggy chunk finder      | OP      |
| `CLE.admin`       | Access to admin GUI               | OP      |
| `CLE.reload`      | Access to configuration reload    | OP      |

---

## ⚙️ Configuration

### Entity Clearing

Configure automatic entity clearing in `config.yml`:

```yaml
entity-clearing:
  enabled: true              # Enable/disable automatic clearing
  interval: 300              # Clear interval in seconds (300 = 5 minutes)
  protect-named-entities: true   # Protect entities with custom names
  protect-tamed-entities: true   # Protect tamed animals (pets)
  worlds: []                 # Target specific worlds (empty = all worlds)

  # Whitelist: Entities that will NEVER be cleared
  whitelist:
    - "VILLAGER"
    - "IRON_GOLEM"
    - "ARMOR_STAND"
    - "ITEM_FRAME"
    - "PAINTING"
    - "MINECART"
    - "BOAT"
```

**How it works:**
- Any entity **NOT** in the whitelist will be cleared (except players, named, and tamed)
- Players are always protected
- Named entities are protected when `protect-named-entities: true`
- Tamed animals are protected when `protect-tamed-entities: true`

**Common entity types:** `DROPPED_ITEM`, `EXPERIENCE_ORB`, `ARROW`, `ZOMBIE`, `SKELETON`, `CREEPER`, `SPIDER`, `ENDERMAN`, `COW`, `SHEEP`, `PIG`, `CHICKEN`, `PRIMED_TNT`

---

### Lag Prevention Modules

#### Mob Limiter
Prevents mob spawning when chunk limits are reached.

```yaml
mob-limiter:
  enabled: true
  max-mobs-per-chunk: 50    # Global mob limit per chunk

  # Per-type limits (optional)
  per-type-limits:
    enabled: true
    limits:
      ZOMBIE: 10
      SKELETON: 10
      CREEPER: 8
      VILLAGER: 15
      COW: 12
      CHICKEN: 15
```

**Features:**
- Global chunk limit prevents excessive mob spawning
- Per-type limits control specific mob types
- Automatic chunk optimization when limits are exceeded
- Compatible with spawner and natural spawns

---

#### Spawner Limiter
Controls mob spawner activation rates to reduce lag.

```yaml
spawner-limiter:
  enabled: true
  spawn-delay-multiplier: 1.5  # Multiply spawner delays by this value
  worlds: []                    # Target specific worlds (empty = all)
```

**How it works:**
- Increases spawner delays when chunk mob limits are reached
- Multiplies the current spawner delay by the configured multiplier
- Integrates with mob limiter for optimal results

---

#### Misc Entity Limiter
Manages non-mob entities like armor stands, boats, and item frames.

```yaml
misc-entity-limiter:
  enabled: true

  # Per-chunk entity limits (-1 = unlimited)
  limits-per-chunk:
    ARMOR_STAND: 5
    BOAT: 5
    CHEST_BOAT: 5
    MINECART: 5
    ITEM_FRAME: 5
    GLOW_ITEM_FRAME: 5
    PAINTING: 5

  # Protection settings
  protect:
    named: true              # Protect named entities
    tags:                    # Protect entities with these scoreboard tags
      - "CLE_PROTECTED"

  # Sweep configuration
  sweep:
    interval-ticks: 100      # How often to check chunks (100 = 5 seconds)
    max-chunks-per-tick: 20  # Chunks to process per tick (prevents lag spikes)

  # Admin notifications
  notify:
    admins-permission: "CLE.admin"
    throttle-seconds: 60     # Cooldown between notifications

  worlds: []                 # Target specific worlds (empty = all)
```

**Protecting entities:**
1. **By name** - Set `protect.named: true` to protect entities with custom names
2. **By tag** - Add scoreboard tags to protect specific entities:
   ```
   /tag @e[type=armor_stand,limit=1,sort=nearest] add CLE_PROTECTED
   ```

**How it works:**
- Periodically sweeps chunks to count miscellaneous entities
- Removes excess entities when limits are exceeded (oldest first)
- Respects protection settings (named/tagged entities)
- Notifies admins when entities are trimmed

---

### Notifications

Configure entity clearing warnings and notifications:

```yaml
notifications:
  broadcast-times: [60, 30, 10, 5]  # Warning times in seconds
  type: "ACTION_BAR"                # Display type: CHAT, ACTION_BAR, or TITLE
  console-notifications: false       # Show notifications in console

  sound:
    enabled: true
    name: "BLOCK_NOTE_BLOCK_PLING"
    volume: 1.0
    pitch: 1.2
```

**Notification types:**
- `CHAT` - Send warnings in chat
- `ACTION_BAR` - Display above the hotbar (recommended)
- `TITLE` - Show as large title overlay

---

### Database

ClearLaggEnhanced uses a database for performance tracking and optimization.

```yaml
database:
  type: "sqlite"             # Database type: sqlite or mysql
  file: "clearlagg.db"       # SQLite database filename

  # MySQL configuration (only used if type is "mysql")
  mysql:
    host: "localhost"
    port: 3306
    database: "clearlagg"
    username: "root"
    password: "password"
    ssl: false
```

**Database features:**
- **HikariCP Connection Pooling** - Enterprise-grade connection management
- **Strategic Indexes** - 10-100x faster queries
- **Automatic Migration** - Schema updates handled automatically
- **MySQL Support** - For multi-server networks

---

## 📊 PlaceholderAPI Integration

If PlaceholderAPI is installed, you can use these placeholders in other plugins:

| Placeholder                            | Description              | Example Output  |
|----------------------------------------|--------------------------|-----------------|
| `%clearlagenhanced_tps%`               | Current server TPS       | `19.85`         |
| `%clearlagenhanced_memory_used%`       | Used memory in MB        | `2048`          |
| `%clearlagenhanced_memory_max%`        | Maximum memory in MB     | `4096`          |
| `%clearlagenhanced_memory_percentage%` | Memory usage percentage  | `50.0`          |
| `%clearlagenhanced_entities_total%`    | Total entities on server | `1250`          |
| `%clearlagenhanced_next_clear%`        | Seconds until next clear | `180`           |

**Example usage in other plugins:**
- Scoreboards: Display TPS and memory usage
- Tab list: Show time until next clear
- Holograms: Real-time performance statistics

---

## 🔧 Advanced Features

### Chunk Finder Tool
Locate laggy chunks around your current position:

```
/lagg chunkfinder
```

**Configuration:**
```yaml
chunk-finder:
  radius: 10                 # Scan radius in chunks
  entity-threshold: 50       # Entities per chunk to be considered "laggy"
```

**Output:**
- Shows top 10 laggy chunks with entity counts
- Displays distance from your current position
- Helps identify problem areas quickly

---

### Performance Monitoring
Real-time performance tracking with configurable update intervals:

```yaml
performance:
  update-interval: 20        # Update interval in ticks (20 = 1 second)
```

**Available commands:**
- `/lagg tps` - View server TPS with color-coded indicators
- `/lagg ram` - Detailed memory usage breakdown

---

### Message Customization
All messages support MiniMessage formatting. Edit `messages.yml` to customize:

```yaml
warnings:
  entity-clear: "<gold>⚠</gold> <yellow>Entities will be cleared in <white>{seconds}</white> seconds!</yellow>"

notifications:
  clear-complete: "<green>✓</green> <green>Cleared <white>{count}</white> entities in <gray>{time}ms</gray></green>"
```

**MiniMessage format reference:** https://docs.advntr.dev/minimessage/format.html

---

## 🎯 Performance Optimization Guide

### For Small Servers (1-20 players)
```yaml
entity-clearing:
  interval: 600              # 10 minutes

mob-limiter:
  max-mobs-per-chunk: 75
```

### For Medium Servers (20-100 players)
```yaml
entity-clearing:
  interval: 300              # 5 minutes

mob-limiter:
  max-mobs-per-chunk: 50
  per-type-limits:
    enabled: true
```

### For Large Servers (100+ players)
```yaml
entity-clearing:
  interval: 180              # 3 minutes

mob-limiter:
  max-mobs-per-chunk: 35
  per-type-limits:
    enabled: true

misc-entity-limiter:
  enabled: true
  limits-per-chunk:
    ARMOR_STAND: 3
    ITEM_FRAME: 3
```

### General Tips
1. **Start conservative** - Begin with default settings and adjust based on performance
2. **Monitor regularly** - Use `/lagg tps` and `/lagg chunkfinder` to identify issues
3. **Enable gradually** - Enable lag prevention modules one at a time
4. **World-specific config** - Configure different settings for different worlds
5. **Player education** - Inform players about entity limits and lag prevention

---

## 🔍 Troubleshooting

### Entities Not Being Cleared

**Check these settings:**
- Is the entity in the whitelist? Remove it if it should be cleared
- Is `protect-named-entities: true`? Named entities won't be cleared
- Is the entity tamed? Set `protect-tamed-entities: false` to clear tamed animals
- Is entity clearing enabled? Check `entity-clearing.enabled: true`

### Performance Still Poor

**Try these solutions:**
1. Enable more lag prevention modules
2. Lower mob limits per chunk (`max-mobs-per-chunk: 35`)
3. Reduce clearing interval (`interval: 180` = 3 minutes)
4. Use `/lagg chunkfinder` to locate problem areas
5. Enable misc entity limiter for armor stands/boats

### Commands Not Working

**Verify:**
- Player has the correct permission node
- Use full command `/lagg` instead of aliases
- Console can always execute commands regardless of permissions
- Check for permission plugin conflicts

### High Memory Usage

**Solutions:**
1. Reduce entity clearing interval
2. Clear more entity types (reduce whitelist)
3. Enable all lag prevention modules
4. Allocate more RAM to your server
5. Check for memory leaks in other plugins

### Folia Compatibility Issues

**Notes:**
- ClearLaggEnhanced is fully Folia-compatible
- Ensure you're running the latest version
- Report any Folia-specific issues on GitHub

---

## 💬 Support

### Getting Help
- **GitHub Issues:** [Report bugs or request features](https://github.com/BusyBee-Development/ClearLaggEnhanced/issues)
- **Documentation:** This README and config.yml comments
- **Discord:** Join our community for real-time support

### Reporting Bugs
When reporting bugs, please include:
1. Server version (Paper/Spigot/Folia and MC version)
2. Plugin version
3. Full error logs (from `logs/latest.log`)
4. Relevant config sections
5. Steps to reproduce the issue

### Feature Requests
We welcome feature requests! Please:
1. Check if it's already requested on GitHub Issues
2. Describe the feature and use case clearly
3. Explain how it would benefit server owners

---

## 🙏 Credits

- **bob7l** - Original ClearLagg developer
- **djtmk** - ClearLaggEnhanced developer
- **BusyBee Development** - Development team
- **R00tB33rMan** - Folia support and Contributor
- All contributors and community members

---

**Made with ❤️ for the Minecraft server community**
