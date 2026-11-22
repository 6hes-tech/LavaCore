package cc.lavacast.lavacore.commands;

import cc.lavacast.lavacore.LavaCore;
import cc.lavacast.lavacore.util.Msg;
import cc.lavacast.lavacore.util.ServerStats;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

/** Prints a list of lines from the config. Used for /help and /stats. */
public final class HelpCommand extends LavaCommand {

    private final String path;

    public HelpCommand(String key, String path) {
        super(key);
        this.path = path;
    }

    @Override
    protected void run(CommandSender sender, String[] args) {
        long left = LavaCore.get().restartTask().millisLeft();
        String restart = left < 0 ? "disabled" : Msg.duration(left / 1000);
        ServerStats stats = LavaCore.get().stats();
        for (String line : LavaCore.get().getConfig().getStringList(path)) {
            Msg.sendRaw(sender, line
                    .replace("<online>", String.valueOf(Bukkit.getOnlinePlayers().size()))
                    .replace("<discord>", Msg.cfg("links.discord"))
                    .replace("<restart>", restart)
                    .replace("<player>", sender.getName())
                    .replace("<server-name>", Msg.cfg("stats.server-name"))
                    .replace("<created>", stats.created())
                    .replace("<days>", stats.daysSinceCreated())
                    .replace("<world-size>", stats.worldSize())
                    .replace("<total-players>", stats.totalPlayers()));
        }
    }
}
