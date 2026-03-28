# YinwuChat 说明文档

### 关于YinwuChat

#### 前言

[YinwuChat]为中国正版Minecraft公益服务器[YinwuRealm](www.yinwurealm.org)插件，开发者为服务器运维团队前辈[LinTx]

随着游戏版本变更，沧海桑田，前任服务器维护团队逐渐由于各类原因停止了维护，其中包括该项目。
7年时间里，服主[fengshuai]也曾尝试过对插件进行补丁修正，但苦于精力有限，未能对chat进行大版本更新，chat的版本号停留在了2.12

7年后的今日，由于AI的飞速进步，服务器运维团队终于可以乘AI的东风重启对Chat的运维计划，尘封已久的chat项目得以重见天日

如今，该项目由服务器运维团队[YinwuRealm服务组]运维者[Xx_Kirov_xX]尝试重新运维

时光荏苒，随着Mincraft的版本更迭，如今的Minecraft服务器已不复当年百花齐放的姿态，YinwuRealm公益服务器更是因为缺乏资金与人手，一直处于举步为艰的状态。
2025年底，服务组组长[MadaoMeloN]启动了Yinwu第十二周目的建设工作，chat作为核心插件被提上日程。
如今，chat将按照如下几点逐步更新：

1. 与更好的服务器核心兼容，如folia等 
2. 跨服通讯功能，从bungeecord迁移至velocity 
3. 玩家物品信息[i]功能 
4. 彩色文本功能 
5. 服务器消息与Q群同步功能 **暂无更新计划**
6. 关键词过滤功能等
7. 网页端通讯，实现使用APP与服务器内玩家实时通讯的功能等

#### 版本更新

- 2.12.60：更新版本号配置，解决私聊格式问题
- 2.12.61：消除上版本存在的消息重复发送问题
- 2.12.62：解决玩家名称hover的占位符和msg无法调用玩家名称的问题，基本完成YinwuChat跨服通讯功能搭建
- 2.12.63：redis依赖转向velocity,at与noat上线
- 2.12.64：前后缀编辑上线，优化编辑功能，现在可以编辑、删除、以及快捷指令导入，加入私聊前后缀
- 2.12.65：folia支持加入，正在测试中，bukkit运行正常，paper测试正常
- 2.12.66：新增禁言功能，隐身功能，重加载插件功能，支持定时禁言、永久禁言、禁言原因；新增独立命令 /mute、/unmute、/muteinfo；优化 [p] 位置显示功能，修复自定义前后缀与占位符冲突问题，至此，服内聊天功能已全部上线
- 2.12.67：更新网页端公共聊天室消息显示兼容处理，完善 WebSocket 消息解析与颜色码渲染，实现web端指令集成
- 2.12.68：上线APP debug版本，支持安卓端
- 2.12.69：Web端隐身功能上线，支持独立 /vanish 指令；优化 Web 端指令权限控制与白名单机制；更新 Web 端系统头像；完善忽略（Ignore）逻辑；支持独立 /ignore 和 /noat 指令等
- 2.12.70：UI优化：移除私聊窗口顶部头像图标，精简界面；Web端侧边栏交互优化：支持手机端左侧侧滑呼出/关闭玩家列表，实现平滑跟随效果；修复 Android App 手势兼容性；加入/chatban、/chatunban，实现封禁管理，并加入了APP/web端双端同时登录功能
- 2.12.71：支持 Web 端与 APP 同时使用同一账号登录，消息实时同步；web端头像改为 /helm/ 端点，显示双层皮肤（底层+头盔层）；新增 /chatban指令，实现 web 端封禁同步游戏端禁言，新增 /chatunban 解封指令，支持解除 /chatban 的封禁
- 2.12.72：Web端UI优化，新增快捷选人功能，支持点击玩家列表快速发起私聊；新增 HarmonyOS（鸿蒙）原生应用支持，基于 ArkTS 开发，支持 HarmonyOS NEXT 5.0.0+，实现与 Web 端/Android 端相同的聊天功能
- 2.12.73：增加了 Web 端物品展示功能，现在可以在游戏内向玩家和 Web 端玩家展示物品；修正服务器反代环境下 App 无法连接服务器的问题，现在可以正常连接
- 2.12.74：新增服务器端消息缓存与重放，Web/App 用户上线后自动补发离线消息；新增未读消息分割线定位；修复 App 端消息重复与 WebSocket 连接问题；内嵌 HarmonyOS Sans 字体统一显示；优化图标自动渲染与 Velocity 控制台日志
- 2.12.75：修复 Folia 服务端插件启动失败问题，新增 SchedulerUtil 调度工具类，通过反射兼容 Folia AsyncScheduler 与 EntityScheduler；ItemDisplayCache 与 ViewItemCommand 全面适配 Folia 区域线程调度
- 2.12.76：新增 Web 端"重置 Token"功能，支持一键清空账号下所有已绑定 Token 并强制重新绑定；修复服务器广播消息在公屏聊天重复显示问题，广播改为置顶横幅展示（支持多条广播折叠查看）；修复 Web 端玩家发送消息时游戏内服务器前缀显示错误问题；修复 Token 绑定失败与 WebSocket 路径路由不匹配问题（新增 Netty 管线路径重写）；修复 TokenManager 未完整清除同一 UUID 下所有 Token 的问题；优化 Token 绑定时旧 Token 自动清理逻辑

- 3.0.0：YinwuChat重构后第一个正式版本，该版本针对web端做了全新优化，快捷指令列表重构，可读性进行优化，token绑定消息转为消息块显示模式，更加清晰。至此，YinwuChat基本重构完成
- 3.1.0：web端现已支持快捷登录，选择记住登录后即可在下次登录时快速登录；新增账号切换功能，同一web账号下可绑定多个游戏账号，可在登录后切换，亦可在聊天时切换。游戏端新增[b]背包展示功能，输入后即可展示背包，管理员新增背包公示功能，输入/yinwuchat backpackview 玩家名 即可将对应玩家的背包展现在公屏上。另增改动：游戏端指令快速补全已加入，可根据指令自动展示tab补全，方便输入
- 3.1.1：@全体管理员功能优化，加入管理员快捷确认；前后缀编辑器优化，现在可以快捷输入和清除前后缀；广播消息优化，现在被写入click的网址可以被自动识别，游戏端点击即可自动打开网页
- 3.2.0：更新视监功能（私聊监听）`/yinwuchat monitor`子命令与视监菜单；现在可监听全体/指定玩家、查看玩家私聊历史记录；
#### 示例图片
<p align="center">
  <img src="./picturesForReadme/log_in.png" alt="登录界面" />
