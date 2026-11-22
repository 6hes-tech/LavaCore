package cc.lavacast.lavacore.listeners;

import cc.lavacast.lavacore.LavaCore;
import cc.lavacast.lavacore.util.IllegalUtils;
import cc.lavacast.lavacore.util.Msg;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

/**
 * The old version of this was a dupe: if you had ANY illegal item and opened a hopper, it dropped a
 * copy of every stack in your inventory on the ground and then set each stack to 1. Now it just
 * deletes the illegal items.
 */
public final class IllegalItemListener implements Listener {

    private final LavaCore plugin;

    public IllegalItemListener(LavaCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onOpen(InventoryOpenEvent e) {
        if (!plugin.getConfig().getBoolean("anti-illegals.enabled")) return;
        if (e.getInventory().getType() != InventoryType.HOPPER) return;
        if (!(e.getPlayer() instanceof Player p)) return;
        if (p.getGameMode() == GameMode.CREATIVE || p.hasPermission("lavacore.bypassillegals")) return;

        PlayerInventory inv = p.getInventory();
        int removed = 0;
        ItemStack[] contents = inv.getContents();
        for (int i = 0; i < contents.length; i++) {
            if (IllegalUtils.isIllegal(contents[i])) {
                inv.setItem(i, null);
                removed++;
            }
        }
        if (removed > 0) {
            Msg.send(p, Msg.cfg("anti-illegals.message").replace("<count>", String.valueOf(removed)));
            plugin.getLogger().info("Removed " + removed + " illegal item(s) from " + p.getName());
        }
    }
}
