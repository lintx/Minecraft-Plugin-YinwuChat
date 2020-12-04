package org.lintx.plugins.yinwuchat.Util;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.TextComponent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageUtil {

    public static String filter(String message,String denyStyle){
        message = message.replaceAll("&[" + denyStyle + "]","");
        message = message.replaceAll("§[" + denyStyle + "]","");
        return message;
    }

    public static String filterRGB(String message){
        message = message.replaceAll("&#([0-9a-fA-F]{6}|[0-9a-fA-F]{3})","");
        message = message.replaceAll("§#([0-9a-fA-F]{6}|[0-9a-fA-F]{3})","");
        return message;

    }

    public static String replace(String str){
        return str.replaceAll("&([0-9a-fA-Fk-oK-OrRxX])","§$1");
    }

    public static TextComponent newTextComponent(String str){
        List<String> strings = new ArrayList<>();
        String regexp = "&#([0-9a-fA-F]{6}|[0-9a-fA-F]{3})";
        Pattern pattern = Pattern.compile(regexp);
        Matcher matcher = pattern.matcher(str);
        while (matcher.find()){
            String[] splits = str.split(regexp,2);
            if (splits.length!=2) break;

            strings.add(splits[0]);
            //这里处理匹配到的颜色代码
            try {
                String c = matcher.group(1);
                if (null==c){
                    strings.add(matcher.group(0));
                }else {
                    StringBuilder buffer = new StringBuilder();
                    buffer.append("#");
                    int l = c.length();
                    for (int i=0;i<l;i++){
                        char ch = c.charAt(i);
                        buffer.append(ch);
                        if (l==3){
                            buffer.append(ch);
                        }
                    }
                    strings.add(buffer.toString());
                }
            }catch (Exception e){
                strings.add(matcher.group(0));
            }

            str = splits[1];
            matcher = pattern.matcher(str);
        }
        if (!"".equals(str)){
            strings.add(str);
        }

        TextComponent component = new TextComponent();
        Iterator<String> iterator = strings.iterator();
        while (iterator.hasNext()){
            String s = iterator.next();
            TextComponent c = new TextComponent();
            if (s.length()==7 && s.startsWith("#")){
                c.setColor(ChatColor.of(s));
                while (iterator.hasNext()){
                    String s1 = iterator.next();
                    if (s1.length()!=7 || !s1.startsWith("#")){
                        c.setText(s1);
                        break;
                    }
                }
            }else {
                c.setText(s);
            }
            component.addExtra(c);
        }
        return component;
    }

    public static String removeEmoji(String str){
        return str.replaceAll("[^\\p{L}\\p{N}\\p{P}\\p{Z}]","");
    }
}
