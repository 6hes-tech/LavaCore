package cc.lavacast.lavacore.util;

import cc.lavacast.lavacore.LavaCore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Private messages for /msg and /r. Tracks who each player last talked to. */
public final class Whispers {

    private static final Map<UUID, UUID> LAST = new ConcurrentHashMap<>();

    private Whispers() {
    }

    public static UUID lastPartner(Player p) {
        return LAST.get(p.getUniqueId());
    }

    public static void forget(UUID player) {
        LAST.remove(player);
    }

    public static void send(CommandSender from, Player to, String message) {
        LavaCore plugin = LavaCore.get();
        Style msgStyle = Msg.style(Msg.cfg("whispers.message-color"));
        // player text is always plain, never parsed for colors or placeholders
        Component body = Component.text(message, msgStyle);

        from.sendMessage(Msg.color(Msg.cfg("whispers.sent").replace("<player>", to.getName())).append(body));

        if (from instanceof Player fp) {
            LAST.put(fp.getUniqueId(), to.getUniqueId());
            boolean muted = plugin.getConfig().getBoolean("whispers.blocked-by-mute")
                    && plugin.data().isMuted(to.getUniqueId(), fp.getUniqueId());
            if (muted) return; // sender isn't told, same as 2b2t ignore
            LAST.put(to.getUniqueId(), fp.getUniqueId());
        }
        to.sendMessage(Msg.color(Msg.cfg("whispers.received").replace("<player>", from.getName())).append(body));
    }
}
