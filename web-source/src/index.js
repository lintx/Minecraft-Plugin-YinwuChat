import Vue from "vue";
import "./index.scss"

const protocol = location.protocol.toLocaleLowerCase() === "https:" ? "wss" : "ws";
const wsurl = protocol +"://" + location.host + "/ws";
const msg_type_err = 2,msg_type_info = 1,msg_type_default = 0,msg_type_server = 3;
function formatMessageHtmlObj(message, type) {
    let obj = {message:message};
    switch (type) {
        case 2:
            obj.type = "error";
            break;
        case 1:
            obj.type = "info";
            break;
        case 3:
            obj.type = "server";
            break;
        default:
            obj.type = "message";
            break;
    }
    return obj;
}

const app = new Vue({
    el:"#app",
    data:{
        message:[],
        chat:{
            text:"",
            history:[],
            historyIndex:0
        },
        login:false,
        player_list : {
            game:[],
            web:[]
        },
        setting : {
            show_time:true,
            show_player_list:false
        }
    },
    methods:{
        onchat(){
            if (this.chat.text.length === 0) {
                return;
            }
            this.chat.history.push(this.chat.text);
            if (this.chat.history.length>100){
                this.chat.history.shift();
            }
            this.chat.historyIndex = this.chat.history.length;
            if (this.login) {
                sendMessage(this.chat.text);
            } else {
                addMessage("你还没有连接到服务器，或者token校验失败，或者token尚未绑定，暂时无法发送消息",msg_type_err);
            }
            this.chat.text = "";
        },
        chatKeyUp (ev){
            if (ev.ctrlKey || ev.shiftKey || ev.altKey) {
                return;
            }
            if (ev.which === 38) {
                if (this.chat.historyIndex > this.chat.history.length){
                    this.chat.historyIndex = this.chat.history.length;
                }
                this.chat.historyIndex -= 1;
                this.chat.text = this.chat.history[this.chat.historyIndex];
            }
            else if (ev.which === 40) {
                if (this.chat.historyIndex < -1){
                    this.chat.historyIndex = -1;
                }
                this.chat.historyIndex += 1;
                this.chat.text = this.chat.history[this.chat.historyIndex];
            }
        },
        setMsgCmd (player) {
            this.chat.text = "/msg " + player + " ";
        }
    }
});

function addMessage(message, type) {
    app.message.push(formatMessageHtmlObj(message,type));
    if (document.scrollingElement.scrollHeight - document.scrollingElement.scrollTop - document.documentElement.clientHeight <= 100){
        app.$nextTick(()=>{
            document.scrollingElement.scrollTop = document.scrollingElement.scrollHeight;
        });
    }
}
function insMessage(message, type) {
    app.message.splice(0,0,formatMessageHtmlObj(message,type));
}

function notWebSocket() {
    addMessage("看起来你的浏览器不支持WebSocket，YinwuChat的运行依赖于WebSocket，你需要一个支持WebSocket的浏览器，比如Chrome，才能正常使用。",msg_type_err);
}

if (typeof WebSocket !== "function" && typeof MozWebSocket !== "function") {
    notWebSocket();
}

let ws;

const WsHelper = {
    timeout:2000,
    heardCheckTimeout:60000,
    heardCheckTimeoutObj: null,
    heardCheckReset: function(){
        clearInterval(this.heardCheckTimeoutObj);
        this.heardCheckStart();
    },
    heardCheckStart: function(){
        this.heardCheckTimeoutObj = setInterval(function(){
            if(ws.readyState===1){
                ws.send("HeartBeat");
            }
        }, this.heardCheckTimeout)
    },

    lockReconnect:false,
    start:function () {
        const self = this;
        if (this.lockReconnect) return;
        this.lockReconnect = true;
        setTimeout(()=>{
            self.lockReconnect = false;
            self.create();
        },this.timeout);
    },
    create:function () {
        try {
            if ('WebSocket' in window){
                ws = new WebSocket(wsurl);
            }
            else if ('MozWebSocket' in window) {
                ws = new MozWebSocket(wsurl);
            }
            this.bindEvent();
        }
        catch (e) {
            notWebSocket();
            this.start();
        }
    },
    bindEvent:function () {
        const self = this;
        ws.onopen = function(){
            addMessage("连接服务器成功，正在校验token",msg_type_info);
            sendCheckToken(getToken());
            self.heardCheckStart();
        };

        ws.onmessage = function(e){
            self.heardCheckReset();
            let json = e.data;
            try {
                let data = JSON.parse(json);
                switch (data.action) {
                    case "update_token":
                        updateToken(data.token);
                        break;
                    case "check_token":
                        checkToken(data.status,data.isbind,data.message);
                        break;
                    case "send_message":
                        onMessage(data.message);
                        break;
                    case "player_join":
                    case "player_leave":
                    case "player_switch_server":
                        onPlayerStatusMessage(data.player,data.server,data.action);
                        break;
                    case "player_web_join":
                    case "player_web_leave":
                        onWebPlayerStatusMessage(data.player,data.action);
                        break;
                    case "server_message":
                        onServerMessage(data.message,data.status);
                        break;
                    case "game_player_list":
                        this.player_list.game = data.player_list;
                        break;
                    case "web_player_list":
                        this.player_list.web = data.player_list;
                        break;
                }
            }
            catch (e) {
                console.error(e);
            }
        };
        ws.onclose = function(e){
            addMessage("WebSocket断开了连接",msg_type_info);
        };
        ws.onerror = function (err) {
        };
    }
};


