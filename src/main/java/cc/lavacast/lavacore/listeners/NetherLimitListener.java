package cc.lavacast.lavacore.listeners;

import cc.lavacast.lavacore.LavaCore;
import cc.lavacast.lavacore.util.Msg;
import cc.lavacast.lavacore.util.SafeSpawn;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.vehicle.VehicleMoveEvent;

import java.util.ArrayList;
import java.util.Locale;

/**
 * Nether roof / bottom limit. Each side has a mode in the config:
 * teleport = move to the nearest safe spot inside the nether, kill = kill the player, free = allowed.
 *
 * The old version teleported you 5 blocks straight down from the roof, which lands you inside the
 * bedrock ceiling and suffocates you, reset your camera angle every time, only worked on a world
 * literally named "world_nether", and did nothing if you were in a boat or on a horse.
 */
public final class NetherLimitListener implements Listener {

    private static final int SEARCH_RADIUS = 8;

    private enum Mode { TELEPORT, KILL, FREE }

    /** Which limit got crossed. */
    private enum Side {
        ROOF("antinetherroof"), BOTTOM("antinetherbottom");

        final String section;

        Side(String section) {
            this.section = section;
        }
    }

    private final LavaCore plugin;

    public NetherLimitListener(LavaCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent e) {
        Player p = e.getPlayer();
        Side side = violation(e.getTo());
        if (side == null || p.isDead() || p.hasPermission("lavacore.bypassnether")) return;

        if (mode(side) == Mode.KILL) {
            if (p.isInsideVehicle()) p.leaveVehicle();
            p.setHealth(0.0);
        } else {
            Location safe = findSafe(e.getTo(), side);
            if (safe != null) e.setTo(safe);
            else e.setCancelled(true);
            p.setGliding(false);
        }
        Msg.send(p, Msg.cfg(side.section + ".disabled-message"));
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onVehicleMove(VehicleMoveEvent e) {
        Side side = violation(e.getTo());
        if (side == null) return;
        for (Entity passenger : new ArrayList<>(e.getVehicle().getPassengers())) {
            if (!(passenger instanceof Player p) || p.isDead() || p.hasPermission("lavacore.bypassnether")) continue;
            e.getVehicle().removePassenger(p);
            if (mode(side) == Mode.KILL) {
                p.setHealth(0.0);
            } else {
                Location safe = findSafe(e.getTo(), side);
                if (safe != null) p.teleport(safe);
            }
            Msg.send(p, Msg.cfg(side.section + ".disabled-message"));
        }
    }

    private Mode mode(Side side) {
        String raw = plugin.getConfig().getString(side.section + ".mode", "teleport");
        try {
            return Mode.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return Mode.TELEPORT;
        }
    }

    /** null = fine, otherwise which limit the location is past. */
    private Side violation(Location to) {
        if (to == null || to.getWorld() == null) return null;
        if (to.getWorld().getEnvironment() != World.Environment.NETHER) return null;
        FileConfiguration c = plugin.getConfig();
        if (active(Side.ROOF) && to.getY() >= c.getInt("antinetherroof.max-y-level", 127)) return Side.ROOF;
        if (active(Side.BOTTOM) && to.getY() < c.getInt("antinetherbottom.min-y-level", 1)) return Side.BOTTOM;
        return null;
    }

    private boolean active(Side side) {
        return plugin.getConfig().getBoolean(side.section + ".enabled") && mode(side) != Mode.FREE;
    }

    /** Nearest safe spot inside the nether, searching this column first, then outwards. Keeps the camera angle. */
    private Location findSafe(Location from, Side side) {
        World w = from.getWorld();
        int roofY = plugin.getConfig().getInt("antinetherroof.max-y-level", 127);
        int floorY = plugin.getConfig().getInt("antinetherbottom.min-y-level", 1);
        int top = roofY - 4;                                  // stay clear of the bedrock ceiling
        int bottom = Math.max(floorY, w.getMinHeight()) + 4;  // and the bedrock floor
        int bx = from.getBlockX();
        int bz = from.getBlockZ();

        for (int r = 0; r <= SEARCH_RADIUS; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue; // only the ring
                    Location l = side == Side.ROOF
                            ? SafeSpawn.scanDown(w, bx + dx, bz + dz, top, bottom)
                            : SafeSpawn.scanUp(w, bx + dx, bz + dz, bottom, top);
                    if (l != null) {
                        l.setYaw(from.getYaw());
                        l.setPitch(from.getPitch());
                        return l;
                    }
                }
            }
        }
        return null;
    }
}
