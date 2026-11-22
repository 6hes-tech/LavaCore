package cc.lavacast.lavacore.util;

import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Waterlogged;

import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Finds spots a player can stand on without dying or getting stuck.
 *
 * The old AnarchyCore used getHighestBlockYAt(x, z) as the feet Y. Since 1.15 that
 * returns the Y of the top block itself, so players spawned inside it. It also
 * happily returned water, lava and leaves.
 */
public final class SafeSpawn {

    /** Never stand on these. */
    private static final Set<Material> BAD_FLOOR = EnumSet.of(
            Material.MAGMA_BLOCK, Material.CACTUS, Material.CAMPFIRE, Material.SOUL_CAMPFIRE,
            Material.POWDER_SNOW, Material.POINTED_DRIPSTONE, Material.SWEET_BERRY_BUSH,
            Material.WITHER_ROSE, Material.FIRE, Material.SOUL_FIRE, Material.LAVA, Material.WATER,
            Material.BUBBLE_COLUMN, Material.COBWEB, Material.SCAFFOLDING, Material.BEDROCK,
            Material.END_PORTAL, Material.NETHER_PORTAL, Material.END_GATEWAY, Material.HONEY_BLOCK,
            Material.SLIME_BLOCK
    );

    /** Never have feet or head inside these, even if they're technically passable. */
    private static final Set<Material> BAD_SPACE = EnumSet.of(
            Material.FIRE, Material.SOUL_FIRE, Material.SWEET_BERRY_BUSH, Material.WITHER_ROSE,
            Material.POWDER_SNOW, Material.COBWEB, Material.LAVA, Material.WATER, Material.BUBBLE_COLUMN,
            Material.END_PORTAL, Material.NETHER_PORTAL, Material.END_GATEWAY, Material.POINTED_DRIPSTONE,
            Material.SEAGRASS, Material.TALL_SEAGRASS, Material.KELP, Material.KELP_PLANT,
            Material.CAMPFIRE, Material.SOUL_CAMPFIRE, Material.CACTUS, Material.MAGMA_BLOCK
    );

    private SafeSpawn() {
    }

    /** Random safe spot in the box, or null if every attempt failed. */
    public static Location random(World world, int minX, int maxX, int minZ, int maxZ, int attempts) {
        if (minX > maxX) { int t = minX; minX = maxX; maxX = t; }
        if (minZ > maxZ) { int t = minZ; minZ = maxZ; maxZ = t; }
        ThreadLocalRandom r = ThreadLocalRandom.current();
        for (int i = 0; i < Math.max(1, attempts); i++) {
            int x = r.nextInt(minX, maxX + 1);
            int z = r.nextInt(minZ, maxZ + 1);
            Location l = world.getEnvironment() == World.Environment.NETHER
                    ? scanDown(world, x, z, Math.min(120, world.getMaxHeight() - 3), world.getMinHeight() + 5)
                    : surface(world, x, z);
            if (l != null) {
                l.setYaw(r.nextFloat() * 360f - 180f);
                return l;
            }
        }
        return null;
    }

    /** Safe spot on the surface at this column, or null. */
    public static Location surface(World world, int x, int z) {
        Block floor = world.getHighestBlockAt(x, z, HeightMap.MOTION_BLOCKING_NO_LEAVES);
        return standOn(floor);
    }

    /** First safe spot scanning down from fromY to toY at this column. */
    public static Location scanDown(World world, int x, int z, int fromY, int toY) {
        for (int y = fromY; y >= toY; y--) {
            Location l = standOn(world.getBlockAt(x, y, z));
            if (l != null) return l;
        }
        return null;
    }

    /** First safe spot scanning up from fromY to toY at this column. */
    public static Location scanUp(World world, int x, int z, int fromY, int toY) {
        for (int y = fromY; y <= toY; y++) {
            Location l = standOn(world.getBlockAt(x, y, z));
            if (l != null) return l;
        }
        return null;
    }

    /** Location standing on top of this block, or null if that isn't safe. */
    public static Location standOn(Block floor) {
        World w = floor.getWorld();
        if (floor.getY() <= w.getMinHeight() || floor.getY() + 2 >= w.getMaxHeight()) return null;
        if (!safeFloor(floor)) return null;
        Block feet = floor.getRelative(BlockFace.UP);
        Block head = feet.getRelative(BlockFace.UP);
        if (!safeSpace(feet) || !safeSpace(head)) return null;
        return new Location(w, floor.getX() + 0.5, floor.getY() + 1, floor.getZ() + 0.5);
    }

    private static boolean safeFloor(Block b) {
        Material t = b.getType();
        if (!t.isSolid() || b.isLiquid() || BAD_FLOOR.contains(t)) return false;
        if (Tag.LEAVES.isTagged(t)) return false;
        if (waterlogged(b)) return false;
        // fences, walls etc are taller than a block and would leave you stuck in them
        return b.getBoundingBox().getMaxY() - b.getY() <= 1.0;
    }

    private static boolean safeSpace(Block b) {
        Material t = b.getType();
        if (BAD_SPACE.contains(t) || b.isLiquid() || waterlogged(b)) return false;
        return b.isPassable();
    }

    private static boolean waterlogged(Block b) {
        BlockData d = b.getBlockData();
        return d instanceof Waterlogged wl && wl.isWaterlogged();
    }
}