</p>

<p align="center">
  <img src="./picturesForReadme/chat_day.png" alt="聊天界面——日间" />
</p>

<p align="center">
  <img src="./picturesForReadme/chat.png" alt="聊天界面——夜间" />
</p>

<p align="center">
  <img src="./picturesForReadme/token.png" alt="token绑定消息" />
</p>

<p align="center">
  <img src="./picturesForReadme/game_chat.png" alt="游戏内聊天" />
</p>

<p align="center">
  <img src="./picturesForReadme/web_and_game.png" alt="web端消息与游戏端前缀分离" />
</p>

<p align="center">
  <img src="./picturesForReadme/i_in_game.png" alt="游戏内i的显示" />
</p>

<p align="center">
  <img src="./picturesForReadme/i_detail.png" alt="游戏内i的物品详情" />
</p>

<p align="center">
  <img src="./picturesForReadme/i_web_detail.png" alt="web端i的详情" />
</p>

<p align="center">
  <img src="./picturesForReadme/p_in_game.png" alt="游戏内p的显示" />
</p>

<p align="center">
  <img src="./picturesForReadme/p_web.png" alt="web端p的显示" />
</p>

### 简介
YinwuChat是Velocity代理插件和Spigot插件，主要功能有：

1. 跨服聊天同步
2. 跨服私聊（`/msg <玩家名> 消息`）
3. 跨服@（聊天内容中输入想@的玩家的名字，或名字的前面一部分，不区分大小写）
4. 跨服物品展示（聊天内容中输入`[i]`即可将手中的物品发送到聊天栏，点击即可查看物品详情信息，支持跨服展示）
5. 跨服位置信息展示（聊天中输入`[p]`即可将位置发送至聊天栏，支持悬浮查看hover中详细信息）
6. Web端与游戏互联，开启WebSocket后配合YinwuChat-Web（Web客户端）可以实现web、APP、游戏内聊天同步
7. 关键词屏蔽，可自定义关键词以屏蔽发表不正确、仇恨言论等的消息，并把多次发送此类消息玩家踢出服务器
8. 聊天自定义前后缀，支持自定义前后缀，自定义颜色，装饰你的聊天

**注意**：你需要在你的**Velocity代理服务器**和这个Velocity接入的所有的**Spigot服务器**都安装这个插件

### 指令说明

#### Web 端配置指令

- `/yinwuchat bind <Token>`：绑定 Web 账号
- `/yinwuchat unbind`：解绑当前 Web 账号
- `/yinwuchat list`：查看已绑定 Token
- `/yinwuchat webbind query <账号名/玩家名>`：查询 Web 绑定关系（管理员）
- `/yinwuchat webbind unbind <账号名/玩家名>`：解除 Web 绑定关系（管理员）

说明：

- Web 账号与玩家名的自动绑定在 **Web 登录成功并建立 WS 连接且 Token 已绑定** 后完成。
- 使用玩家名执行查询/解绑时，会自动映射到对应 Web 账号。

#### Velocity 指令

- `/yinwuchat ws`：获取当前 WebSocket 地址
- `/yinwuchat reload [config|ws]`：重新加载配置（管理员）
- `/yinwuchat msg <玩家名> <消息>`：发送私聊消息（别名：`/msg /tell /whisper /w /t`）
- `/yinwuchat noat`：禁止/允许被@
- `/yinwuchat muteat`：切换@时的声音
- `/yinwuchat ignore <玩家名>`：忽略/取消忽略玩家
- `/yinwuchat vanish`：切换隐身模式（管理员）
- `/yinwuchat atalladmin`：报告突发事件给所有管理员（每日限一次；管理员收到提醒后可点击“确认收到”快捷填入确认指令）
- `/yinwuchat atalladmin confirm <玩家名>`：重置玩家报告冷却时间（管理员）
- `/yinwuchat format edit|show`：编辑/查看聊天前后缀
- `/yinwuchat format <public|private> <prefix|suffix> set <内容>`：设置对应前后缀
- `/yinwuchat format <public|private> <prefix|suffix> clear`：清除对应前后缀
- `/yinwuchat mute <玩家> [时长] [原因]`：禁言玩家（别名：`/mute`，管理员）
- `/yinwuchat unmute <玩家>`：解除禁言（别名：`/unmute`，管理员）
- `/yinwuchat muteinfo <玩家>`：查看禁言信息（别名：`/muteinfo`，管理员）
- `/yinwuchat permsync`：同步 LuckPerms 组权限节点（管理员）
- `/yinwuchat chatban <玩家> [时长] [原因]`：封禁 Web 账号（别名：`/chatban`，管理员）
- `/yinwuchat chatunban <玩家>`：解封 Web 账号（别名：`/chatunban`，管理员）
- `/yinwuchat itemdisplay`：物品展示帮助（别名：`/itemdisplay /showitem /displayitem`）

#### Velocity Tab 补全

- `/yinwuchat` + `Tab`：显示当前玩家有权限使用的一级子命令
- `/yinwuchat reload`：补全 `config`、`ws`
- `/yinwuchat ignore`、`msg`、`mute`、`unmute`、`muteinfo`、`chatban`、`chatunban`：补全在线玩家名
- `/yinwuchat mute <玩家>`、`chatban <玩家>`：继续补全常用时长模板，如 `10m`、`30m`、`1h`、`1d`
- `/yinwuchat format`：按层补全 `edit|show|public|private -> prefix|suffix -> set|clear`
- `/yinwuchat atalladmin`：补全 `confirm`，管理员继续补全玩家名
- `/yinwuchat webbind`：补全 `query|unbind`，随后补全玩家名
- `/yinwuchat badword`：补全 `add|remove|list`，`remove` 后继续补全现有屏蔽词

