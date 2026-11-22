package cc.lavacast.lavacore.listeners;

import cc.lavacast.lavacore.LavaCore;
import cc.lavacast.lavacore.util.IllegalUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/**
 * 32k patch. The old one cancelled any player hit over 40 damage, which on 1.21 means mace
 * smashes just stop working. Now it only cancels hits with an overenchanted weapon.
 */
public final class DamageListener implements Listener {

    private final LavaCore plugin;

    public DamageListener(LavaCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent e) {
        if (!plugin.getConfig().getBoolean("32kpatch.enabled")) return;
        if (!(e.getDamager() instanceof Player p)) return;
        if (p.hasPermission("lavacore.bypassillegals")) return;
        if (IllegalUtils.overEnchanted(p.getInventory().getItemInMainHand())) e.setCancelled(true);
    }
}
