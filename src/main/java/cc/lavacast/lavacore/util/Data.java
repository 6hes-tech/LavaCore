package cc.lavacast.lavacore.util;

import cc.lavacast.lavacore.LavaCore;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;

/**
 * Player data saved to plugins/LavaCore/data.yml. Thread safe for reads, since chat is async.
 */
public final class Data {

    private final LavaCore plugin;
    private final File file;

    private final Map<UUID, Set<UUID>> mutes = new ConcurrentHashMap<>();
    private final Map<UUID, String> nameStyles = new ConcurrentHashMap<>();
    private final Set<UUID> hideDeaths = ConcurrentHashMap.newKeySet();
    private final Set<UUID> hideJoins = ConcurrentHashMap.newKeySet();

    private final AtomicLong version = new AtomicLong();
    private long writtenVersion = -1;

    public Data(LavaCore plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "data.yml");
    }

    public void load() {
        mutes.clear();
        nameStyles.clear();
        hideDeaths.clear();
        hideJoins.clear();
        if (!file.exists()) return;

        YamlConfiguration y = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection ms = y.getConfigurationSection("mutes");
        if (ms != null) {
            for (String key : ms.getKeys(false)) {
                UUID owner = parse(key);
                if (owner == null) continue;
                Set<UUID> set = ConcurrentHashMap.newKeySet();
                for (String s : ms.getStringList(key)) {
                    UUID u = parse(s);
                    if (u != null) set.add(u);
                }
                if (!set.isEmpty()) mutes.put(owner, set);
            }
        }
        ConfigurationSection ns = y.getConfigurationSection("name-colors");
        if (ns != null) {
            for (String key : ns.getKeys(false)) {
                UUID owner = parse(key);
                String style = ns.getString(key);
                if (owner != null && style != null && !style.isEmpty()) nameStyles.put(owner, style);
            }
        }
        for (String s : y.getStringList("hide-death-messages")) {
            UUID u = parse(s);
            if (u != null) hideDeaths.add(u);
        }
        for (String s : y.getStringList("hide-join-messages")) {
            UUID u = parse(s);
            if (u != null) hideJoins.add(u);
        }
    }

    private static UUID parse(String s) {
        try {
            return UUID.fromString(s);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private String serialize() {
        YamlConfiguration y = new YamlConfiguration();
        for (Map.Entry<UUID, Set<UUID>> e : mutes.entrySet()) {
            if (e.getValue().isEmpty()) continue;
            List<String> list = new ArrayList<>();
            for (UUID u : e.getValue()) list.add(u.toString());
            y.set("mutes." + e.getKey(), list);
        }
        for (Map.Entry<UUID, String> e : nameStyles.entrySet()) {
            y.set("name-colors." + e.getKey(), e.getValue());
        }
        y.set("hide-death-messages", hideDeaths.stream().map(UUID::toString).toList());
        y.set("hide-join-messages", hideJoins.stream().map(UUID::toString).toList());
        return y.saveToString();
    }

    /** Snapshot on the calling thread, write the file off the main thread. */
    public void saveAsync() {
        long v = version.incrementAndGet();
        String out = serialize();
        if (!plugin.isEnabled()) {
            write(out, v);
            return;
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> write(out, v));
    }

    public void saveSync() {
        write(serialize(), version.incrementAndGet());
    }

    private synchronized void write(String out, long v) {
        if (v < writtenVersion) return; // a newer snapshot already hit the disk
        try {
            Files.createDirectories(plugin.getDataFolder().toPath());
            Path tmp = new File(plugin.getDataFolder(), "data.yml.tmp").toPath();
            Files.writeString(tmp, out, StandardCharsets.UTF_8);
            try {
                Files.move(tmp, file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException atomicFailed) {
                Files.move(tmp, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            writtenVersion = v;
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Couldn't save data.yml", e);
        }
    }

    // mutes

    public boolean isMuted(UUID viewer, UUID sender) {
        Set<UUID> set = mutes.get(viewer);
        return set != null && set.contains(sender);
    }

    public boolean mute(UUID owner, UUID target) {
        boolean added = mutes.computeIfAbsent(owner, k -> ConcurrentHashMap.newKeySet()).add(target);
        if (added) saveAsync();
        return added;
    }

    public boolean unmute(UUID owner, UUID target) {
        Set<UUID> set = mutes.get(owner);
        if (set == null || !set.remove(target)) return false;
        if (set.isEmpty()) mutes.remove(owner);
        saveAsync();
        return true;
    }

    public Set<UUID> mutesOf(UUID owner) {
        Set<UUID> set = mutes.get(owner);
        return set == null ? Set.of() : Set.copyOf(set);
    }

    // name colors

    public String nameStyle(UUID player) {
        return nameStyles.get(player);
    }

    public void setNameStyle(UUID player, String style) {
        nameStyles.put(player, style);
        saveAsync();
    }

    public void clearNameStyle(UUID player) {
        if (nameStyles.remove(player) != null) saveAsync();
    }

    // toggles

    public boolean hidesDeaths(UUID player) {
        return hideDeaths.contains(player);
    }

    public boolean hidesJoins(UUID player) {
        return hideJoins.contains(player);
    }

    /** @return true if death messages are now hidden for this player */
    public boolean toggleDeaths(UUID player) {
        boolean nowHidden = hideDeaths.add(player) || !hideDeaths.remove(player);
        saveAsync();
        return nowHidden;
    }

    /** @return true if join messages are now hidden for this player */
    public boolean toggleJoins(UUID player) {
        boolean nowHidden = hideJoins.add(player) || !hideJoins.remove(player);
        saveAsync();
        return nowHidden;
    }
}