#### Bukkit 指令（子服）

- `/msg <玩家名> <消息>`：子服私聊指令
- `/viewitem <物品ID>`：查看物品展示详情
- `/yinwuchat-bukkit`：子服桥接指令（内部使用）

### 配置说明

#### Velocity代理服务器配置

Velocity配置文件通常位于 `plugins/velocity/config.toml`：

```toml
[servers]
lobby = "127.0.0.1:30001"
survival = "127.0.0.1:30002"
creative = "127.0.0.1:30003"
```

#### 后端服务器配置

每个后端服务器的配置文件位于 `plugins/YinwuChat/config.yml`：

```yaml
# 服务器名称配置（自动检测玩家所在服务器）
serverName: "lobby"  # 可选：手动指定服务器名称，不设置则自动检测

# 消息格式配置（所有消息都通过Velocity统一处理）
format:
  - message: "&b[ServerName]"    # 服务器名称占位符，自动替换为实际服务器名
    hover: "所在服务器：ServerName"  # 悬停显示，ServerName会被替换
    click: "/server ServerName"      # 点击事件，命令会填入聊天栏而不是自动执行
  - message: "&e{displayName}"   # 玩家名称占位符
    hover: "点击私聊"
    click: "/msg {displayName}"
  - message: " &6>>> "
  - message: "&r{message}"       # 消息内容占位符

# 其他配置...
```

#### 跨服聊天说明

插件实现了完整的跨服聊天功能：

- **普通消息**：自动跨服同步，所有服务器的玩家都能看到
- **物品消息**：支持跨服展示物品信息
- **服务器标识**：消息前显示发送者所在的服务器名称
- **悬停点击**：支持悬停查看服务器信息，点击切换服务器

#### 消息处理流程

1. 玩家发送消息 - Bukkit端接收
2. Bukkit端发送到Velocity - 包含服务器名称信息
3. Velocity处理格式化 - 应用占位符替换
4. Velocity广播到所有服务器 - 所有玩家收到消息

#### 服务器名称自动检测

插件支持多种方式自动检测服务器名称：

1. **配置文件指定**（推荐）：
  ```yaml
   serverName: "lobby"
  ```
2. **系统属性**：
  ```bash
   java -Dyinwuchat.server.name=lobby -jar server.jar
  ```
3. **环境变量**：
  ```bash
   export YINWUCHAT_SERVER_NAME=lobby
   java -jar server.jar
  ```
4. **自动检测**：使用Velocity配置中的服务器名称

#### 消息格式占位符

- `[ServerName]` 或 `{ServerName}`: 显示服务器名称
- `{displayName}`: 显示玩家名称
- `{message}`: 显示消息内容

#### 示例效果

- **大厅服务器**: `[lobby]玩家A >>> 你好`
- **生存服务器**: `[survival]玩家B >>> 欢迎来到生存服`
- **创造服务器**: `[creative]玩家C >>> [物品展示]`

### 定期广播

#### 功能简介

定期广播功能允许管理员配置多条自动循环广播消息，广播内容支持颜色代码、悬停提示和点击事件（将命令填入聊天栏）。游戏端和 Web 端均可接收广播。

#### 配置文件

配置文件路径：`velocity/plugins/YinwuChat/broadcast.yml`

首次启动插件后会自动生成带有中文注释的默认配置文件，修改后执行 `/yinwuchat reload` 即可热重载。

#### 字段说明


| 字段               | 类型      | 默认值     | 说明                                      |
| ---------------- | ------- | ------- | --------------------------------------- |
| `enable`         | boolean | `false` | 是否启用该广播任务                               |
| `interval`       | int     | `300`   | 广播间隔（秒），300 = 5分钟                       |
| `web`            | boolean | `true`  | 是否发送到 Web 端（独立控制，不受白名单/黑名单影响）           |
| `includeMode`    | boolean | `false` | 白名单模式开关，开启后仅 `includeServers` 中的游戏服务器接收 |
| `includeServers` | list    | `[]`    | 白名单服务器列表                                |
| `excludeMode`    | boolean | `false` | 黑名单模式开关，开启后 `excludeServers` 中的游戏服务器不接收 |
| `excludeServers` | list    | `[]`    | 黑名单服务器列表                                |
| `list`           | list    | -       | 广播内容列表                                  |
| `list[].message` | string  | -       | 显示文本，支持 `&` 颜色码（如 `&e` 黄色、`&b` 青色）      |
| `list[].hover`   | string  | （可选）    | 鼠标悬停提示文本                                |
| `list[].click`   | string  | （可选）    | 点击行为：网址自动打开；命令、`[i]`、`[p]`、`[b]` 自动填入聊天栏 |


> **注意：** `includeMode` 和 `excludeMode` 都关闭时，所有游戏服务器都接收；两者同时开启时 `includeMode` 优先。

> **点击识别规则：**
> - 支持自动打开：`http://`、`https://`、`ftp://`、`file://`、`www.xxx.com`
> - 支持自动填入聊天栏：`/yinwuchat noat`、`/msg 玩家名`、`[i]`、`[i:0]`、`[p]`、`[b]`
> - `example.com/path` 这类裸域名默认不会自动识别为网址

#### 使用场景示例

**场景一：全服广播（默认，所有游戏服务器 + Web 端都接收）**

```yaml
tasks:
  - enable: true
    interval: 300
    web: true
    includeMode: false
    includeServers: []
    excludeMode: false
    excludeServers: []
    list:
      - message: "&e[官网]"
        hover: "点击打开官网"
        click: "www.yinwurealm.org"
      - message: "&r 欢迎来到服务器，输入"
      - message: "&b[i]"
        hover: "点击填入物品展示占位符"
        click: "[i]"
      - message: "&r 即可展示手持物品"
```

**场景二：白名单模式（仅指定服务器接收，Web 端不接收）**

```yaml
tasks:
  - enable: true
    interval: 600
    web: false
    includeMode: true
    includeServers: [survival, lobby]
    excludeMode: false
    excludeServers: []
    list:
      - message: "&a[生存/大厅] &r这条广播仅 survival 和 lobby 可见"
```

