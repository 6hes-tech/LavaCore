package cc.lavacast.lavacore.commands;

import cc.lavacast.lavacore.LavaCore;
import cc.lavacast.lavacore.util.Msg;
import cc.lavacast.lavacore.util.PapiHook;
import org.bukkit.command.CommandSender;

public final class ReloadCommand extends LavaCommand {

    public ReloadCommand() {
        super("corereload");
    }

    @Override
    protected void run(CommandSender sender, String[] args) {
        if (!sender.hasPermission("lavacore.reload")) {
            Msg.sendRaw(sender, Msg.cfg("global.no-permission-message"));
            return;
        }
        LavaCore.get().reload();
        PapiHook.init();
        Msg.send(sender, Msg.cfg("core-reload.message-reload"));
    }
}
