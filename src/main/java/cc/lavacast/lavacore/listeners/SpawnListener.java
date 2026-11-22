package cc.lavacast.lavacore.listeners;

import cc.lavacast.lavacore.LavaCore;
import cc.lavacast.lavacore.util.SafeSpawn;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

/**
 * Random spawn on first join and on death without a bed/anchor.
 *
 * Fixes from AnarchyCore: spawning inside blocks, in water/lava/leaves, and getting
 * random spawned when leaving the End through the exit portal (that fires a respawn
 * event too, the old code didn't check why the player was respawning).
 */
public final class SpawnListener implements Listener {

    private final LavaCore plugin;
    private boolean warnedMissingWorld;

    public SpawnListener(LavaCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onRespawn(PlayerRespawnEvent e) {
        if (!plugin.getConfig().getBoolean("random-spawn.enabled")) return;
        if (e.getRespawnReason() != PlayerRespawnEvent.RespawnReason.DEATH) return;
        if (e.isBedSpawn() || e.isAnchorSpawn()) return;
        Location l = randomSpawn();
        if (l != null) e.setRespawnLocation(l);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onFirstJoin(PlayerJoinEvent e) {
        if (e.getPlayer().hasPlayedBefore()) return;
        if (!plugin.getConfig().getBoolean("random-spawn.enabled")) return;
        Location l = randomSpawn();
        if (l != null) e.getPlayer().teleport(l);
    }

    private Location randomSpawn() {
        FileConfiguration c = plugin.getConfig();
        String worldName = c.getString("random-spawn.world-name", "world");
        World w = Bukkit.getWorld(worldName);
        if (w == null) {
            if (!warnedMissingWorld) {
                plugin.getLogger().warning("random-spawn.world-name '" + worldName + "' doesn't exist, random spawn skipped.");
                warnedMissingWorld = true;
            }
            return null;
        }
        Location l = SafeSpawn.random(w,
                c.getInt("random-spawn.minX"), c.getInt("random-spawn.maxX"),
                c.getInt("random-spawn.minZ"), c.getInt("random-spawn.maxZ"),
                c.getInt("random-spawn.attempts", 50));
        if (l != null) return l;

        // nothing safe in the box (all ocean?), fall back to a safe spot at world spawn
        Location spawn = w.getSpawnLocation();
        Location safe = SafeSpawn.surface(w, spawn.getBlockX(), spawn.getBlockZ());
        plugin.getLogger().warning("Couldn't find a safe random spawn, using world spawn instead.");
        return safe != null ? safe : spawn;
    }
}
