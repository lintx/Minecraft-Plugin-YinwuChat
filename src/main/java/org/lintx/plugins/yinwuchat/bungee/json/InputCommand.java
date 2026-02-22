package org.lintx.plugins.yinwuchat.bungee.json;

/**
 * 命令输入
 */
public class InputCommand extends InputBase {
    private final String command;
    
    public String getCommand() {
        return command;
    }
    
    public InputCommand(String command) {
        if (command == null) {
            command = "";
        }
        this.command = command;
    }
}
