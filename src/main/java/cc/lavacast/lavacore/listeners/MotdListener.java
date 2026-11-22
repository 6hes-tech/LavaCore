package cc.lavacast.lavacore.listeners;

import cc.lavacast.lavacore.LavaCore;
import cc.lavacast.lavacore.util.Msg;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerListPingEvent;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class MotdListener implements Listener {

    private final LavaCore plugin;

    public MotdListener(LavaCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPing(ServerListPingEvent e) {
        if (!plugin.getConfig().getBoolean("motd.enabled")) return;
        List<String> list = plugin.getConfig().getStringList("motd.motd-list");
        if (list.isEmpty()) return; // old code crashed here with an empty list
        e.motd(Msg.color(list.get(ThreadLocalRandom.current().nextInt(list.size()))));
    }
}