**场景三：黑名单模式（排除指定服务器，Web 端接收）**

```yaml
tasks:
  - enable: true
    interval: 300
    web: true
    includeMode: false
    includeServers: []
    excludeMode: true
    excludeServers: [creative]
    list:
      - message: "&c[重要] &r这条广播除了 creative 以外的服务器都能收到"
```

#### 热重载

修改 `broadcast.yml` 后，在游戏内或控制台执行以下命令即可重载广播配置，无需重启服务器：

```
/yinwuchat reload
```

### 平台变更

- **v2.12+**: 从BungeeCord迁移至Velocity代理平台
- Velocity提供更好的性能和现代化的API设计
- 所有功能保持兼容，配置文件格式不变

## 构建说明

### 环境要求

- Java 17+
- Maven 3.6+

### 多平台构建

项目支持同时构建 Velocity 代理和 Bukkit/Spigot 后端版本：

#### Windows

```bash
# 构建所有平台
build.bat all

# 仅构建 Velocity 版本
build.bat velocity

# 仅构建 Bukkit 版本
build.bat bukkit
```

#### Linux/Mac

```bash
# 构建所有平台
./build.sh all

# 仅构建 Velocity 版本
./build.sh velocity

# 仅构建 Bukkit 版本
./build.sh bukkit
```

#### Maven 命令

```bash
# 构建所有平台（默认）
mvn clean package

# 仅构建 Velocity 版本
mvn clean package -P velocity

# 仅构建 Bukkit 版本
mvn clean package -P bukkit
```

### 输出文件

构建完成后，在 `target/` 目录下会生成：

- `YinwuChat-Velocity-3.2.0.jar` - Velocity 代理专用版本
- `YinwuChat-Bukkit-3.2.0.jar` - Bukkit/Spigot 后端专用版本
- `YinwuChat-3.2.0.jar` - 包含所有平台代码（向后兼容）

### 部署说明

1. **Velocity 代理服务器**
  - 安装：`YinwuChat-Velocity-3.2.0.jar`
  - 位置：`plugins/YinwuChat-Velocity-3.2.0.jar`
2. **Bukkit/Spigot 后端服务器**
  - 安装：`YinwuChat-Bukkit-3.2.0.jar`
  - 位置：`plugins/YinwuChat-Bukkit-3.2.0.jar`
3. **配置文件**
  - 首次运行后会在相应插件目录生成配置文件

### Q群聊天同步

本插件支持通过 **AQQBot**（基于 OneBot 标准）或 **CoolQ HTTP API** 实现 QQ 群聊天同步。
**推荐使用 AQQBot**（支持 Lagrange、LLoneBot、NapCat 等后端）

#### 方式一：使用 AQQBot（推荐）

1. **YinwuChat 插件配置**
  - 需要开启 `openwsserver`
  - 配置 `aqqBotConfig` 部分：
    - `qqGroup`: 设置为你想同步的 QQ 群号
    - `accessToken`: 设置为一个足够复杂足够长的字符串（推荐32位左右的随机字符串）
    - `gameToQQ`: 设置为 `true` 以启用游戏内消息发送到 QQ 群
    - `qqToGame`: 设置为 `true` 以启用 QQ 群消息发送到游戏内
