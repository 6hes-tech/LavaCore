package cc.lavacast.lavacore.util;

import cc.lavacast.lavacore.LavaCore;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitTask;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Cached numbers for /stats: world size, total players, server creation date.
 * World size means walking every world folder, so it's done off the main thread on a timer
 * instead of every time someone runs /stats.
 */
public final class ServerStats {

    private final LavaCore plugin;
    private final AtomicLong worldBytes = new AtomicLong(-1);
    private volatile int totalPlayers = -1;
    private volatile long earliestJoin = -1;
    private BukkitTask task;

    public ServerStats(LavaCore plugin) {
        this.plugin = plugin;
    }

    public void start() {
        stop();
        long minutes = Math.max(1, plugin.getConfig().getLong("stats.refresh-minutes", 10));
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::refresh, 40L, minutes * 60L * 20L);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    /** Main thread part (Bukkit API), then the folder walk goes async. */
    private void refresh() {
        OfflinePlayer[] players = Bukkit.getOfflinePlayers();
        long earliest = Long.MAX_VALUE;
        for (OfflinePlayer p : players) {
            long first = p.getFirstPlayed();
            if (first > 0 && first < earliest) earliest = first;
        }
        totalPlayers = players.length;
        earliestJoin = earliest == Long.MAX_VALUE ? -1 : earliest;

        List<Path> folders = new ArrayList<>();
        for (World w : Bukkit.getWorlds()) folders.add(w.getWorldFolder().toPath());
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            long total = 0;
            for (Path f : folders) total += folderSize(f);
            worldBytes.set(total);
        });
    }

    private static long folderSize(Path root) {
        AtomicLong size = new AtomicLong();
        try {
            Files.walkFileTree(root, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    size.addAndGet(attrs.size());
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    return FileVisitResult.CONTINUE; // region files get rewritten while saving, skip them
                }
            });
        } catch (IOException ignored) {
        }
        return size.get();
    }

    public String worldSize() {
        long b = worldBytes.get();
        if (b < 0) return "calculating...";
        double mb = b / 1024.0 / 1024.0;
        if (mb < 1024) return String.format("%.2f MB", mb);
        double gb = mb / 1024.0;
        if (gb < 1024) return String.format("%.2f GB", gb);
        return String.format("%.2f TB", gb / 1024.0);
    }

    public String totalPlayers() {
        return totalPlayers < 0 ? "?" : String.valueOf(totalPlayers);
    }

    /** stats.created-date from config, or the first time anyone joined if that's blank. */
    private LocalDate createdDate() {
        String raw = plugin.getConfig().getString("stats.created-date", "").trim();
        if (!raw.isEmpty()) {
            try {
                return LocalDate.parse(raw);
            } catch (DateTimeParseException e) {
                plugin.getLogger().warning("stats.created-date must look like 2026-11-22, got '" + raw + "'");
            }
        }
        if (earliestJoin < 0) return null;
        return Instant.ofEpochMilli(earliestJoin).atZone(ZoneId.systemDefault()).toLocalDate();
    }

    public String created() {
        LocalDate d = createdDate();
        if (d == null) return "?";
        String pattern = plugin.getConfig().getString("stats.date-format", "MM/dd/yyyy");
        try {
            return d.format(DateTimeFormatter.ofPattern(pattern));
        } catch (IllegalArgumentException e) {
            return d.format(DateTimeFormatter.ofPattern("MM/dd/yyyy"));
        }
    }

    public String daysSinceCreated() {
        LocalDate d = createdDate();
        if (d == null) return "?";
        return String.valueOf(ChronoUnit.DAYS.between(d, LocalDate.now()));
    }
}
