# YinwuChat 说明文档

### 关于YinwuChat
YinwuChat同时是Bungeecord插件和Spigot插件，主要功能有。
1. 跨服聊天同步
2. 跨服私聊（`/msg <玩家名> 消息`）
3. 跨服@（聊天内容中输入想@的玩家的名字，或名字的前面一部分，不区分大小写）
4. 跨服物品展示（聊天内容中输入`[i]`即可将手中的物品发送到聊天栏，输入`[i:x]`可以展示背包中x对应的物品栏的物品，物品栏为0-8，然后从背包左上角从左至右从上至下为9-35，装备栏为36-39，副手为40，一条消息中可以展示多个物品）
5. WebSocket，开启WebSocket后配合YinwuChat-Web（Web客户端）可以实现web、游戏内聊天同步
6. 关键词屏蔽
7. 使用酷Q和酷Q HTTP API来实现Q群聊天同步

注：你需要在你的Bungee服务端和这个Bungee接入的所有的Spigot服务端都安装这个插件


### Q群聊天同步
1. YinwuChat插件配置
    1. 需要开启openwsserver
    2. 将coolQGroup设置为你想同步的Q群的号码
    3. 将coolQAccessToken设置为一个足够复杂足够长的字符串（推荐32位左右的随机字符串）
2. 安装酷Q HTTP API插件
    1. 去 https://github.com/richardchien/coolq-http-api/releases/latest 下载最新版本的coolq-http-api，coolq-http-api具体的安装说明可以到 https://cqhttp.cc/docs/ 或 http://richardchien.gitee.io/coolq-http-api/docs/ 查看
    2. 将coolq-http-api放到酷Q目录下的app目录下
    3. 打开酷Q的应用管理界面，点击重载应用按钮
    4. 找到“[未启用]HTTP API”，点它，然后点右边的启用按钮
    5. 有提示的全部点“是”
    6. 到酷Q目录下的“data\app\io.github.richardchien.coolqhttpapi\config”目录，下，打开你登录的QQ号对应的json文件（比如你登录的QQ号是10000，那文件名就是10000.json）
    7. 将use_http修改为false（如果你没有其他应用需要使用的话）
    8. 将use_ws_reverse修改为true（必须！）
    9. 将ws_reverse_url修改为插件的websocket监听地址加端口（比如你端口是9000，酷Q和mc服务器在一台机器上就填 ws://127.0.0.1:9000/）
    10. post_message_format请务必保证是"string"
    11. 将enable_heartbeat设置为true
    12. 增加一行   "ws_reverse_use_universal_client": true,    或者如果你的json文件中有ws_reverse_use_universal_client的话将它改为true（必须！）
    13. 将access_token修改为和YinwuChat配置中的coolQAccessToken一致的内容
    14. 右键酷Q主界面，选择应用-HTTP API-重启应用

### 配置文件
YinwuChat-Bungeecord的配置文件内容为：

