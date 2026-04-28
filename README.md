# ClearLaggEnhanced

[![Version](https://img.shields.io/badge/version-2026.4.8-blue.svg)](https://github.com/BusyBee-Development/ClearLaggEnhanced/releases)

A modern, high-performance lag prevention plugin for Minecraft servers running Paper, Spigot, and Folia. Designed to help server owners maintain optimal server performance through intelligent entity management, advanced lag prevention systems, and real-time performance monitoring.

**✨ Special Thanks:** To **bob7l**, the original developer of ClearLagg, whose pioneering work inspired this enhanced version.

## 📚 Documentation

Full documentation is available in the [Wiki](https://busybeedev.net/docs/clearlaggenhanced/intro).

## ✨ Key Features

- **Automatic Entity Clearing** - Smart entity removal with comprehensive protection systems
- **Advanced Lag Prevention** - Three specialized modules (Mob Limiter, Spawner Limiter, Misc Entity Limiter)
- **Real-Time Monitoring** - Live TPS and memory tracking with color-coded indicators
- **Interactive Admin GUI** - Graphical interface for easy configuration and monitoring
- **Folia Support** - Full compatibility with Folia's regionized threading system
- **Performance Database** - Optimized SQLite/MySQL storage with HikariCP connection pooling
- **Smart Configuration** - Automatic updates while preserving your changes and comments
- **Plugin Integrations** - Full support for ModernShowcase, RoseStacker, and WildStacker
- **PlaceholderAPI** - Rich placeholder support for other plugins

## 📦 Quick Start

### Requirements

- **Minecraft:** 1.20+
- **Server:** Paper, Spigot, or Folia
- **Java:** 17 or higher

### Installation

1. Download from [Modrinth](https://modrinth.com/plugin/clearlaggenhanced)
2. Place in your `plugins` folder
3. Restart your server
4. Configure in `plugins/ClearLaggEnhanced/config.yml`
5. Reload with `/lagg reload`
## 🎮 Basic Commands

| Command             | Description                          | Permission        |
|---------------------|--------------------------------------|-------------------|
| `/lagg help`        | Display the help menu                | `CLE.help`        |
| `/lagg clear`       | Manually clear entities now          | `CLE.clear`       |
| `/lagg next`        | Show time until next automatic clear | `CLE.next`        |
| `/lagg tps`         | Display current server TPS           | `CLE.tps`         |
| `/lagg admin`       | Open the admin GUI                   | `CLE.admin`       |
| `/lagg reload`      | Reload plugin configuration          | `CLE.reload`      |

**Aliases:** `/clearlagg`, `/clearlag`, `/cl`, `/cle`

## 🔧 Modules

### Entity Clearing Module
Automatically clears entities at configurable intervals with smart protection for named, tamed, stacked, and custom-tagged entities. Includes protection for mobs in boats, bred mobs, and plugin-managed entities.

### Mob Limiter Module
Controls entity spawning per chunk with global and per-type limits. Prevents excessive mob spawning that causes lag.

### Spawner Limiter Module
Controls spawner activation rates with configurable delay multipliers and mob cap integration.

### Misc Entity Limiter Module
Manages non-mob entities like armor stands, boats, item frames, and paintings with per-chunk limits and protection options.

### Chunk Finder Module
Locate laggy chunks with high entity counts. Helps identify problem areas quickly.

### Performance Module
Real-time TPS and memory monitoring with color-coded indicators and detailed statistics.

## 📊 PlaceholderAPI Support

If PlaceholderAPI is installed, use these placeholders in other plugins:

- `%clearlaggenhanced_tps%` - Current server TPS
- `%clearlaggenhanced_memory_used%` - Used memory in MB
- `%clearlaggenhanced_memory_max%` - Maximum memory in MB
- `%clearlaggenhanced_memory_percentage%` - Memory usage percentage
- `%clearlaggenhanced_entities_total%` - Total entities on server
- `%clearlaggenhanced_next_clear%` - Seconds until next clear
- `%clearlaggenhanced_next_clear_formatted%` - Human-readable time until next clear (e.g., "5m 20s")

## 🤝 Support

- **GitHub Issues:** [Report bugs or request features](https://github.com/BusyBee-Development/ClearLaggEnhanced/issues)
- **Documentation:** [Full documentation](https://busybeedev.net/docs/clearlaggenhanced/intro)
- **Discord:** Join the community on [Discord](https://discord.com/invite/abdm29q7af) for support and to suggest new ideas.

## 🙏 Credits

- **[bob7l](https://github.com/bob7l)** - Original ClearLagg developer
- **[djtmk](https://github.com/djtmk1)** - ClearLaggEnhanced developer and maintainer
- **BusyBee Development** - Development team
- **[R00tB33rMan](https://github.com/R00tB33rMan)** – Folia support and contributor
- **[MrTroxy](https://github.com/MrTroxy)** – Help with modular support in version 2026.4.0
- All contributors and community members

## 📄 License

ClearLaggEnhanced is licensed under the [BusyBee AAR License](LICENSE).
