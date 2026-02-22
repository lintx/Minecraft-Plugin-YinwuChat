package org.lintx.plugins.yinwuchat.bungee;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;

/**
 * 将 /yinwuchat 的子命令代理为独立命令
 * 例如 /vanish 实际执行 /yinwuchat vanish
 */
public class BungeeSubcommandProxy extends Command {
    private final Commands delegate;
    private final String subcommand;

    public BungeeSubcommandProxy(Commands delegate, String commandName, String subcommand) {
        super(commandName);
        this.delegate = delegate;
        this.subcommand = subcommand;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        String[] newArgs = new String[args.length + 1];
        newArgs[0] = subcommand;
        System.arraycopy(args, 0, newArgs, 1, args.length);
        delegate.execute(sender, newArgs);
    }
}
