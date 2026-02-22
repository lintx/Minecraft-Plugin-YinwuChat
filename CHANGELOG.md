# 更新日志 (CHANGELOG)

本文件记录项目各版本的重要更新与修复，便于在 GitHub 分支或发布页查看变更内容。

## [Unreleased]
- 尚未整理的改动请先记录在此

## [2.12.76] - 2026-02-20
### 新功能
- 新增 Web 端"重置 Token"功能，支持一键清空账号下所有已绑定 Token 并强制重新在游戏内绑定
- 服务器广播消息改为置顶横幅展示，支持多条不同广播折叠查看，不再出现在公屏聊天流中

### 修复
- 修复 Web 端玩家发送消息时游戏内服务器前缀显示为玩家当前游戏服务器而非"Web"的问题
- 修复新生成的 Token 在游戏内绑定时提示"Token 无效"的问题（输入净化、持久化修正）
- 修复 WebSocket 路径路由不匹配导致 Web 端连接到错误后端的问题，新增 Netty 管线 URI 路径重写（`/new-ws`、`/new-chat/ws` → `/ws`）
- 修复 `TokenManager.removeUuid()` 仅删除最新 Token 而非全部 Token 的问题，现在会清除同一 UUID 下所有关联 Token
- 修复 `TokenManager.bindToken()` 未清理旧 Token 导致 Token 累积的问题

### 优化
- 反代环境下 WebSocket 信息接口 (`/api/wsinfo`) 返回 `/new-chat/ws` 路径，与 Nginx 代理配置对齐
- 重置 Token 后自动清除前端缓存的 WebSocket 地址，强制重新发现正确连接路径
- 清理所有临时诊断日志（`System.out.println`），保持控制台输出干净

### English Summary
- Added "Reset Token" feature on Web client to clear all bound tokens and force re-binding in-game
- Server broadcasts now display as sticky top banners (collapsible, multi-broadcast) instead of inline chat messages
- Fixed web player messages showing wrong server prefix in-game (now correctly shows "Web")
- Fixed token binding failures: input sanitization, persistence, and WebSocket path routing issues
- Fixed TokenManager to remove ALL tokens for a UUID (not just the latest) and clean up old tokens on rebind
- Added Netty pipeline URI rewrite handler for `/new-ws` and `/new-chat/ws` paths
- Cleaned up all diagnostic logging

## [2.12.75] - 2026-02-20
### 修复
- 修复 Folia 服务端因 Bukkit Scheduler 不兼容导致插件启动失败的问题（`UnsupportedOperationException`）
- 新增 `SchedulerUtil` 调度工具类，通过反射自动识别 Folia/Bukkit 环境并选择对应的任务调度器
- `ItemDisplayCache` 缓存清理任务改用 `SchedulerUtil`，兼容 Folia 的 `AsyncScheduler`
- `ViewItemCommand` 物品展示界面改用 Folia 的 `EntityScheduler`（区域线程），确保跨区块/跨服玩家可正常打开展示界面
- 超时清理任务改用异步调度，兼容 Folia 异步任务机制

## [2.12.74] - 2026-02-18
### 新功能
- 新增服务器端消息缓存与重放，Web/App 用户上线后自动补发离线消息
- 新增未读消息分割线定位

### 修复
- 修复 App 端消息重复与 WebSocket 连接问题

### 优化
- 内嵌 HarmonyOS Sans 字体统一显示
- 优化图标自动渲染与 Velocity 控制台日志

## [2.12.73] - 2026-02-13
### 新功能
- 增加了 Web 端物品展示功能，现在可以在游戏内向玩家和 Web 端玩家展示物品

### 修复
- 修正服务器反代环境下 App 无法连接服务器的问题，现在可以正常连接

## [2.12.72] - 2026-01-20
### 新功能
- Web/App 指令输入新增快捷选人面板（显示在线状态并自动补全）

### 优化
- 移动端玩家列表宽度与手势交互优化（任意位置滑动触发）
- 物品展示 [i] 的悬停改为原版物品展示格式（Velocity 端）
- /yinwuchat 帮助与 README 指令说明整理

## [2.12.71] - 2026-01-20
### 新功能
- 支持 Web 端与 APP 同时使用同一账号登录，消息实时同步（多端同时在线）
- 头像改用 `/helm/` 端点，显示双层皮肤（底层+头盔层）
- 新增 `/chatunban` 解封指令，支持解除 `/chatban` 的封禁
- 管理员快捷指令新增 `/chatunban`

### 优化
- 绑定成功消息只在游戏内 `bind` 指令成功时发送，Web 端连接时不再重复提示
- Web 端违禁词过多改为封禁1小时（游戏端仍为踢出服务器）

### English Summary
- Multi-device login: Web and APP can now login simultaneously with same account
- Avatar uses `/helm/` endpoint for dual-layer skin display
- Added `/chatunban` command to unban players
- Bind success message only sent on in-game bind command
- Web excessive shielded words now results in 1-hour ban instead of kick

## [2.12.70]
- web端UI 优化：移除私聊窗口顶部头像图标，精简界面
- Web 端侧边栏交互优化：支持手机端左侧侧滑呼出/关闭玩家列表，实现平滑跟随效果
- 修复 Android App 手势兼容性
- 优化物品展示功能[i]：实现点击[i]后展示物品的功能
- 优化权限组，支持lp等插件整理命令权限

## [2.12.69]
- Web 端隐身功能上线，支持独立 `/vanish` 指令
- 优化 Web 端指令权限控制与白名单机制
- 更新 Web 端系统头像
- 完善忽略（Ignore）逻辑
- 支持web端 `/ignore` 和 `/noat` 指令

## [2.12.68]
- 上线 APP debug 版本，支持安卓端

## [2.12.67]
- 更新网页端公共聊天室消息显示兼容处理
- 完善 WebSocket 消息解析与颜色码渲染
- 实现 Web 端指令集成

## [2.12.66]
- 新增velocity端禁言、隐身、重加载插件功能
- 支持定时禁言、永久禁言、禁言原因
- 新增独立命令 `/mute`、`/unmute`、`/muteinfo`
- 优化 `[p]` 位置显示功能
- 修复自定义前后缀与占位符冲突问题
- 服内聊天功能全部上线

## [2.12.65]
- 加入 Folia 支持（测试中），Bukkit/Paper 测试正常

## [2.12.64]
- 前后缀编辑上线，优化编辑功能
- 支持编辑、删除、快捷指令导入
- 加入私聊前后缀

## [2.12.63]
- Redis 依赖转向 Velocity
- `@` 与 `noat` 功能上线

## [2.12.62]
- 修复玩家名称 hover 占位符与 `/msg` 无法调用玩家名称的问题
- 基本完成 YinwuChat 跨服通讯功能搭建

## [2.12.61]
- 消除上版本存在的消息重复发送问题

## [2.12.60]
- 更新版本号配置
- 解决私聊格式问题