2. **安装 AQQBot 后端**
  **选项 A：使用 Lagrange 后端**
  - 从 [Lagrange 的 GitHub Releases](https://github.com/LagrangeDev/Lagrange.Core) 下载适合您操作系统的版本
  - 运行 `Lagrange.OneBot.exe`（Windows）或 `./Lagrange.OneBot`（Linux/macOS）
  - 首次运行会生成 `appsettings.json` 配置文件
  - 配置 WebSocket 反向连接：
    - `ws_reverse_url`: 设置为 `ws://你的服务器IP:YinwuChat的ws端口/ws`（例如：`ws://127.0.0.1:8888/ws`）
    - `ws_reverse_use_universal_client`: 设置为 `true`
    - `access_token`: 设置为与 YinwuChat 配置中的 `accessToken` 一致
   **选项 B：使用 LLoneBot 后端（LiteLoaderQQNT 插件）**
  - 安装 LiteLoaderQQNT：[https://github.com/LiteLoaderQQNT/LiteLoaderQQNT](https://github.com/LiteLoaderQQNT/LiteLoaderQQNT)
  - 从 [LLoneBot 的 GitHub Releases](https://github.com/jackiotyu/llonebot-docker) 下载 `.zip` 文件
  - 在 QQ 设置中，点击 LiteLoaderQQNT，选择"安装新插件"，选择下载的 `.zip` 文件
  - 重启 QQ，在 LiteLoaderQQNT 设置中启用 LLoneBot
  - 配置正向 WebSocket 或反向 WebSocket（推荐反向）：
    - 启用反向 WebSocket，设置端口和 Access Token
    - 将连接地址设置为 YinwuChat 的 WebSocket 地址
   **选项 C：使用 NapCat 后端**
  - 安装 LiteLoaderQQNT（同上）
  - 从 [NapCat 的 GitHub Releases](https://github.com/NapNeko/NapCatQQ) 下载 `.zip` 文件
  - 安装步骤和 LLoneBot 类似
  - 配置 WebSocket 连接和 Access Token
3. **游戏内使用**
  - 使用 `/qq <消息>` 命令将消息发送到 QQ 群
  - 每条消息之间有 5 秒冷却时间
  - 第一次发送后有 5 秒内无法再次发送

#### 方式二（老方式，由于酷Q开发者不再支持，已弃用）：使用 CoolQ HTTP API

1. YinwuChat插件配置
  1. 需要开启openwsserver
  2. 将coolQGroup设置为你想同步的Q群的号码
  3. 将coolQAccessToken设置为一个足够复杂足够长的字符串（推荐32位左右的随机字符串）
2. 安装酷Q HTTP API插件
  1. 从 [https://github.com/richardchien/coolq-http-api/releases/latest](https://github.com/richardchien/coolq-http-api/releases/latest) 下载最新版本的coolq-http-api，coolq-http-api具体的安装说明可以到 [https://cqhttp.cc/docs/](https://cqhttp.cc/docs/) 或 [http://richardchien.gitee.io/coolq-http-api/docs/](http://richardchien.gitee.io/coolq-http-api/docs/) 查看
  2. 将coolq-http-api放到酷Q目录下的app目录中
  3. 打开酷Q的应用管理界面，点击重载应用按钮
  4. 找到"[未启用]HTTP API"，点它，然后点右边的启用按钮
  5. 有提示的全部点"是"
  6. 到酷Q目录下的"data\app\io.github.richardchien.coolqhttpapi\config"目录，下，打开你登录的QQ号对应的json文件（比如你登录的QQ号是10000，那文件名就是10000.json）
  7. 将use_http修改为false（如果你没有其他应用需要使用的话）
  8. 将use_ws_reverse修改为true（必须！）
  9. 将ws_reverse_url修改为插件的websocket监听地址加端口（比如你端口是9000，酷Q和mc服务器在一台机器上就填 ws://127.0.0.1:9000/ws）
  10. post_message_format请务必保证是"string"
  11. 将enable_heartbeat设置为true
  12. 增加一行  "ws_reverse_use_universal_client": true,    或者如果你的json文件中有ws_reverse_use_universal_client的话将它改为true（必须！）
  13. 将access_token修改为和YinwuChat配置中的coolQAccessToken一致的内容
  14. 右键酷Q主界面，选择应用-HTTP API-重启应用

**注意**：CoolQ 已停止维护，请使用 AQQBot 替代方案。

### 跨Velocity聊天

> 支持公屏聊天、私聊、at等所有功能

1. 将Velocity端配置文件的redisConfig.openRedis修改为true
2. redisConfig.ip修改为redis服务器的ip
3. redisConfig.port修改为redis服务器的端口
4. redisConfig.password修改为redis服务器的密码
5. redisConfig.selfName修改为每个Velocity端都不一样的一个字符串（插件内部标记消息来源及消息目的用，每个Velocity必须不一样，无其他要求）
6. 重新加载插件后，在一个Velocity端接入的玩家发送的消息可以在其他Velocity端接入的玩家处看到

### 配置文件

YinwuChat-Velocity的配置文件内容为：

```yaml
#是否开启WebSocket
openwsserver: false

#WebSocket监听端口
wsport: 8888

#WebSocket发送消息时间间隔（毫秒）
wsCooldown: 1000

#安装了BungeeAdminTools插件时，
#在Web端发送消息，使用哪个服务器作为禁言/ban的服务器
webBATserver: lobby

#@玩家时的冷却时间（秒）
atcooldown: 10

#@全体玩家的关键词
#默认为all，可以使用@all来@所有人
#如果你有一个服务器叫做lobby
#那么可以使用@lobbyall来@lobby服务器的所有人
#@lobbyall可以简写为@lall或@loball等（服务器名前面一部分）
atAllKey: all

#链接识别正则表达式，符合该正则的聊天内容会被替换，并且可以点击
linkRegex: ((https?|ftp|file)://[-A-Za-z0-9+&@#/%?=~_|!:,.;]+[-A-Za-z0-9+&@#/%=~_|])

#聊天屏蔽模式，目前1为将聊天内容替换为shieldedReplace的内容，其他为直接拦截
shieldedMode: 1

#多少秒内总共发送屏蔽关键词shieldedKickCount次就会被踢出服务器（包括web端）
shieldedKickTime: 60

#shieldedKickTime秒内发送屏蔽关键词多少次会被踢出服务器
shieldedKickCount: 3

#配置文件的版本，请勿修改
configVersion: 4

#从web页面发送消息到游戏中时禁用的样式代码
webDenyStyle: klmnor

#聊天内容屏蔽关键词，list格式，你需要自己添加这个设置
shieldeds:
#每个关键词一行
- keyword

tipsConfig:
  shieldedKickTip: 你因为发送屏蔽词语，被踢出服务器
  
  #聊天内容中含有屏蔽关键词时，整个消息会被替换为这个
  shieldedReplace: 富强、民主、文明、和谐、自由、平等、公正、法治、爱国、敬业、诚信、友善
  atyouselfTip: '&c你不能@你自己'
  atyouTip: '&e{player}&b@了你'
  cooldownTip: '&c每次使用@功能之间需要等待10秒'
  ignoreTip: '&c对方忽略了你，并向你仍了一个烤土豆'
  banatTip: '&c对方不想被@，只想安安静静的做一个美男子'
  toPlayerNoOnlineTip: '&c对方不在线，无法发送私聊'
  msgyouselfTip: '&c你不能私聊你自己'
  youismuteTip: '&c你正在禁言中，不能说话'
  youisbanTip: '&c你被ban了，不能说话'
  
  #发送的聊天消息中含有屏蔽的关键词时会收到的提醒
  shieldedTip: '&c发送的信息中有被屏蔽的词语，无法发送，继续发送将被踢出服务器'
  
  #聊天内容中的链接将被替换为这个文本
  linkText: '&7[&f&l链接&r&7]&r'
  
#各种消息的格式化设置
formatConfig:
  #WebSocket发送过来的消息格式化内容，
  #由list构成，每段内容都分message、hover、click 3项设置
  format:
  #直接显示在聊天栏的文字，
  #{displayName}将被替换为玩家名
  #hover和click字段中的{displayName}也会替换
  - message: '&b[Web]'
    #鼠标移动到这段消息上时显示的悬浮内容
    hover: 点击打开YinwuChat网页
    #点击这段消息时的动作，自动识别是否链接，如果是链接则打开链接
    #否则如果是以!开头就执行命令，否则就将内容填充到聊天框
    #比如让看到消息的人点击就直接给发消息的人发送tpa请求，
    #就可以写成 /tpa {displayName}（不写斜杠会按发送消息处理）
    click: https://chat.yinwurealm.org
  - message: '&e{displayName}'
    hover: 点击私聊
    click: /msg {displayName}
  - message: ' &6>>> '
  - message: '&r{message}'
  
  #QQ群群员发送的消息，游戏内展示的样式
  qqFormat:
  - message: '&b[QQ群]'
    hover: 点击加入QQ群xxxxx
    #这里可以替换为你QQ群的申请链接
    click: https://xxxxxx.xxxx.xxx
  - message: '&e{displayName}'
  - message: ' &6>>> '
  - message: '&r{message}'
  
  #私聊时，自己收到的消息的格式
  toFormat:
  - message: '&7我 &6-> '
  - message: '&e{displayName}'
    hover: 点击私聊
    click: /msg {displayName}
  - message: ' &6>>> '
  - message: '&r{message}'
  
  #私聊时，对方收到的消息的格式
  fromFormat:
  - message: '&b[Web]'
    hover: 点击打开YinwuChat网页
    click: https://xxxxxx.xxxx.xxx
  - message: '&e{displayName}'
    hover: 点击私聊
    click: /msg {displayName}
  - message: ' &6-> &7我'
  - message: ' &6>>> '
  - message: '&r{message}'
  
  #其他玩家私聊时，有权限的玩家看到的监听消息的样式
  monitorFormat:
  - message: '&7{formPlayer} &6-> '
  - message: '&e{toPlayer}'
  - message: ' &6>>> '
  - message: '&r{message}'

# AQQBot 配置（推荐使用，支持 Lagrange、LLoneBot、NapCat 等基于 OneBot 标准的后端）
aqqBotConfig:
  # QQ群有新消息时是否发送到游戏内
  qqToGame: true
  
  # QQ群有新消息时，只有开头跟这里一样才发送到游戏中（为空则所有消息都转发）
  qqToGameStart: ''
  
  # 游戏中有新消息时是否发送到QQ群中
  gameToQQ: true
  
  # 游戏中有新消息时，只有开头跟这里一样才发送到QQ群中（为空则所有消息都转发）
  gameToQQStart: ''
  
  # 转发QQ群消息到游戏时禁用的样式代码
  qqDenyStyle: 0-9a-fklmnor
  
  # 监听的QQ群的群号，AQQBot接收到消息时，如果是QQ群，且群号和这里一致，就会转发到游戏中
  qqGroup: 0
  
  # 与 AQQBot WebSocket 通信时使用的 Access Token，为空时不验证，强烈建议设置为一个复杂的字符串
  accessToken: ''
  
  # QQ群中群员发送的@信息将被替换为这个文本
  # {qq}将被替换为被@的人的QQ号
  qqAtText: '&7[@{qq}]&r'
  
  # QQ群中群员发送的图片将被替换为这个文本
  qqImageText: '&7[图片]&r'
  
  # QQ群中群员发送的语音将被替换为这个文本
  qqRecordText: '&7[语音]&r'

# CoolQ HTTP API 配置（旧版，已弃用，保留以向后兼容）
coolQConfig:
  #qq群有新消息时是否发送到游戏内
  coolQQQToGame: true
  
  #qq群有新消息时，只有开头跟这里一样才发送到游戏内
  coolqToGameStart: ''
  
  #游戏中有新消息时是否发送到QQ群中
  coolQGameToQQ: true
  
  #游戏中有新消息时，只有开头跟这里一样才发送到QQ群中
  gameToCoolqStart: ''
  
  #转发QQ群消息到游戏时禁用的样式代码
  qqDenyStyle: 0-9a-fklmnor
  
  #监听的QQ群的群号，酷Q接收到消息时，如果是QQ群，且群号和这里一致，就会转发到游戏中
  coolQGroup: 0
  
  #和酷Q HTTP API插件通信时使用的accesstoken，为空时不验证，强烈建议设置为一个复杂的字符串
  coolQAccessToken: ''
  
  #QQ群中群员发送的@信息将被替换为这个文本
  #{qq}将被替换为被@的人的QQ号
  qqAtText: '&7[@{qq}]&r'
  
  #QQ群中群员发送的图片将被替换为这个文本
  qqImageText: '&7[图片]&r'
  
  #QQ群中群员发送的语音将被替换为这个文本
  qqRecordText: '&7[语音]&r'

#利用redis做跨bc聊天同步的配置
redisConfig:
  #是否开启redis聊天同步
  openRedis: false
  
  #redis服务器的ip地址或域名
  ip: ''
  
  #redis的端口
  port: 0
  
  #一般不要修改
  maxConnection: 8
  
  #redis的密码
  password: ''
  
  #服务器标识，每个bc端的YinwuChat插件的标识请设置为不一样
  selfName: bc1
```

### 接口

本插件所有信息均由WebSocket通信，格式均为JSON格式，具体数据如下：

#### 发往本插件的数据：

1. 检查token

```json
{
    "action": "check_token",
    "token": "待检查的token，token由服务器下发，初次连接时可以使用空字符串"
}
```

1. 发送消息

```json
{
    "action": "send_message",
    "message": "需要发送的消息，注意，格式代码必须使用§"
}
```

#### 发往Web客户端的数据：

1. 更新token（接收到客户端发送的check_token数据，然后检查token失败时下发，收到该数据应提醒玩家在游戏内输入/yinwuchat token title命令绑定token）

```json
{
    "action": "update_token",
    "token": "一个随机的token"
}
```

1. token校验结果（检查token成功后返回，或玩家在游戏内绑定成功后，token对应的WebSocket在线时主动发送，只有接收到了这个数据，且数据中的status为true，且数据中的isbind为true时才可以向服务器发送send_message数据）

```json5
{
    "action": "check_token",
    "status": true,        //表示该token是否有效
    "message": "成功时为success，失败时为原因，并同时发送一个更新token数据",
    "isbind": false         //表示该token是否被玩家绑定
}
```

1. 玩家在游戏内发送了消息

```json
{
    "action": "send_message",
    "message": "消息内容"
}
```

1. 游戏玩家列表

```json
{
    "action": "game_player_list",
    "player_list":[
        {
            "player_name": "玩家游戏名",
            "server_name": "玩家所在服务器"
        }
    ]
}
```

1. WebClient玩家列表

```json
{
    "action": "web_player_list",
    "player_list":[
        "玩家名1",
        "玩家名2"
    ]
}
```

1. 服务器提示消息（一般为和服务器发送数据包后的错误反馈信息）

```json5
{
    "action": "server_message",
    "message": "消息内容",
    "time": 1, //unix时间戳
    "status": 1 //状态码，详情见下方表格(int)
}
```

#### 服务器消息状态码


| 状态码  | 具体含义                       |
| ---- | -------------------------- |
| 0    | 一般成功或提示消息                  |
| 1    | 一般错误消息                     |
| 1001 | 获取历史聊天记录时，内容为空（不可继续获取历史消息） |


### Velocity端命令

#### 禁言命令（管理员）


| 命令                     | 说明     | 权限                              |
| ---------------------- | ------ | ------------------------------- |
| `/mute <玩家> [时长] [原因]` | 禁言玩家   | `yinwuchat.admin.mute`（控制台无需权限） |
| `/unmute <玩家>`         | 解除禁言   | `yinwuchat.admin.mute`（控制台无需权限） |
| `/muteinfo <玩家>`       | 查看禁言信息 | `yinwuchat.admin.mute`（控制台无需权限） |


**时长格式：**

- `1d` - 1天
- `2h` - 2小时
- `30m` - 30分钟
- `60s` 或 `60` - 60秒
- `1d2h30m` - 可组合使用
- 不指定时长则为**永久禁言**

**使用示例：**

```
mute Steve              # 永久禁言
mute Steve 1h           # 禁言1小时
mute Steve 30m 刷屏     # 禁言30分钟，原因：刷屏
mute Steve 1d2h 多次违规  # 禁言1天2小时
unmute Steve            # 解除禁言
muteinfo Steve          # 查看禁言状态
```

#### 其他命令

1. 控制台命令
  - `yinwuchat reload [config|ws]`：重新加载插件配置
2. 游戏内命令
  - `/yinwuchat`：插件帮助
    - `/yinwuchat reload [config|ws]`：重新加载配置文件（需要 `yinwuchat.admin.reload` 权限）
    - `/msg <玩家名> <消息>`：向玩家发送私聊消息
    - `/qq <消息>`：向QQ群发送消息（需配置QQ机器人）
    - `/yinwuchat vanish`：切换聊天系统隐身模式（需要 `yinwuchat.admin.vanish` 权限）
    - `/yinwuchat ignore <玩家名>`：忽略/取消忽略玩家消息（需要 `yinwuchat.default.ignore` 权限）
    - `/yinwuchat noat`：禁止/允许自己被@（需要 `yinwuchat.default.noat` 权限）
    - `/yinwuchat muteat`：切换自己被@时有没有声音（需要 `yinwuchat.default.muteat` 权限）
    - `/yinwuchat format edit`：编辑聊天前后缀（可点击设置或清除）
    - `/yinwuchat format show`：显示当前前后缀
    - `/yinwuchat format <public|private> <prefix|suffix> clear`：直接清除前后缀
    - `/yinwuchat atalladmin`：报告突发事件给所有管理员（需要 `yinwuchat.default.atalladmin` 权限，每日限一次；管理员可点击“确认收到”快捷填入确认指令）
    - `/yinwuchat atalladmin confirm <玩家名>`：重置玩家报告冷却时间（仅管理员）
    - **私聊视监**（需要 `yinwuchat.admin.monitor` 或配置中的管理员）：须先执行 `monitor all` 或 `monitor <玩家名>` 才会收到实时视监推送；仅有权限而未开启视监时不会收到。私聊双方不会收到任何「被视监」提示。
      - `/yinwuchat monitor all`：视监所有玩家的私聊（新消息）
      - `/yinwuchat monitor <玩家名>`：仅视监该玩家作为发送方或接收方的私聊（新消息）
      - `/yinwuchat monitor off`：关闭视监
      - `/yinwuchat monitor menu`：打开「YinwuChat视监菜单」（可点击快捷填入命令）
      - `/yinwuchat monitor history <玩家> [条数]`：从代理端私聊缓存中读取该玩家相关的最近若干条记录（默认 20 条，上限 500）；依赖已写入 `web-private-messages.jsonl` 的消息

### Velocity端权限


| 权限节点                              | 说明                 |
| --------------------------------- | ------------------ |
| `yinwuchat.admin`                 | 通用管理权限（拥有所有管理权限）   |
| `yinwuchat.admin.mute`            | 禁言/解除禁言玩家（控制台默认拥有） |
| `yinwuchat.admin.reload`          | 重新加载配置             |
| `yinwuchat.admin.vanish`          | 聊天隐身模式             |
| `yinwuchat.admin.atall`           | @所有人               |
| `yinwuchat.admin.monitor`         | 使用 `/yinwuchat monitor` 子命令并接收私聊视监推送（须先 `all` / 指定玩家） |
| `yinwuchat.admin.badword`         | 管理屏蔽词              |
| `yinwuchat.admin.cooldown.bypass` | @人无冷却时间            |
| `yinwuchat.default`               | 通用基础权限（拥有所有基础权限）   |
| `yinwuchat.default.ws`            | 查看 WebSocket 地址    |
| `yinwuchat.default.bind`          | 绑定 Web 账号          |
| `yinwuchat.default.list`          | 查看已绑定账号            |
| `yinwuchat.default.unbind`        | 解绑账号               |
| `yinwuchat.default.ignore`        | 忽略玩家消息             |
| `yinwuchat.default.noat`          | 禁止被@               |
| `yinwuchat.default.muteat`        | 被@静音               |
| `yinwuchat.default.format`        | 自定义聊天前后缀           |
| `yinwuchat.default.atalladmin`    | 报告突发事件给管理员         |
| `yinwuchat.style.*`               | 使用聊天样式代码           |


#### 权限组配置建议(LuckPerms)

为了方便管理，建议在 LuckPerms 中创建两个权限组。你可以直接在 LuckPerms 代理端（Velocity/Bungee）或各子服执行以下命令：

**1. 设置管理员组 (admin)**
该组包含 YinwuChat 的所有管理功能：

```bash
/lp creategroup admin
/lp group admin permission set yinwuchat.admin true
/lp group admin permission set yinwuchat.admin.atalladmin.reset true
```

**2. 设置普通玩家组 (default)**
普通玩家可以拥有基础功能权限：

```bash
/lp creategroup default
/lp group default permission set yinwuchat.default true
/lp group default permission set yinwuchat.default.atalladmin true
```

**3. 分配玩家到组**

```bash
# 将某玩家设为管理员
/lp user <玩家名> parent add admin
```

**方式二：使用配置文件 admins 列表**

如果 LuckPerms 权限桥接存在问题（某些 Velocity 快照版本可能出现），可以使用配置文件中的 `admins` 列表来授权管理员。

编辑 `plugins/yinwuchat-velocity/config.yml`：

```yaml
# 管理员列表（绕过权限检查）
# 列表中的玩家自动拥有 vanish、mute、reload 等管理权限
# 玩家名不区分大小写
admins:
  - Xx_Kirov_xX
  - Steve
  - 其他管理员名字
```

修改后执行 `/yinwuchat reload` 或重启 Velocity 生效。

**权限检查优先级：**

1. 控制台 - 自动拥有所有权限
2. LuckPerms 权限节点 - 检查对应权限
3. admins 配置列表 - 列表中的玩家拥有管理权限

---

### Bungeecord端命令（旧版）

1. 控制台命令
  - `yinwuchat reload [config|ws]`：重新加载插件，或仅重新加载配置（在ws配置有变动时自动重启ws），或只重启ws
2. 游戏内命令
  - `/yinwuchat`：插件帮助（其他未识别的命令也都将显示帮助）
    - `/yinwuchat reload [config|ws]`：重新加载配置文件，需要具有`yinwuchat.admin.reload`权限
    - `/yinwuchat bind <token>`：绑定token，需要具有`yinwuchat.default.bind`权限
    - `/yinwuchat list`：列出玩家已绑定的token，需要具有`yinwuchat.default.list`权限
    - `/yinwuchat unbind <token>`：解绑token，需要具有`yinwuchat.default.unbind`权限
    - `/msg <玩家名> <消息>`：向玩家发送私聊消息
    - `/yinwuchat vanish`：切换聊天系统隐身模式，需要具有`yinwuchat.admin.vanish`权限
    - `/yinwuchat ignore <玩家名>`：忽略/取消忽略玩家消息，需要具有`yinwuchat.default.ignore`权限
    - `/yinwuchat noat`：禁止/允许自己被@，需要具有`yinwuchat.default.noat`权限
    - `/yinwuchat muteat`：切换自己被@时有没有声音，需要具有`yinwuchat.default.muteat`权限
    - `/yinwuchat monitor <all|玩家名|off>`：私聊视监（Bungee 端无 `menu`/`history`；完整功能请使用 Velocity）。需要 `yinwuchat.admin.monitor` 或配置管理员；须先 `all` 或指定玩家才会收到推送
3. WebClient命令
  - `/msg <玩家名> <消息>`：向玩家发送私聊消息

补充：

- `/yinwuchat` 与别名 `/yw` 现在支持一级子命令 Tab 补全，并按权限过滤结果
- `/chatban`、`/chatunban` 以及代理出的独立命令（如 `/vanish`、`/noat`、`/muteat`、`/monitor`）会转发到对应的 `/yinwuchat` 子命令补全逻辑
- Bungee 侧已补全 `reload`、`bind`、`list`、`unbind`、`format`、`ignore`、`noat`、`muteat`、`monitor`、`vanish`、`permsync`、`atalladmin`、`webbind`、`badword`、`chatban`、`chatunban`、`reset` 等分支

### Bukkit端权限

`yinwuchat.admin.reload`玩家可以在游戏中使用`/yinwuchat-bukkit reload`命令重新加载bukkit端yinwuchat的配置，默认权限：仅OP可以使用
`yinwuchat.style.x`是否允许玩家使用对应的样式代码，`x`为具体样式代码，具体为`0-9`,`a-f`,`klmnor`共22个样式代码，默认设置时`0-9`,`a-f`,`r`为允许，其他为不允许

### Bungee端权限

- `yinwuchat.admin.reload`玩家可以在游戏中使用`/yinwuchat reload`命令重新加载插件配置
- `yinwuchat.admin.cooldown.bypass`@人没有冷却时间
- `yinwuchat.admin.atall`允许@所有人
- `yinwuchat.admin.vanish`允许进入聊天隐身模式
- `yinwuchat.admin.badword`允许编辑聊天系统关键词列表
- `yinwuchat.admin.monitor`允许使用`/yinwuchat monitor`子命令并接收私聊视监推送（须先执行 `monitor all` 或 `monitor <玩家名>` 才会收到实时内容）
- `yinwuchat.default.`*基础功能权限节点（bind, list, unbind, ignore, noat, muteat, ws）

- 权限需要在Bungeecord中设置，玩家可以在Bungeecord连接到的任何服务器使用这个命令

### @所有人

@所有人可以@整个服务器所有人（不包括WebSocket），或者分服务器@该服务器所有人（不包括WebSocket）
具体使用方法为：
假如配置文件中的`atAllKey`是默认的`all`，那么聊天内容中含有`@all`时即可@整个服务器的人（all后面不能紧接着英文或数字，可以是中文、空格等）
假如你有一个服务器名字为`lobby`，那么聊天内容中含有`@lall`或`@lobbyall`时，即可@lobby服务器的所有人（即服务器名只需要输入前面部分即可，该服务器名为BungeeCord配置文件中的名字）

### 错误信息

有些时候，玩家执行命令的时候可能会碰到一些错误（主要为数据库错误），具体含义为：


| 错误代码 | 具体含义               |
| ---- | ------------------ |
| 001  | 根据UUID查找用户失败，且新增失败 |


### 其他信息

本插件由国内正版Minecraft服务器[YinwuRealm](https://www.yinwurealm.org/)玩家[LinTx](https://mine.ly/LinTx.1)为服务器开发

插件升级由[YinwuRealm]服务组成员[Xx_kirov_xX](https://namemc.com/profile/Xx_Kirov_xX.1)完成