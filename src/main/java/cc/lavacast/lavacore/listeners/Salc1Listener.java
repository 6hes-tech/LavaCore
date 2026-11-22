package cc.lavacast.lavacore.listeners;

import cc.lavacast.lavacore.LavaCore;
import cc.lavacast.lavacore.util.Msg;
import org.bukkit.Material;
import org.bukkit.entity.ChestedHorse;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/** Off by default, see config. Old version only caught the main hand and missed mules. */
public final class Salc1Listener implements Listener {

    private final LavaCore plugin;

    public Salc1Listener(LavaCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEntityEvent e) {
        if (!plugin.getConfig().getBoolean("salc1-patch.enabled")) return;
        if (!(e.getRightClicked() instanceof ChestedHorse donkey)) return;
        Player p = e.getPlayer();
        ItemStack held = e.getHand() == EquipmentSlot.OFF_HAND
                ? p.getInventory().getItemInOffHand()
                : p.getInventory().getItemInMainHand();
        if (held.getType() != Material.CHEST) return;

        e.setCancelled(true);
        for (ItemStack i : donkey.getInventory().getContents()) {
            if (i != null && !i.getType().isAir() && i.getType() != Material.SADDLE) {
                donkey.getWorld().dropItemNaturally(donkey.getLocation(), i);
            }
        }
        donkey.setCarryingChest(false);
        Msg.send(p, Msg.cfg("salc1-patch.message"));
    }
}