addMessage("正在连接服务器",msg_type_info);
WsHelper.create();

function getToken(){
    let token = localStorage.getItem("yinwuchat_token");
    if (typeof token !== "string") {
        token = "";
    }
    return token;
}

function saveToken(token){
    localStorage.setItem("yinwuchat_token",token);
}

function sendCheckToken(token){
    let obj = {
        action:"check_token",
        token:token
    };
    ws.send(JSON.stringify(obj));
}

function sendMessage(message,status){
    message = message.replace(/&([0-9abcdef])([^&]*)/ig, (regex, color, msg) => {
        return "§" + color + msg;
    });

    message = message.replace(/&([klmnor])([^&]*)/ig, (regex, style, msg) => {
        return msg;
    });

    message = message.replace(/§([klmnor])([^§]*)/ig, (regex, style, msg) => {
        return msg;
    });

    let obj = {
        action:"send_message",
        message:message
    };
    ws.send(JSON.stringify(obj));
}

function addBindMsg(token) {
    addMessage("请进入游戏，并<span class='badge badge-warning'>在游戏内输入命令</span><span class='badge badge-light'>/yinwuchat bind " + token + "</span>以绑定token。",msg_type_info);
}

function updateToken(token){
    saveToken(token);
    addBindMsg(token);
}

function checkToken(status,isbind,message){
    if (!status) {
        addMessage(message,msg_type_err);
    }
    else {
        if (isbind) {
            addMessage("token校验成功，你现在可以发送消息到游戏内了",msg_type_info);
            app.login = true;
        }
        else {
            addBindMsg(getToken());
        }
    }
}

function onMessage(message){
    message = formatMessage(message);
    addMessage(message,msg_type_default);
}

// function getClickPlayer(player) {
//     return "<span class='cursor-hand' title='点击向"+player+"发送私聊消息' @click='setMsgCmd(\""+player+"\")'>"+player+"</span>"
// }

function onServerMessage(message,status){
    message = formatMessage(message);
    if (status === 1001) {
        insMessage(message,msg_type_server);
    } else {
        addMessage(message,msg_type_server);
    }
}

function formatMessage(message) {
    message = message.replace(/&([0-9abcdefklmnor])([^&]*)/ig, (regex, style, msg) => {
        return "§" + style + msg;
    });

    // message = message.replace(/&([klmnor])([^&]*)/ig, (regex, style, msg) => {
    //     return msg;
    // });

    // message = message.replace(/§([klmnor])([^§]*)/ig, (regex, style, msg) => {
    //     return msg;
    // });

    message = message.replace(/§([0-9abcdef])([^§]*)/ig, (regex, color, msg) => {
        //msg = msg.replace(/ /g, '&nbsp;');
        return `<span class="color-${color}">${msg}</span>`;
    });

    message = message.replace(/§([klmnor])([^§]*)/ig, (regex, style, msg) => {
        //msg = msg.replace(/ /g, '&nbsp;');
        return `<span class="yinwuchat-chat-style-${style}">${msg}</span>`;
    });
    return message;
}

function onPlayerStatusMessage(player,server,status){
    let message = "";
    switch (status) {
        case "player_join":
            message = "§6玩家§e" + player + "§6";
            message += "加入了游戏";
            if (server.length > 0) {
                message += "，所在服务器：§b" + server;
            }
            break;
        case "player_leave":
            message = "§6玩家§e" + player + "§6";
            message += "退出了游戏";
            break;
        case "player_switch_server":
            message = "§6玩家§e" + player + "§6";
            message += "加入了服务器：§b" + server;
            break;
    }
    message = formatMessage(message);
    addMessage(message,msg_type_default);
}

function onWebPlayerStatusMessage(player,status){
    let message = "";
    switch (status) {
        case "player_web_join":
            message = "§6玩家§e" + player + "§6";
            message += "加入了YinwuChat";
            break;
        case "player_web_leave":
            message = "§6玩家§e" + player + "§6";
            message += "离开了YinwuChat";
            break;
    }
    message = formatMessage(message);
    addMessage(message,msg_type_default);
}