```yaml
openwsserver: false   #是否开启WebSocket
wsport: 8888          #WebSocket监听端口
wsCooldown: 1000      #WebSocket发送消息时间间隔（毫秒）
webBATserver: lobby   #安装了BungeeAdminTools插件时，在WebSocket发送消息，使用哪个服务器作为禁言/ban的服务器
format:               #WebSocket发送过来的消息格式化内容，由list构成，每段内容都分message、hover、click 3项设置
- message: '&7[Web]'  #直接显示在聊天栏的文字，{displayName}将被替换为玩家名（包括hover和click字段）
  hover: 点击打开YinwuChat网页版           #鼠标移动到这段消息上时显示的悬浮内容
  click: https://xxxxxx.xxxx.xxx         #点击这段消息时的动作，自动识别是否链接，如果是链接则打开链接，否则就将内容填充到聊天框
- message: '&e{displayName}'
  hover: 点击私聊
  click: /msg {displayName}
- message: ' &6>>> '
- message: '&r{message}'
qqFormat:             #QQ群群员发送的消息，游戏内展示的样式
- message: '&b[QQ群]'
  hover: 点击加入QQ群xxxxx
  click: https://xxxxxx.xxxx.xxx   #这里可以替换为你QQ群的申请链接
- message: '&e{displayName}'
  hover: 点击私聊
  click: /msg {displayName}
- message: ' &6>>> '
- message: '&r{message}'
toFormat:             #私聊时，对方收到的消息的格式
- message: '&7[Web]'
  hover: 点击打开YinwuChat网页
  click: https://chat.yinwurealm.org
- message: '&e{displayName}'
  hover: 点击私聊
  click: /msg {displayName}
- message: ' &6-> &7我'
- message: ' &6>>> '
- message: '&r{message}'
fromFormat:           #私聊时，自己收到的消息的格式
- message: '&7我 &6-> '
- message: '&e{displayName}'
  hover: 点击私聊
  click: /msg {displayName}
- message: ' &6>>> '
- message: '&r{message}'
atcooldown: 10        #@玩家时的冷却时间（秒）
atAllKey: all         #@全体玩家的关键词
linkRegex: ((https?|ftp|file)://[-A-Za-z0-9+&@#/%?=~_|!:,.;]+[-A-Za-z0-9+&@#/%=~_|])      #链接识别正则表达式
linkText: '&7[&f&l链接&r&7]&r'      #聊天内容中的链接将被替换为这个文本    #链接识别正则表达式
qqImageText: '&7[图片]&r'           #QQ群中群员发送的图片将被替换为这个文本
qqRecordText: '&7[语音]&r'      #QQ群中群员发送的语音将被替换为这个文本
qqAtText: '&7[@{qq}]&r'      #QQ群中群员发送的@信息将被替换为这个文本，{qq}将被替换为被@的人的QQ号
atyouselfTip: '&c你不能@你自己'
atyouTip: '&e{player}&b@了你'
cooldownTip: '&c每次使用@功能之间需要等待10秒'
ignoreTip: '&c对方忽略了你，并向你仍了一个烤土豆'
banatTip: '&c对方不想被@，只想安安静静的做一个美男子'
toPlayerNoOnlineTip: '&c对方不在线，无法发送私聊'
msgyouselfTip: '&c你不能私聊你自己'
youismuteTip: '&c你正在禁言中，不能说话'
youisbanTip: '&c你被ban了，不能说话'
shieldedTip: '&c发送的信息中有被屏蔽的词语，无法发送，继续发送将被踢出服务器'        #发送的聊天消息中含有屏蔽的关键词时会收到的提醒
shieldeds:          #聊天内容屏蔽关键词，list格式
- keyword
shieldedMode: 1     #聊天屏蔽模式，目前1为将聊天内容替换为shieldedReplace的内容，其他为直接拦截
shieldedReplace: 富强、民主、文明、和谐、自由、平等、公正、法治、爱国、敬业、诚信、友善
shieldedKickTime: 60      #多少秒内总共发送屏蔽关键词`shieldedKickCount`次就会被踢出服务器(包括web端)
shieldedKickCount: 3      #`shieldedKickTime`秒内发送屏蔽关键词多少次会被踢出服务器
shieldedKickTip: 你因为发送屏蔽词语，被踢出服务器     #发送屏蔽次达到次数后被踢出服务器时的提示语
coolQGroup: 0     #监听的QQ群的群号，酷Q接收到消息时，如果是QQ群，且群号和这里一致，就会转发到游戏中
coolQAccessToken: ''     #和酷Q HTTP API插件通信时使用的accesstoken，为空时不验证，强烈建议设置为一个复杂的字符串
```
`webBATserver`可以实现WebSocket端的禁言（当你的服务器安装了BungeeAdminTools时，玩家在WebSocket发送信息，会以这个项目的内容作为玩家所在服务器，
去BungeeAdminTools查询该玩家是否被禁言或被ban，当他被禁言或被ban时无法说话，由于BungeeAdminTools禁言、ban人只能选择Bungee的配置文件中实际存在的服务器，
所以这里需要填一个实际存在的服务器的名字，建议使用大厅服的名字）

