# YinwuChat Minecraft Plugin - AI Development Guide

## Project Overview

YinwuChat is a dual-plugin system for cross-server Minecraft chat synchronization:
- **Velocity** (proxy): Modern proxy platform for message routing, WebSocket server, and Redis coordination
- **Spigot/Bukkit** (server): Client plugin on individual game servers, sends/receives messages via plugin channels

**Platform**: Migrated from BungeeCord to **Velocity 3.0+** (v2.12+)

Key features: cross-server chat, private messaging, @mentions, item display, QQ group integration, WebSocket web client support.

## Architecture Patterns

### Message Flow Architecture

1. **Bukkit → Velocity**: Messages sent via Bukkit's PluginMessaging Channel (`"BungeeCord"`)
   - [MessageManage.java](../src/main/java/org/lintx/plugins/yinwuchat/bukkit/MessageManage.java#L81) uses `ByteArrayDataOutput` to serialize JSON messages
   - Sub-channels: `org.lintx.plugins.yinwuchat:chat` (public), `org.lintx.plugins.yinwuchat:msg` (private)

2. **Velocity Processing** ([MessageManage.java](../src/main/java/org/lintx/plugins/yinwuchat/velocity/message/MessageManage.java)):
   - Subscribes to PlayerChatEvent via @Subscribe annotations
   - Applies message handlers (items, links, emojis, style filters, CoolQ encoding)
   - Routes to: other servers, WebSocket clients, QQ groups, Redis (cross-Velocity sync)

3. **WebSocket Server** ([VelocityHttpServer.java](../src/main/java/org/lintx/plugins/yinwuchat/velocity/httpserver/VelocityHttpServer.java)):
   - Netty-based HTTP/WebSocket on configurable port (default 8888)
   - Handles token auth, player list updates, message broadcasting
   - CoolQ integration via reverse WebSocket (token-based validation)

### Velocity-Specific Patterns

- **@Subscribe**: Replaces @EventHandler for event listening
- **@Plugin annotation**: Metadata-driven plugin configuration (no Plugin base class needed)
- **@Inject**: Dependency injection for ProxyServer, Logger, Path
- **Event System**: Uses Velocity's event infrastructure with proper typing
- **Scheduler**: `proxy.getScheduler().buildTask()` replaces BungeeCord's scheduler

### Configuration System

- Uses custom `Configure` module (dependency: `org.lintx.plugins.modules:Configure`)
- [Config.java](../src/main/java/org/lintx/plugins/yinwuchat/bungee/config/Config.java) (Bungee) and [Config.java](../src/main/java/org/lintx/plugins/yinwuchat/bukkit/Config.java) (Bukkit) are singleton instances
- YAML-based with version tracking (prevents breaking changes on updates)
- Nested configs: FormatConfig, CoolQConfig, RedisConfig, TipsConfig

### Optional Integrations

1. **BungeeAdminTools** (optional): [BatManage.java](../src/main/java/org/lintx/plugins/yinwuchat/bungee/BatManage.java)
   - Checks mute/ban status; safe-fails if plugin absent
   - Not in pom.xml (runtime-only)

2. **Redis** (optional): [RedisUtil.java](../src/main/java/org/lintx/plugins/yinwuchat/bungee/RedisUtil.java)
   - Jedis client for cross-BungeeCord message sync
   - Enabled via `redisConfig.openRedis` flag

3. **PlaceholderAPI** (optional): [MessageManage.java](../src/main/java/org/lintx/plugins/yinwuchat/bukkit/MessageManage.java#L107)
   - Expands PAPI placeholders (e.g., `%player_name%`)
   - Checked at runtime; graceful fallback

## Critical Dependencies & Libraries

| Library | Version | Scope | Usage |
|---------|---------|-------|-------|
| **Velocity API** | 3.2.0-SNAPSHOT | provided | Proxy plugin base, event/player APIs |
| **Velocity Native** | 3.2.0-SNAPSHOT | provided | Native bindings for Velocity |
| **Spigot API** | 1.21.11 | provided | Server-side plugin base, player/item APIs |
| **Netty** | 4.1.109 | compile | WebSocket server, HTTP handling |
| **Gson** | (bundled) | compile | JSON serialization for all message protocols |
| **Jedis** | 5.1.0 | shaded | Redis client for cross-Velocity messaging |
| **bStats Velocity** | 3.0.2 | compile | Usage metrics for Velocity plugins |
| **Lombok** | 1.18.22 | provided | Annotation processing (compile-only) |
| **org.lintx.plugins.modules:Configure** | 1.2.5 | compile | Custom YAML config framework |

**Build Plugin**: maven-shade-plugin relocates Jedis, Commons-Pool to avoid conflicts.

## Message Format & JSON Protocols

### Bukkit → BungeeCord Protocol

```json
// Public message (sub-channel: org.lintx.plugins.yinwuchat:chat)
{
  "player": "PlayerName",
  "chat": "message text",
  "format": [{"message": "text", "hover": "...", "click": "..."}],
  "handles": [{"placeholder": "regex", "format": [...]}],
  "items": ["serialized_item_json"]
}
```

### WebSocket Client Protocol

**Incoming** (Web → Server):
```json
{"action": "check_token", "token": "..."}  // or send_message, /msg commands
```

**Outgoing** (Server → Web):
```json
{"action": "send_message", "message": "..."}
{"action": "game_player_list", "player_list": [...]}
{"action": "server_message", "status": 0}
```

See [NettyWebSocketFrameHandler.java](../src/main/java/org/lintx/plugins/yinwuchat/bungee/httpserver/NettyWebSocketFrameHandler.java#L30) for input validation.

## Key Files & Their Responsibilities

| File | Purpose |
|------|---------|
| [Const.java](../src/main/java/org/lintx/plugins/yinwuchat/Const.java) | Plugin channel names, permissions, regex patterns |
| [BatManage.java](../src/main/java/org/lintx/plugins/yinwuchat/bungee/BatManage.java) | Optional BungeeAdminTools integration |
| [Listeners.java](../src/main/java/org/lintx/plugins/yinwuchat/bungee/Listeners.java) (Bungee) | PluginChannel message ingestion |
| [MessageManage.java](../src/main/java/org/lintx/plugins/yinwuchat/bungee/MessageManage.java) | Message routing, handler chaining, format application |
| [RedisUtil.java](../src/main/java/org/lintx/plugins/yinwuchat/bungee/RedisUtil.java) | Jedis pool, pub/sub for cross-BC |
| chat/handle/* | Message transformation pipeline (links, items, styles, emojis) |
| httpserver/ | Netty WebSocket server, frame parsing, HTTP static files |
| config/PlayerConfig.java | Per-player state (tokens, preferences) |

## Build & Test

```bash
# Build with Maven
mvn clean package  # Produces JAR in target/ with relocated dependencies

# Java version: 17 (configured in pom.xml)
```

No explicit test suite found; manual testing recommended after changes.

## Code Conventions

1. **Singleton pattern**: Config, MessageManage use static getInstance()
2. **Plugin channels**: All messages use Bukkit's PluginMessaging with custom JSON serialization
3. **Async task scheduling**: BungeeCord scheduler for WebSocket/Redis operations to avoid blocking
4. **Error handling**: Try-catch with safe fallback (e.g., isMute returns false if BAT absent)
5. **Style codes**: Bukkit uses `§` (section symbol); Web clients use `&` (configurable deny filters)

## Common Dev Tasks

**Adding a new message handler**:
1. Extend [Chat.java](../src/main/java/org/lintx/plugins/yinwuchat/chat/struct/Chat.java) or create handler in chat/handle/
2. Register in message processing pipeline (check [MessageManage.java](../src/main/java/org/lintx/plugins/yinwuchat/bungee/MessageManage.java) for call order)
3. Update config to expose handler settings if needed

**Integrating a new optional plugin**:
1. Add optional dependency to pom.xml with `<scope>provided</scope>`
2. Use reflection or plugin manager check (pattern: [BatManage.java](../src/main/java/org/lintx/plugins/yinwuchat/bungee/BatManage.java#L18))
3. Gracefully degrade if plugin absent

**Modifying WebSocket protocol**:
1. Update input/output JSON classes in bungee/json/
2. Validate action field in [NettyWebSocketFrameHandler.channelRead0()](../src/main/java/org/lintx/plugins/yinwuchat/bungee/httpserver/NettyWebSocketFrameHandler.java#L28)
3. Test with web client (web-source/ Vue.js frontend)

---

*Last updated: 2026-01-10*
