package cc.lavacast.lavacore.commands;

import cc.lavacast.lavacore.LavaCore;
import cc.lavacast.lavacore.util.Msg;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /toggledeathmsgs and /togglejoinmsgs. Saved, so they survive relogs and restarts. */
public final class ToggleCommand extends LavaCommand {

    public enum Type { DEATHS, JOINS }

    private final Type type;

    public ToggleCommand(String key, Type type) {
        super(key);
        this.type = type;
    }

    @Override
    protected void run(CommandSender sender, String[] args) {
        Player p = requirePlayer(sender);
        if (p == null) return;
        String section = type == Type.DEATHS ? "death-messages" : "join-messages";
        boolean hidden = type == Type.DEATHS
                ? LavaCore.get().data().toggleDeaths(p.getUniqueId())
                : LavaCore.get().data().toggleJoins(p.getUniqueId());
        Msg.send(p, Msg.cfg(section + (hidden ? ".muted" : ".unmuted")));
    }
}
