package cc.lavacast.lavacore.util;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Map;

public final class IllegalUtils {

    private IllegalUtils() {
    }

    public static boolean isIllegal(ItemStack i) {
        if (i == null || i.getType().isAir()) return false;
        if (overEnchanted(i)) return true;
        if (i.getAmount() > i.getMaxStackSize()) return true;
        if (i.getType() == Material.FIREWORK_ROCKET && i.getItemMeta() instanceof FireworkMeta fm) {
            return fm.getPower() > 3 || fm.getPower() < 0;
        }
        return false;
    }

    /** Any enchant (or stored book enchant) above its max level. */
    public static boolean overEnchanted(ItemStack i) {
        if (i == null || i.getType().isAir() || !i.hasItemMeta()) return false;
        ItemMeta meta = i.getItemMeta();
        if (tooHigh(meta.getEnchants())) return true;
        return meta instanceof EnchantmentStorageMeta esm && tooHigh(esm.getStoredEnchants());
    }

    private static boolean tooHigh(Map<Enchantment, Integer> enchants) {
        for (Map.Entry<Enchantment, Integer> e : enchants.entrySet()) {
            if (e.getValue() > e.getKey().getMaxLevel()) return true;
        }
        return false;
    }
}
