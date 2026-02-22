# Velocity Migration Guide

## Overview
YinwuChat has been migrated from BungeeCord to Velocity proxy platform. This guide covers the changes and how to deploy the new version.

## What's Changed

### Architecture Changes
- **Proxy Platform**: BungeeCord → Velocity
- **Event System**: EventHandler annotations → @Subscribe annotations
- **Plugin API**: BungeeCord API → Velocity API
- **Dependency Management**: Updated Maven dependencies for Velocity

### Key Directory Changes
```
Old Structure (BungeeCord):
src/main/java/org/lintx/plugins/yinwuchat/bungee/

New Structure (Velocity):
src/main/java/org/lintx/plugins/yinwuchat/velocity/
├── YinwuChat.java           (Main plugin class)
├── config/Config.java        (Configuration management)
├── listeners/Listeners.java  (Event listeners)
├── message/MessageManage.java (Message routing)
└── httpserver/               (WebSocket server)
    ├── VelocityHttpServer.java
    ├── VelocityWebSocketFrameHandler.java
    ├── VelocityHttpRequestHandler.java
    └── NettyChannelMessageHelper.java
```

## Installation

### Requirements
- Java 17 or higher
- Velocity 3.0.0+
- Spigot 1.21.11+ (for backend servers)
- Redis (optional, for cross-proxy clustering)

### Steps
1. Build the plugin:
   ```bash
   mvn clean package
   ```

2. Place the generated JAR in your Velocity's `plugins/` directory:
   ```
   velocity/plugins/YinwuChat-2.12.jar
   ```

3. Restart Velocity to generate default config files

4. Edit `plugins/yinwuchat/config.yml` with your settings

5. Restart Velocity again to apply configuration

## Configuration Migration

### From BungeeCord to Velocity
The configuration file format remains the same YAML structure. You can:

**Option A**: Copy your existing BungeeCord config
- Copy `BungeeCord/plugins/YinwuChat/config.yml`
- Paste to `Velocity/plugins/yinwuchat/config.yml`
- Velocity will handle it automatically

**Option B**: Use generated default config
- Let Velocity generate a fresh config
- Manually copy your settings over

### Configuration File Location
- **BungeeCord**: `plugins/YinwuChat/config.yml`
- **Velocity**: `plugins/yinwuchat/config.yml` (created automatically)

## Features Status

### Fully Implemented
- ✅ WebSocket server for web client connections
- ✅ Configuration loading and management
- ✅ Player chat message handling
- ✅ Message filtering and transformation

### In Development
- 🔄 Cross-Velocity chat via Redis
- 🔄 Private messaging system
- 🔄 @mention functionality
- 🔄 Item serialization and display
- 🔄 QQ group integration

### Not Yet Implemented
- ❌ BungeeAdminTools integration (needs Velocity alternative)
- ❌ PlaceholderAPI support (Velocity feature set differs)

## Building from Source

### Prerequisites
- Maven 3.6.0+
- Java 17 JDK

### Build Steps
```bash
cd YinwuChat/
mvn clean package
```

Output JAR: `target/YinwuChat-2.12.jar`

## Troubleshooting

### Plugin Not Loading
- Check Java version: `java -version` (must be 17+)
- Check logs in `logs/latest.log`
- Ensure JAR is in correct plugins directory

### WebSocket Server Won't Start
- Check if port is already in use: `netstat -an | grep 8888`
- Check firewall settings
- Verify `openwsserver: true` in config.yml
- Check port availability with different value

### Configuration Not Loading
- Delete auto-generated config and restart to regenerate
- Check file permissions
- Ensure YAML syntax is valid

## API Changes for Developers

### Event Handling
```java
// BungeeCord
@EventHandler
public void onChat(ChatEvent event) { }

// Velocity
@Subscribe
public void onChat(PlayerChatEvent event) { }
```

### Plugin Registration
```java
// BungeeCord
public class YinwuChat extends Plugin { }

// Velocity
@Plugin(id = "yinwuchat-velocity", ...)
public class YinwuChat {
    @Inject
    public YinwuChat(ProxyServer proxy, Logger logger) { }
}
```

### Player Access
```java
// BungeeCord
ProxiedPlayer player = (ProxiedPlayer) sender;

// Velocity
Player player = (Player) source;
```

## Support

For issues or questions:
1. Check logs in `logs/latest.log`
2. Enable debug mode in config.yml
3. Create GitHub issue with logs and config (sensitive info removed)

## Contributing

To contribute improvements:
1. Fork the repository
2. Create feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit changes (`git commit -m 'Add AmazingFeature'`)
4. Push to branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

---

**Migration completed**: 2026-01-10
**Version**: 2.12
