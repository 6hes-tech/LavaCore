package cc.lavacast.lavacore.commands;

import cc.lavacast.lavacore.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public final class PingCommand extends LavaCommand {

    public PingCommand() {
        super("ping");
    }

    @Override
    protected void run(CommandSender sender, String[] args) {
        if (args.length == 0) {
            Player p = requirePlayer(sender);
            if (p == null) return;
            Msg.send(p, Msg.cfg("ping.ping-msg").replace("<ping>", String.valueOf(p.getPing())));
            return;
        }
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null || (sender instanceof Player v && !v.canSee(target))) {
            Msg.send(sender, Msg.cfg("ping.ping-notonline"));
            return;
        }
        Msg.send(sender, Msg.cfg("ping.ping-other")
                .replace("<ping>", String.valueOf(target.getPing()))
                .replace("<p>", target.getName()));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return args.length == 1 ? onlineNames(sender, args[0]) : List.of();
    }
}
