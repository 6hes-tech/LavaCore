package cc.lavacast.lavacore.listeners;

import cc.lavacast.lavacore.LavaCore;
import cc.lavacast.lavacore.util.Msg;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.TranslationArgument;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * 2b2t style death messages. Takes the vanilla translatable death message and recolors it:
 * argument 0 is the victim, 1 is the killer, 2 is the weapon. Every death type keeps working,
 * it gets translated client side, and hovering names/items still works.
 */
public final class DeathListener implements Listener {

    private final LavaCore plugin;

    public DeathListener(LavaCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDeath(PlayerDeathEvent e) {
        if (!plugin.getConfig().getBoolean("death-messages.enabled")) return;
        Component original = e.deathMessage();
        e.deathMessage(null);
        if (original == null) return;

        Component out = recolor(original);
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!plugin.data().hidesDeaths(p.getUniqueId())) p.sendMessage(out);
        }
        Bukkit.getConsoleSender().sendMessage(out);
    }

    private Component recolor(Component original) {
        Style text = Msg.style(Msg.cfg("death-messages.text-color"));
        Style victim = Msg.style(Msg.cfg("death-messages.victim-color"));
        Style killer = Msg.style(Msg.cfg("death-messages.killer-color"));
        Style weapon = Msg.style(Msg.cfg("death-messages.weapon-color"));

        if (original instanceof TranslatableComponent tc) {
            List<ComponentLike> args = new ArrayList<>();
            List<TranslationArgument> in = tc.arguments();
            for (int i = 0; i < in.size(); i++) {
                TranslationArgument arg = in.get(i);
                if (arg.value() instanceof Component c) {
                    Style s = i == 0 ? victim : i == 1 ? killer : weapon;
                    args.add(force(c, s));
                } else {
                    args.add(arg);
                }
            }
            return force(tc.arguments(args), text, false);
        }
        // some other plugin already replaced it with plain text, just tint the whole thing
        return Component.text().style(text).append(original).build();
    }

    private static Component force(Component c, Style s) {
        return force(c, s, true);
    }

    /** Applies the style's color/decorations to the component (and its children if deep). Hover/click stay. */
    private static Component force(Component c, Style s, boolean deep) {
        Component r = c;
        if (s.color() != null) r = r.color(s.color());
        for (TextDecoration d : TextDecoration.values()) {
            if (s.decoration(d) == TextDecoration.State.TRUE) r = r.decoration(d, true);
        }
        if (deep && !r.children().isEmpty()) {
            List<Component> kids = new ArrayList<>();
            for (Component child : r.children()) kids.add(force(child, s, true));
            r = r.children(kids);
        }
        return r;
    }
}
