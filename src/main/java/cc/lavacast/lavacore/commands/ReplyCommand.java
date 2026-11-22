package cc.lavacast.lavacore.commands;

import cc.lavacast.lavacore.util.Msg;
import cc.lavacast.lavacore.util.Whispers;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

/** /reply (alias r). Messages whoever you last messaged or got a message from. */
public final class ReplyCommand extends LavaCommand {

    public ReplyCommand() {
        super("reply");
    }

    @Override
    protected void run(CommandSender sender, String[] args) {
        Player p = requirePlayer(sender);
        if (p == null) return;
        if (args.length == 0) {
            Msg.send(p, Msg.cfg("whispers.reply-usage"));
            return;
        }
        UUID last = Whispers.lastPartner(p);
        Player target = last == null ? null : Bukkit.getPlayer(last);
        if (target == null || !p.canSee(target)) {
            Msg.send(p, Msg.cfg("whispers.no-reply"));
            return;
        }
        Whispers.send(p, target, String.join(" ", args));
    }
}
