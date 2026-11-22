package cc.lavacast.lavacore.listeners;

import cc.lavacast.lavacore.LavaCore;
import cc.lavacast.lavacore.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class JoinQuitListener implements Listener {

    private final LavaCore plugin;

    public JoinQuitListener(LavaCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onJoin(PlayerJoinEvent e) {
        Player joined = e.getPlayer();
        if (plugin.getConfig().getBoolean("join-messages.enabled")) {
            e.joinMessage(null);
            broadcast(Msg.cfg("join-messages.join").replace("<player>", joined.getName()));
        }

        // the old plugin only showed this when custom join messages were on
        int show = plugin.getConfig().getInt("welcome-message.show", 1);
        if (show == 2 || (show == 1 && !joined.hasPlayedBefore())) {
            Msg.send(joined, Msg.cfg("welcome-message.message")
                    .replace("<player>", joined.getName())
                    .replace("<connected>", String.valueOf(Bukkit.getOnlinePlayers().size()))
                    .replace("<maxplayers>", String.valueOf(Bukkit.getMaxPlayers())));
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onQuit(PlayerQuitEvent e) {
        cc.lavacast.lavacore.util.Whispers.forget(e.getPlayer().getUniqueId());
        if (plugin.getConfig().getBoolean("join-messages.enabled")) {
            e.quitMessage(null);
            broadcast(Msg.cfg("join-messages.leave").replace("<player>", e.getPlayer().getName()));
        }
    }

    private void broadcast(String raw) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!plugin.data().hidesJoins(p.getUniqueId())) Msg.sendRaw(p, raw);
        }
        Bukkit.getConsoleSender().sendMessage(Msg.color(raw));
    }
}
