# YinwuChat 鸿蒙端

这是 YinwuChat 的 HarmonyOS NEXT 客户端，使用 Web 组件封装方案实现。

## 环境要求

- **DevEco Studio**: 5.0.0 或更高版本
- **HarmonyOS SDK**: API 12 (5.0.0) 或更高版本
- **目标设备**: HarmonyOS NEXT 手机或模拟器

## 项目结构

```
harmonyos-app/
├── AppScope/                          # 应用级配置
│   ├── app.json5                      # 应用配置（包名、版本等）
│   └── resources/base/media/          # 应用图标
├── entry/                             # 主模块
│   ├── src/main/
│   │   ├── ets/
│   │   │   ├── entryability/          # Ability 入口
│   │   │   │   └── EntryAbility.ets
│   │   │   └── pages/                 # 页面
│   │   │       └── Index.ets          # 主页面（Web 组件）
│   │   ├── resources/
│   │   │   ├── base/
│   │   │   │   ├── element/           # 字符串、颜色等资源
│   │   │   │   ├── media/             # 图片资源
│   │   │   │   └── profile/           # 页面路由配置
│   │   │   └── rawfile/               # Web 资源文件
│   │   │       ├── index.html         # YinwuChat 网页
│   │   │       ├── forge.min.js       # 加密库
│   │   │       ├── logo.png           # Logo
│   │   │       └── avater.png         # 默认头像
│   │   └── module.json5               # 模块配置
│   └── build-profile.json5
├── build-profile.json5                # 构建配置
├── hvigorfile.ts                      # 构建脚本
└── oh-package.json5                   # 包配置
```

## 构建步骤

### 1. 安装 DevEco Studio

1. 访问 [华为开发者联盟](https://developer.huawei.com/consumer/cn/deveco-studio/)
2. 下载并安装最新版 DevEco Studio
3. 首次启动时，按提示安装 HarmonyOS SDK

### 2. 打开项目

1. 启动 DevEco Studio
2. 选择 **File** → **Open**
3. 选择 `harmonyos-app` 文件夹
4. 等待项目同步完成（首次可能需要下载依赖）

### 3. 配置签名（调试包）

1. 点击 **File** → **Project Structure**
2. 选择 **Signing Configs**
3. 勾选 **Automatically generate signature**
4. 登录华为账号（如提示）
5. 点击 **Apply** → **OK**

### 4. 构建调试包

#### 方式一：直接运行（推荐）

1. 连接鸿蒙真机或启动模拟器
2. 点击工具栏的 **Run** 按钮（绿色三角形）
3. 选择目标设备
4. 等待构建和安装完成

#### 方式二：生成 HAP 文件

1. 点击 **Build** → **Build Hap(s)/APP(s)** → **Build Hap(s)**
2. 等待构建完成
3. 产物路径：`entry/build/default/outputs/hap/debug/entry-default-signed.hap`

### 5. 安装到设备

如果使用方式二生成了 HAP 文件：

```bash
# 使用 hdc 工具安装
hdc install entry-default-signed.hap
```

## 功能特性

- ✅ 完整的 YinwuChat Web 功能
- ✅ WebSocket 实时通信
- ✅ 本地存储（登录信息缓存）
- ✅ 网络图片加载（玩家头像）
- ✅ 系统返回键拦截（优先处理网页后退）
- ✅ 适配鸿蒙 UI 风格

## 注意事项

### WebSocket 连接

- **生产环境**：鸿蒙系统要求使用 `wss://`（加密 WebSocket）
- **测试环境**：如需使用 `ws://`，需确保服务器在本地或内网

### 网络权限

项目已配置以下权限：
- `ohos.permission.INTERNET` - 网络访问
- `ohos.permission.GET_NETWORK_INFO` - 获取网络状态

### 手势冲突

- 鸿蒙系统有全局侧滑返回手势
- 应用内的左滑隐藏玩家列表与系统手势可能冲突
- 已在代码中通过 `onBackPress` 进行拦截处理

## 更新 Web 资源

当 YinwuChat 的 Web 端有更新时，需要同步更新鸿蒙项目的资源：

```powershell
# 在 Minecraft-Plugin-YinwuChat 目录下执行
Copy-Item "mobile-app\www\*" -Destination "harmonyos-app\entry\src\main\resources\rawfile\" -Force
```

然后重新构建项目。

## 发布包构建

发布到华为应用市场需要：

1. 申请发布证书（.cer）
2. 申请发布 Profile（.p7b）
3. 在 **Project Structure** → **Signing Configs** 中配置正式签名
4. 使用 **Build** → **Build Hap(s)/APP(s)** → **Build APP(s)** 生成 `.app` 文件

详细流程请参考 [华为开发者文档](https://developer.huawei.com/consumer/cn/doc/harmonyos-guides/ide-signing-0000001587684945)。

## 版本信息

- 应用版本：2.12.76
- 版本代码：2127600
- 包名：com.lintx.yinwuchat
- 最低 API：12 (HarmonyOS 5.0.0)