Bungee-Task配置文件(tasks.yml):
```yaml
tasks:
- enable: true    #是否开启这个任务
  interval: 30    #任务间隔时间
  list:           #格式和Bungee的配置文件中的消息格式一致
  - message: '&e[帮助]'
    hover: 服务器帮助文档
    click: ''
  - message: '&r 在聊天中输入'
  - message: '&b[i]'
    hover: 在聊天文本中包含这三个字符即可
    click: ''
  - message: '&r可以展示你手中的物品，输入'
  - message: '&b[i:x]'
    hover: |-
      &b:&r冒号不区分中英文
      &bx&r为背包格子编号
      物品栏为0-8，然后从背包左上角
      从左至右从上至下为9-35
      装备栏为36-39，副手为40
    click: ''
  - message: '&r可以展示背包中x位置对应的物品，一条消息中可以展示多个物品'
  server: all     #任务对应的服务器，不区分大小写，只有对应的服务器的玩家才会收到消息，为"all"时所有服务器都会广播，为"web"时只有web端才会收到通知
```

YinwuChat-Spigot的配置文件内容为：

```yaml
format:         #格式和Bungee的配置文件中的消息格式一致，但是这里的内容支持PlaceholderAPI变量
- message: '&b[%player_server%]'
  hover: 所在服务器：%player_server%
  click: /server %player_server%
- message: '&e{displayName}'
  hover: 点击私聊
  click: /msg {displayName}
- message: ' &6>>> '
- message: '&r{message}'
toFormat:
- message: '&b[%player_server%]'
  hover: 所在服务器：%player_server%
  click: /server %player_server%
- message: '&e{displayName}'
  hover: 点击私聊
  click: /msg {displayName}
- message: ' &6-> &7我'
- message: '&r{message}'
fromFormat:
- message: '&7我 &6-> '
- message: '&e{displayName}'
  hover: 点击私聊
  click: /msg {displayName}
- message: '&r{message}'
eventDelayTime: 50    #接收消息处理延时，单位为毫秒，用于处理部分需要使用聊天栏信息来交互的插件的运行（比如箱子商店等），延时时间就是等待其他插件处理的时间
```


### 接口

本插件所有信息均由WebSocket通信，格式均为JSON格式，具体数据如下：
#### 发往本插件的数据：
1. 检查token
```js
{
    "action": "check_token",
    "token": "待检查的token，token由服务器下发，初次连接时可以使用空字符串"
}
```
2. 发送消息
```js
{
    "action": "send_message",
    "message": "需要发送的消息，注意，格式代码必须使用§"
}
```

#### 发往Web客户端的数据：
1. 更新token（接收到客户端发送的check_token数据，然后检查token失败时下发，收到该数据应提醒玩家在游戏内输入/yinwuchat token title命令绑定token）
```js
{
    "action": "update_token",
    "token": "一个随机的token"
}
```
2. token校验结果（检查token成功后返回，或玩家在游戏内绑定成功后，token对应的WebSocket在线时主动发送，只有接收到了这个数据，且数据中的status为true，且数据中的isbind为true时才可以向服务器发送send_message数据）
```js
{
    "action": "check_token",
    "status": true/false,        //表示该token是否有效
    "message": "成功时为success，失败时为原因，并同时发送一个更新token数据",
    "isbind": false/true         //表示该token是否被玩家绑定
}
```
3. 玩家在游戏内发送了消息
```js
{
    "action": "send_message",
    "message": "消息内容"
}
```
4. 游戏玩家列表
```js
{
    "action": "game_player_list",
    "player_list":[
        {
            "player_name": "玩家游戏名",
            "server_name": "玩家所在服务器"
        },
        ……
    ]
}
```
5. WebClient玩家列表
```js
{
    "action": "web_player_list",
    "player_list":[
        "玩家名1",
        "玩家名2",
        ……
    ]
}
```
6. 服务器提示消息（一般为和服务器发送数据包后的错误反馈信息）
```js
{
    "action": "server_message",
    "message": "消息内容",
    "time": unix时间戳,
    "status": 状态码，详情见下方表格(int)
}
```

