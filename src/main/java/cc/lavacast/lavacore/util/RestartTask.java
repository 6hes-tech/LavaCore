package cc.lavacast.lavacore.util;

import cc.lavacast.lavacore.LavaCore;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.time.*;
import java.util.*;

/**
 * Restarts the server every N hours after startup, or daily at a fixed time.
 * Reloading the config doesn't reset the countdown, it's always measured from server start.
 */
public final class RestartTask {

    private static final long MIN_UPTIME_MS = 30 * 60 * 1000L;

    private final LavaCore plugin;
    private final long startedAt = System.currentTimeMillis();
    private final Set<Long> warned = new HashSet<>();
    private BukkitTask task;
    private long restartAt = -1;
    private boolean restarting;

    public RestartTask(LavaCore plugin) {
        this.plugin = plugin;
    }

    public void start() {
        stop();
        FileConfiguration c = plugin.getConfig();
        if (!c.getBoolean("auto-restart.enabled")) {
            restartAt = -1;
            return;
        }
        restartAt = computeRestartAt(c);
        if (restartAt < 0) return;

        // don't fire every warning we're already past (e.g. after a reload)
        warned.clear();
        long left = restartAt - System.currentTimeMillis();
        for (long w : warnings()) {
            if (left <= w * 1000L) warned.add(w);
        }

        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
        plugin.getLogger().info("Next auto restart in " + Msg.duration(left / 1000) + ".");
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    /** Milliseconds until restart, or -1 if auto restart is off. */
    public long millisLeft() {
        if (restartAt < 0) return -1;
        return Math.max(0, restartAt - System.currentTimeMillis());
    }

    private long computeRestartAt(FileConfiguration c) {
        String daily = c.getString("auto-restart.daily-time", "").trim();
        if (!daily.isEmpty()) {
            try {
                String tz = c.getString("auto-restart.timezone", "").trim();
                ZoneId zone = tz.isEmpty() ? ZoneId.systemDefault() : ZoneId.of(tz);
                LocalTime time = LocalTime.parse(daily);
                ZonedDateTime now = ZonedDateTime.now(zone);
                ZonedDateTime next = now.with(time);
                if (!next.isAfter(now)) next = next.plusDays(1);
                long at = next.toInstant().toEpochMilli();
                // if we booted right before the daily time, skip to tomorrow instead of restarting again
                if (at - startedAt < MIN_UPTIME_MS) at = next.plusDays(1).toInstant().toEpochMilli();
                return at;
            } catch (DateTimeException e) {
                plugin.getLogger().warning("auto-restart.daily-time/timezone is invalid (" + e.getMessage()
                        + "), using interval-hours instead.");
            }
        }
        double hours = c.getDouble("auto-restart.interval-hours", 24);
        if (hours <= 0) {
            plugin.getLogger().warning("auto-restart.interval-hours must be above 0, auto restart is off.");
            return -1;
        }
        return startedAt + (long) (hours * 3_600_000L);
    }

    private List<Long> warnings() {
        List<Long> out = new ArrayList<>();
        for (Object o : plugin.getConfig().getList("auto-restart.warnings", List.of())) {
            if (o instanceof Number n && n.longValue() > 0) out.add(n.longValue());
        }
        out.sort(Comparator.reverseOrder());
        return out;
    }

    private void tick() {
        long left = restartAt - System.currentTimeMillis();
        if (left <= 0) {
            restartNow();
            return;
        }
        boolean announce = false;
        for (long w : warnings()) {
            if (left <= w * 1000L && warned.add(w)) announce = true;
        }
        if (announce) {
            long secs = (long) Math.ceil(left / 1000.0);
            Component msg = Msg.color(Msg.cfg("auto-restart.warning-message").replace("<time>", Msg.duration(secs)));
            Bukkit.getServer().sendMessage(msg);
        }
    }

    public void restartNow() {
        if (restarting) return;
        restarting = true;
        stop();
        plugin.getLogger().info("Auto restarting the server.");
        Component kick = Msg.color(Msg.cfg("auto-restart.kick-message"));
        for (Player p : new ArrayList<>(Bukkit.getOnlinePlayers())) p.kick(kick);
        plugin.data().saveSync();

        String method = plugin.getConfig().getString("auto-restart.method", "restart");
        if ("restart".equalsIgnoreCase(method)) {
            // uses spigot.yml settings.restart-script, falls back to a plain stop if it isn't set up
            Bukkit.getServer().restart();
        } else {
            Bukkit.shutdown();
        }
    }
}
