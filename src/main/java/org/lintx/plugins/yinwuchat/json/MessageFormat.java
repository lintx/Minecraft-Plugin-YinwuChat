package org.lintx.plugins.yinwuchat.json;

import org.lintx.plugins.modules.configure.YamlConfig;

public class MessageFormat {
    public MessageFormat(){

    }
    public MessageFormat(String message){
        this(message,null,null);
    }

    public MessageFormat(String message,String hover,String click){
        this.message = message;
        this.hover = hover;
        this.click = click;
    }

    @YamlConfig
    public String message = null;

    @YamlConfig
    public String hover = null;

    @YamlConfig
    public String click = null;
}
