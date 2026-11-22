package cc.lavacast.lavacore.commands;

import cc.lavacast.lavacore.LavaCore;
import cc.lavacast.lavacore.util.Msg;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * /nc <color> [format...]. Saved per UUID so it survives relogs and restarts.
 * The old version keyed it by Player object (lost on relog), and "strike"/"italic"
 * actually applied magic text because of a copy paste bug.
 */
public final class NameColorCommand extends LavaCommand {

    private static final Map<String, Character> COLORS = new LinkedHashMap<>();
    private static final Map<String, Character> FORMATS = new LinkedHashMap<>();

    static {
        COLORS.put("black", '0');
        COLORS.put("darkblue", '1');
        COLORS.put("darkgreen", '2');
        COLORS.put("darkaqua", '3');
        COLORS.put("darkred", '4');
        COLORS.put("darkpurple", '5');
        COLORS.put("gold", '6');
        COLORS.put("gray", '7');
        COLORS.put("darkgray", '8');
        COLORS.put("blue", '9');
        COLORS.put("green", 'a');
        COLORS.put("aqua", 'b');
        COLORS.put("red", 'c');
        COLORS.put("lightpurple", 'd');
        COLORS.put("yellow", 'e');
        COLORS.put("white", 'f');

        FORMATS.put("bold", 'l');
        FORMATS.put("magic", 'k');
        FORMATS.put("underline", 'n');
        FORMATS.put("strike", 'm');
        FORMATS.put("italic", 'o');
    }

    public NameColorCommand() {
        super("nc");
    }

    @Override
    protected void run(CommandSender sender, String[] args) {
        Player p = requirePlayer(sender);
        if (p == null) return;
        if (!p.hasPermission("lavacore.nc")) {
            Msg.send(p, Msg.cfg("global.no-permission-message"));
            return;
        }
        if (args.length == 0) {
            Msg.send(p, Msg.cfg("nc-command.usage-message"));
            return;
        }
        if (args[0].equalsIgnoreCase("reset")) {
            LavaCore.get().data().clearNameStyle(p.getUniqueId());
            Msg.send(p, Msg.cfg("nc-command.reset-message"));
            return;
        }

        Character color = null;
        Set<Character> formats = new LinkedHashSet<>();
        for (String raw : args) {
            String arg = raw.toLowerCase(Locale.ROOT);
            if (COLORS.containsKey(arg)) {
                color = COLORS.get(arg);
            } else if (FORMATS.containsKey(arg)) {
                if (!p.hasPermission("lavacore.nc.format")) {
                    Msg.send(p, Msg.cfg("global.no-permission-message"));
                    return;
                }
                formats.add(FORMATS.get(arg));
            } else {
                Msg.send(p, Msg.cfg("nc-command.error-message").replace("<arg>", raw));
                return;
            }
        }

        // color has to come first, a color code after a format resets the format
        StringBuilder style = new StringBuilder("&").append(color == null ? 'f' : color);
        for (char f : formats) style.append('&').append(f);

        LavaCore.get().data().setNameStyle(p.getUniqueId(), style.toString());
        Msg.send(p, Msg.cfg("nc-command.success-message").replace("<preview>", style + p.getName()));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 0) return List.of();
        List<String> options = new ArrayList<>(COLORS.keySet());
        if (sender.hasPermission("lavacore.nc.format")) options.addAll(FORMATS.keySet());
        if (args.length == 1) options.add("reset");
        return filter(options, args[args.length - 1]);
    }
}
