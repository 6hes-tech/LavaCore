package cc.lavacast.lavacore.util;

import cc.lavacast.lavacore.LavaCore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyFormat;
import org.bukkit.command.CommandSender;

public final class Msg {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.builder()
            .character('&')
            .hexColors()
            .build();

    private Msg() {
    }

    /** Turns a config string with & codes into a component. Never pass player typed text here. */
    public static Component color(String s) {
        return LEGACY.deserialize(s == null ? "" : s);
    }

    public static String cfg(String path) {
        String s = LavaCore.get().getConfig().getString(path);
        return s == null ? "" : s;
    }

    /** Sends a config message with the plugin prefix. */
    public static void send(CommandSender to, String raw) {
        sendRaw(to, cfg("global.prefix") + raw);
    }

    /** Sends a config message without the prefix. */
    public static void sendRaw(CommandSender to, String raw) {
        to.sendMessage(color(PapiHook.apply(to, raw)));
    }

    /** Parses a code string like "&b&l" or "&#ff8800" into a Style. Empty string gives an empty style. */
    public static Style style(String codes) {
        Style.Builder b = Style.style();
        if (codes == null) return b.build();
        for (int i = 0; i < codes.length(); i++) {
            char c = codes.charAt(i);
            if ((c != '&' && c != '§') || i + 1 >= codes.length()) continue;
            char n = codes.charAt(i + 1);
            if (n == '#' && i + 8 <= codes.length()) {
                TextColor hex = TextColor.fromHexString(codes.substring(i + 1, i + 8));
                if (hex != null) {
                    b.color(hex);
                    i += 7;
                    continue;
                }
            }
            LegacyFormat f = LegacyComponentSerializer.parseChar(n);
            if (f != null) {
                if (f.color() != null) b.color(f.color());
                else if (f.decoration() != null) b.decoration(f.decoration(), true);
            }
            i++;
        }
        return b.build();
    }

    /** "1 hour 5 minutes", "30 seconds", etc. */
    public static String duration(long totalSeconds) {
        if (totalSeconds < 0) totalSeconds = 0;
        long h = totalSeconds / 3600;
        long m = (totalSeconds % 3600) / 60;
        long s = totalSeconds % 60;
        StringBuilder sb = new StringBuilder();
        if (h > 0) sb.append(h).append(h == 1 ? " hour" : " hours");
        if (m > 0) sb.append(sb.isEmpty() ? "" : " ").append(m).append(m == 1 ? " minute" : " minutes");
        if (s > 0 || sb.isEmpty()) sb.append(sb.isEmpty() ? "" : " ").append(s).append(s == 1 ? " second" : " seconds");
        return sb.toString();
    }
}
