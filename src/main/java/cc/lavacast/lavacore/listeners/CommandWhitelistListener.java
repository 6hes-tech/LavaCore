package cc.lavacast.lavacore.listeners;

import cc.lavacast.lavacore.LavaCore;
import cc.lavacast.lavacore.commands.HelpCommand;
import cc.lavacast.lavacore.util.Msg;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerCommandSendEvent;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public final class CommandWhitelistListener implements Listener {

    private final LavaCore plugin;

    public CommandWhitelistListener(LavaCore plugin) {
        this.plugin = plugin;
    }

    private boolean active(Player p) {
        return plugin.getConfig().getBoolean("command-whitelist.enabled") && !p.hasPermission("lavacore.bypasswhitelist");
    }

    private Set<String> whitelist() {
        Set<String> set = new HashSet<>();
        for (String s : plugin.getConfig().getStringList("command-whitelist.whitelisted-commands")) {
            set.add(s.toLowerCase(Locale.ROOT).replace("/", "").trim());
        }
        return set;
    }

    private boolean allowed(String label, Set<String> whitelist) {
        return !label.contains(":") && whitelist.contains(label);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent e) {
        Player p = e.getPlayer();
        String msg = e.getMessage();
        String label = msg.length() > 1 ? msg.substring(1).split(" ", 2)[0].toLowerCase(Locale.ROOT) : "";

        if (active(p) && !allowed(label, whitelist())) {
            e.setCancelled(true);
            Msg.sendRaw(p, Msg.cfg("command-whitelist.whitelist-message"));
            return;
        }

        // Bukkit's built in /help would win over ours, so run ours directly
        if (label.equals("help")) {
            HelpCommand help = plugin.helpCommand();
            if (help != null && help.isEnabled()) {
                e.setCancelled(true);
                help.onCommand(p, plugin.getCommand("help"), "help", new String[0]);
            }
        }
    }

    /** Hide non-whitelisted commands from tab completion. */
    @EventHandler(priority = EventPriority.HIGH)
    public void onCommandSend(PlayerCommandSendEvent e) {
        if (!active(e.getPlayer())) return;
        Set<String> whitelist = whitelist();
        e.getCommands().removeIf(c -> !allowed(c.toLowerCase(Locale.ROOT), whitelist));
    }
}