#### 服务器消息状态码
状态码|具体含义
-:|-
0|一般成功或提示消息
1|一般错误消息
1001|获取历史聊天记录时，内容为空（不可继续获取历史消息）

### Bungeecord端命令
1. 控制台命令
    - `yinwuchat reload [config|ws]`：重新加载插件，或仅重新加载配置（在ws配置有变动时自动重启ws），或只重启ws
2. 游戏内命令
    - `/yinwuchat`：插件帮助（其他未识别的命令也都将显示帮助）
    - `/yinwuchat reload [config|ws]`：重新加载配置文件，执行这个命令需要你具有`yinwuchat.reload`权限
    - `/yinwuchat bind <token>`：绑定token，`token`是插件下发给web客户端的，玩家从web客户端获取token后到游戏内使用命令将玩家和token进行绑定
    - `/yinwuchat list`：列出玩家已绑定的token
    - `/yinwuchat unbind <token>`：解绑token，当你需要解绑某个token时使用（如在公共场合绑定了token，或者不想用这个token了等），`token`为使用`list`命令时查询到的`token`，可以只输入前面部分文字
    - `/msg <玩家名> <消息>`：向玩家发送私聊消息
    - `/yinwuchat vanish`：切换聊天系统隐身模式（无法被@，无法被私聊，web端无法看见在线，需要有`yinwuchat.vanish`权限）
    - `/yinwuchat ignore <玩家名>`：忽略/取消忽略玩家消息
    - `/yinwuchat noat`：禁止/允许自己被@（@全体除外）
    - `/yinwuchat muteat`：切换自己被@时有没有声音
3. WebClient命令
    - `/msg <玩家名> <消息>`：向玩家发送私聊消息

### 权限
- `yinwuchat.reload`玩家可以在游戏中使用`/yinwuchat reload`命令重新加载插件配置
- `yinwuchat.cooldown.bypass`@人没有冷却时间
- `yinwuchat.atall`允许@所有人
- `yinwuchat.vanish`允许进入聊天隐身模式
- `yinwuchat.badword`允许编辑聊天系统关键词列表
* 权限需要在Bungeecord中设置，玩家可以在Bungeecord连接到的任何服务器使用这个命令

### @所有人
@所有人可以@整个服务器所有人（不包括WebSocket），或者分服务器@该服务器所有人（不包括WebSocket）
具体使用方法为：
假如配置文件中的`atAllKey`是默认的`all`，那么聊天内容中含有`@all`时即可@整个服务器的人（all后面不能紧接着英文或数字，可以是中文、空格等）
假如你有一个服务器名字为`lobby`，那么聊天内容中含有`@lall`或`@lobbyall`时，即可@lobby服务器的所有人（即服务器名只需要输入前面部分即可，该服务器名为BungeeCord配置文件中的名字）

### 错误信息
有些时候，玩家执行命令的时候可能会碰到一些错误（主要为数据库错误），具体含义为：

错误代码|具体含义
-:|-
001|根据UUID查找用户失败，且新增失败

### 其他信息
本插件由国内正版Minecraft服务器[YinwuRealm](https://www.yinwurealm.org/)玩家[LinTx](https://mine.ly/LinTx.1)为服务器开发

### 更新记录
2019-04-16 1.0.2版本：
    1.修复发送的消息只有链接，没有其他内容时，无法识别链接的bug
    2.关键词屏蔽忽略大小写、忽略样式代码、忽略空格（之前添加到屏蔽词库的关键词，需要手动修改为小写，否则将无法生效）
    3.修复一些其他bug
    
2019-04-13 1.0.1版本：
    1.修改web端玩家排序方式
    2.修复bc端插件默认的私聊信息格式不对的问题
    3.增加发送屏蔽词可以踢出服务器的设置
    
2019-04-12 1.0.0版本