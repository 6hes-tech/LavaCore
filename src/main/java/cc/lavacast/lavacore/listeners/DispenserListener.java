package cc.lavacast.lavacore.listeners;

import cc.lavacast.lavacore.LavaCore;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDispenseEvent;

/**
 * The old version hardcoded 1.12 heights (y &gt; 250 or y &lt; 5), which on 1.18+ broke every
 * dispenser below y 5, so basically all of deepslate. Now uses the world's real build limits.
 */
public final class DispenserListener implements Listener {

    private final LavaCore plugin;

    public DispenserListener(LavaCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDispense(BlockDispenseEvent e) {
        if (!plugin.getConfig().getBoolean("antidispensercrash.enabled")) return;
        Block b = e.getBlock();
        World w = b.getWorld();
        boolean edge = b.getY() >= w.getMaxHeight() - 1 || b.getY() <= w.getMinHeight();
        boolean flint = plugin.getConfig().getBoolean("antidispensercrash.block-flint-and-steel")
                && e.getItem().getType() == Material.FLINT_AND_STEEL;
        if (!edge && !flint) return;

        e.setCancelled(true);
        if (plugin.getConfig().getBoolean("antidispensercrash.log-to-console")) {
            plugin.getLogger().warning("Blocked a dispenser at " + w.getName() + " "
                    + b.getX() + " " + b.getY() + " " + b.getZ());
        }
    }
}
