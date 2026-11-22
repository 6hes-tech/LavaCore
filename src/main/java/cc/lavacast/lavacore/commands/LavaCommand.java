package cc.lavacast.lavacore.commands;

import cc.lavacast.lavacore.LavaCore;
import cc.lavacast.lavacore.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Base for every LavaCore command. Checks enabled-commands at run time so /corereload
 * can turn commands on and off, and never blindly casts the sender to Player.
 */
public abstract class LavaCommand implements TabExecutor {

    private final String key;

    protected LavaCommand(String key) {
        this.key = key;
    }

    public boolean isEnabled() {
        return LavaCore.get().getConfig().getBoolean("enabled-commands." + key, true);
    }

    @Override
    public final boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!isEnabled()) {
            Msg.sendRaw(sender, Msg.cfg("command-whitelist.whitelist-message"));
            return true;
        }
        run(sender, args);
        return true;
    }

    protected abstract void run(CommandSender sender, String[] args);

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return List.of();
    }

    protected static Player requirePlayer(CommandSender sender) {
        if (sender instanceof Player p) return p;
        Msg.sendRaw(sender, Msg.cfg("global.players-only-message"));
        return null;
    }

    protected static List<String> filter(Iterable<String> options, String typed) {
        String t = typed.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (String o : options) {
            if (o.toLowerCase(Locale.ROOT).startsWith(t)) out.add(o);
        }
        return out;
    }

    protected static List<String> onlineNames(CommandSender viewer, String typed) {
        List<String> names = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (viewer instanceof Player v && !v.canSee(p)) continue;
            names.add(p.getName());
        }
        return filter(names, typed);
    }
}
