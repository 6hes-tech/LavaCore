package cc.lavacast.lavacore.listeners;

import cc.lavacast.lavacore.LavaCore;
import cc.lavacast.lavacore.util.Data;
import cc.lavacast.lavacore.util.Msg;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.UUID;

/**
 * Mutes and name colors.
 *
 * Mutes just drop the muter from the message's viewers, so chat still goes through
 * Paper's normal pipeline. The old plugin cancelled every message and re-broadcast it
 * through its color code + PlaceholderAPI formatter, which let anyone use &amp; colors
 * and %placeholders% in chat.
 */
public final class ChatListener implements Listener {

    private final LavaCore plugin;

    public ChatListener(LavaCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onChat(AsyncChatEvent e) {
        Data data = plugin.data();
        UUID sender = e.getPlayer().getUniqueId();

        if (plugin.getConfig().getBoolean("enabled-commands.mute", true)) {
            e.viewers().removeIf(a -> a instanceof Player viewer && data.isMuted(viewer.getUniqueId(), sender));
        }

        // greentext / orangetext: ">text" and "<text" get colored. Only the message, never the name.
        String plain = PlainTextComponentSerializer.plainText().serialize(e.message());
        Style textStyle = null;
        if (startsWith(plain, "greentext")) textStyle = Msg.style(Msg.cfg("greentext.color"));
        else if (startsWith(plain, "orangetext")) textStyle = Msg.style(Msg.cfg("orangetext.color"));
        if (textStyle != null) e.message(Component.text(plain, textStyle));

        String styleCodes = data.nameStyle(sender);
        if (styleCodes != null && plugin.getConfig().getBoolean("enabled-commands.nc", true)) {
            Style nameStyle = Msg.style(styleCodes);
            e.renderer((source, displayName, message, viewer) -> Component.text()
                    .append(Component.text("<"))
                    .append(Component.text(source.getName(), nameStyle))
                    .append(Component.text("> "))
                    .append(message)
                    .build());
        }
    }

    private boolean startsWith(String plain, String section) {
        if (!plugin.getConfig().getBoolean(section + ".enabled")) return false;
        String prefix = Msg.cfg(section + ".prefix");
        return !prefix.isEmpty() && plain.startsWith(prefix);
    }
}
