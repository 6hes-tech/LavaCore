package cc.lavacast.lavacore.commands;

import cc.lavacast.lavacore.LavaCore;
import cc.lavacast.lavacore.util.Data;
import cc.lavacast.lavacore.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * /mute, /unmute and /mutelist. A mute only hides the target's chat from the person who muted them.
 * Replaces AnarchyCore's /ignore, which never worked: its chat listener was commented out and the
 * ignore map was looked up with the wrong key types, so nobody was ever actually ignored.
 */
public final class MuteCommand extends LavaCommand {

    public enum Mode { MUTE, UNMUTE, LIST }

    private final Mode mode;

    public MuteCommand(String key, Mode mode) {
        super(key);
        this.mode = mode;
    }

    @Override
    protected void run(CommandSender sender, String[] args) {
        Player p = requirePlayer(sender);
        if (p == null) return;
        switch (mode) {
            case MUTE -> mute(p, args);
            case UNMUTE -> unmute(p, args);
            case LIST -> list(p);
        }
    }

    private void mute(Player p, String[] args) {
        if (args.length == 0) {
            Msg.send(p, Msg.cfg("mute-command.usage"));
            return;
        }
        OfflinePlayer target = resolve(args[0]);
        if (target == null) {
            Msg.send(p, Msg.cfg("mute-command.not-found"));
            return;
        }
        if (target.getUniqueId().equals(p.getUniqueId())) {
            Msg.send(p, Msg.cfg("mute-command.self"));
            return;
        }
        String name = nameOf(target, args[0]);
        boolean added = LavaCore.get().data().mute(p.getUniqueId(), target.getUniqueId());
        Msg.send(p, Msg.cfg(added ? "mute-command.muted" : "mute-command.already-muted").replace("<player>", name));
    }

    private void unmute(Player p, String[] args) {
        if (args.length == 0) {
            Msg.send(p, Msg.cfg("mute-command.unmute-usage"));
            return;
        }
        Data data = LavaCore.get().data();
        UUID targetId = null;
        String name = args[0];
        // look through the player's own mutes first, so this works even if the name isn't cached anymore
        for (UUID u : data.mutesOf(p.getUniqueId())) {
            String n = Bukkit.getOfflinePlayer(u).getName();
            if (n != null && n.equalsIgnoreCase(args[0])) {
                targetId = u;
                name = n;
                break;
            }
        }
        if (targetId == null) {
            OfflinePlayer target = resolve(args[0]);
            if (target != null) {
                targetId = target.getUniqueId();
                name = nameOf(target, args[0]);
            }
        }
        if (targetId != null && data.unmute(p.getUniqueId(), targetId)) {
            Msg.send(p, Msg.cfg("mute-command.unmuted").replace("<player>", name));
        } else {
            Msg.send(p, Msg.cfg("mute-command.not-muted").replace("<player>", name));
        }
    }

    private void list(Player p) {
        List<String> names = mutedNames(p);
        if (names.isEmpty()) {
            Msg.send(p, Msg.cfg("mute-command.list-empty"));
            return;
        }
        Msg.send(p, Msg.cfg("mute-command.list")
                .replace("<count>", String.valueOf(names.size()))
                .replace("<list>", String.join(", ", names)));
    }

    private static List<String> mutedNames(Player p) {
        List<String> names = new ArrayList<>();
        for (UUID u : LavaCore.get().data().mutesOf(p.getUniqueId())) {
            String n = Bukkit.getOfflinePlayer(u).getName();
            names.add(n == null ? u.toString() : n);
        }
        names.sort(String.CASE_INSENSITIVE_ORDER);
        return names;
    }

    /** Online player by exact name, else a cached offline player who has actually joined before. */
    private static OfflinePlayer resolve(String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) return online;
        OfflinePlayer off = Bukkit.getOfflinePlayerIfCached(name);
        if (off != null && off.hasPlayedBefore()) return off;
        return null;
    }

    private static String nameOf(OfflinePlayer p, String fallback) {
        return p.getName() == null ? fallback : p.getName();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1 || !(sender instanceof Player p)) return List.of();
        if (mode == Mode.UNMUTE) return filter(mutedNames(p), args[0]);
        if (mode == Mode.MUTE) {
            List<String> names = onlineNames(sender, args[0]);
            names.remove(p.getName());
            return names;
        }
        return List.of();
    }
}
