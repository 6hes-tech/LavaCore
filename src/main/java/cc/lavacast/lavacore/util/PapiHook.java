package cc.lavacast.lavacore.util;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;

/**
 * Optional PlaceholderAPI support through reflection, so building doesn't need PAPI.
 * Only ever applied to config strings, never to player chat.
 */
public final class PapiHook {

    private static Method setPlaceholders;

    private PapiHook() {
    }

    public static void init() {
        setPlaceholders = null;
        if (!Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) return;
        try {
            setPlaceholders = Class.forName("me.clip.placeholderapi.PlaceholderAPI")
                    .getMethod("setPlaceholders", OfflinePlayer.class, String.class);
        } catch (ReflectiveOperationException ignored) {
            setPlaceholders = null;
        }
    }

    public static String apply(CommandSender to, String s) {
        if (setPlaceholders == null || !(to instanceof Player p)) return s;
        try {
            return (String) setPlaceholders.invoke(null, p, s);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return s;
        }
    }
}
