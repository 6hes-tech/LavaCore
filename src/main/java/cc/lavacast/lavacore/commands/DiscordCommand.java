package cc.lavacast.lavacore.commands;

import cc.lavacast.lavacore.util.Msg;
import org.bukkit.command.CommandSender;

public final class DiscordCommand extends LavaCommand {

    public DiscordCommand() {
        super("discord");
    }

    @Override
    protected void run(CommandSender sender, String[] args) {
        Msg.send(sender, Msg.cfg("links.discord-message") + Msg.cfg("links.discord"));
    }
}
