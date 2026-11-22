package cc.lavacast.lavacore.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class KillCommand extends LavaCommand {

    public KillCommand() {
        super("kill");
    }

    @Override
    protected void run(CommandSender sender, String[] args) {
        // ops still get vanilla /kill <selector> since this command overrides it
        if (args.length > 0 && sender.hasPermission("minecraft.command.kill")) {
            Bukkit.dispatchCommand(sender, "minecraft:kill " + String.join(" ", args));
            return;
        }
        Player p = requirePlayer(sender);
        if (p == null || p.isDead()) return;
        p.setHealth(0.0);
    }
}
