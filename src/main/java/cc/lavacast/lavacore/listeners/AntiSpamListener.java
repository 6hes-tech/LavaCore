package cc.lavacast.lavacore.listeners;

import cc.lavacast.lavacore.LavaCore;
import cc.lavacast.lavacore.util.Msg;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.ArrayDeque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple chat antispam: a cooldown between messages and a burst limit (X messages per Y seconds).
 * A blocked message is a strike. Getting blocked again within the strike window adds another
 * strike, and hitting the strike limit kicks. Chat is async, so state is per player and synchronized.
 */
public final class AntiSpamListener implements Listener {

    private static final class State {
        long lastMessage;
        final ArrayDeque<Long> recent = new ArrayDeque<>();
        int strikes;
        long lastStrike;
    }

    private final LavaCore plugin;
    private final Map<UUID, State> states = new ConcurrentHashMap<>();

    public AntiSpamListener(LavaCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChat(AsyncChatEvent e) {
        FileConfiguration c = plugin.getConfig();
        if (!c.getBoolean("antispam.enabled")) return;
        Player p = e.getPlayer();
        if (p.hasPermission("lavacore.bypassspam")) return;

        long now = System.currentTimeMillis();
        State s = states.computeIfAbsent(p.getUniqueId(), k -> new State());
        boolean kick;
        synchronized (s) {
            if (!tooFast(s, c, now)) {
                s.lastMessage = now;
                s.recent.addLast(now);
                return;
            }
            // blocked: count a strike, chained strikes only count if they come right after the last one
            long window = (long) (c.getDouble("antispam.kick.strike-window-seconds", 5) * 1000);
            s.strikes = now - s.lastStrike <= window ? s.strikes + 1 : 1;
            s.lastStrike = now;
            kick = c.getBoolean("antispam.kick.enabled") && s.strikes >= Math.max(1, c.getInt("antispam.kick.strikes", 2));
            if (kick) s.strikes = 0;
        }

        e.setCancelled(true);
        if (kick) {
            String reason = Msg.cfg("antispam.kick.message");
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (p.isOnline()) p.kick(Msg.color(reason));
            });
            plugin.getLogger().info("Kicked " + p.getName() + " for spamming.");
        } else {
            Msg.sendRaw(p, Msg.cfg("antispam.blocked-message"));
        }
    }

    private static boolean tooFast(State s, FileConfiguration c, long now) {
        if (c.getBoolean("antispam.cooldown.enabled")) {
            long cooldown = (long) (c.getDouble("antispam.cooldown.seconds", 1.5) * 1000);
            if (now - s.lastMessage < cooldown) return true;
        }
        if (c.getBoolean("antispam.burst.enabled")) {
            long window = (long) (c.getDouble("antispam.burst.per-seconds", 10) * 1000);
            int max = Math.max(1, c.getInt("antispam.burst.max-messages", 5));
            while (!s.recent.isEmpty() && now - s.recent.peekFirst() > window) s.recent.pollFirst();
            if (s.recent.size() >= max) return true;
        }
        return false;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        states.remove(e.getPlayer().getUniqueId());
    }
}
