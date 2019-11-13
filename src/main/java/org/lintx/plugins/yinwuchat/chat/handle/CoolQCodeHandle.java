package org.lintx.plugins.yinwuchat.chat.handle;

import net.md_5.bungee.api.chat.TextComponent;
import org.lintx.plugins.yinwuchat.Util.MessageUtil;
import org.lintx.plugins.yinwuchat.bungee.config.Config;
import org.lintx.plugins.yinwuchat.chat.struct.Chat;
import org.lintx.plugins.yinwuchat.chat.struct.ChatSource;

public class CoolQCodeHandle extends ChatHandle {
    @Override
    public void handle(Chat chat) {
        if (chat.source!= ChatSource.QQ) return;
        Config config = Config.getInstance();
        String regexp = "\\[CQ:(.*?),(.*?)]";
        handle(chat, regexp, (matcher) -> {
            String func = matcher.group(1);
            String ext = matcher.group(2);

            TextComponent component = new TextComponent();
            if (func.equalsIgnoreCase("image")){
                component.setText(MessageUtil.replace(config.qqImageText));
            }else if (func.equalsIgnoreCase("record")){
                component.setText(MessageUtil.replace(config.qqRecordText));
            }else if (func.equalsIgnoreCase("at")){
                component.setText(MessageUtil.replace(config.qqAtText.replaceAll("\\{qq}",ext.replaceAll("qq=",""))));
            }else if (func.equalsIgnoreCase("share")){
                String url = "";
                String[] a = ext.split(",");
                for (String kv : a) {
                    String[] b = kv.split("=",2);
                    if (b.length==2 && b[0].equalsIgnoreCase("url")){
                        url = b[1];
                        break;
                    }
                }
                component.setText(MessageUtil.replace(config.linkText));
                if (!"".equals(url)){
                    chat.setHover(component,url);
                    chat.setClick(component,url);
                }
            }else {
                return null;
            }
            return component;
        });
    }
}
