package cc.lavacast.lavacore.listeners;

import cc.lavacast.lavacore.LavaCore;
import cc.lavacast.lavacore.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.Locale;

/**
 * Old bugs: end portal frames and spawners had config options that were never checked,
 * and it always deleted the main hand item even when the block was placed from the off hand.
 */
public final class BlockPlaceListener implements Listener {

    private final LavaCore plugin;

    public BlockPlaceListener(LavaCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent e) {
        if (!plugin.getConfig().getBoolean("antiblockplaces.enabled")) return;
        Player p = e.getPlayer();
        if (p.hasPermission("lavacore.bypassblocks")) return;

        // Check the item being placed, not the resulting block. Putting an eye of ender into a
        // frame fires this event with END_PORTAL_FRAME as the block, but the item is ENDER_EYE.
        Material itemType = e.getItemInHand().getType();
        boolean blocked = false;
        for (String s : plugin.getConfig().getStringList("antiblockplaces.blocks")) {
            if (itemType.name().equalsIgnoreCase(s.trim())) {
                blocked = true;
                break;
            }
        }
        if (!blocked) return;

        e.setCancelled(true);
        EquipmentSlot hand = e.getHand();
        Material placed = itemType;
        // delete next tick, after the server has finished restoring the cancelled item
        Bukkit.getScheduler().runTask(plugin, () -> {
            ItemStack current = p.getInventory().getItem(hand);
            if (current != null && current.getType() == itemType) p.getInventory().setItem(hand, null);
        });
        String pretty = placed.name().toLowerCase(Locale.ROOT).replace('_', ' ');
        Msg.send(p, Msg.cfg("antiblockplaces.message").replace("<block>", pretty));
    }
}
