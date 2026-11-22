package cc.lavacast.lavacore.commands;

import cc.lavacast.lavacore.util.Msg;
import cc.lavacast.lavacore.util.Whispers;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;

/** /msg (aliases tell, w, whisper, m, pm). Replaces vanilla /msg so /r knows who to reply to. */
public final class MsgCommand extends LavaCommand {

    public MsgCommand() {
        super("msg");
    }

    @Override
    protected void run(CommandSender sender, String[] args) {
        if (args.length < 2) {
            Msg.send(sender, Msg.cfg("whispers.usage"));
            return;
        }
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null || (sender instanceof Player v && !v.canSee(target))) {
            Msg.send(sender, Msg.cfg("whispers.not-online"));
            return;
        }
        if (target.equals(sender)) {
            Msg.send(sender, Msg.cfg("whispers.self"));
            return;
        }
        Whispers.send(sender, target, String.join(" ", Arrays.copyOfRange(args, 1, args.length)));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return args.length == 1 ? onlineNames(sender, args[0]) : List.of();
    }
}
