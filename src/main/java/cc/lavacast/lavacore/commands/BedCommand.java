package cc.lavacast.lavacore.commands;

import cc.lavacast.lavacore.util.Msg;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class BedCommand extends LavaCommand {

    public BedCommand() {
        super("bed");
    }

    @Override
    protected void run(CommandSender sender, String[] args) {
        Player p = requirePlayer(sender);
        if (p == null) return;
        Location loc = p.getRespawnLocation();
        if (loc == null || loc.getWorld() == null) {
            Msg.send(p, Msg.cfg("bed-command.message-nobed"));
            return;
        }
        Msg.send(p, Msg.cfg("bed-command.message-bed")
                .replace("<x>", String.valueOf(loc.getBlockX()))
                .replace("<y>", String.valueOf(loc.getBlockY()))
                .replace("<z>", String.valueOf(loc.getBlockZ()))
                .replace("<world>", loc.getWorld().getName()));
    }
}
