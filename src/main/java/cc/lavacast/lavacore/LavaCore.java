package cc.lavacast.lavacore;

import cc.lavacast.lavacore.commands.*;
import cc.lavacast.lavacore.listeners.*;
import cc.lavacast.lavacore.util.Data;
import cc.lavacast.lavacore.util.PapiHook;
import cc.lavacast.lavacore.util.RestartTask;
import cc.lavacast.lavacore.util.ServerStats;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class LavaCore extends JavaPlugin {

    private static LavaCore instance;
    private Data data;
    private RestartTask restartTask;
    private ServerStats stats;
    private HelpCommand helpCommand;

    public static LavaCore get() {
        return instance;
    }

    public Data data() {
        return data;
    }

    public RestartTask restartTask() {
        return restartTask;
    }

    public ServerStats stats() {
        return stats;
    }

    public HelpCommand helpCommand() {
        return helpCommand;
    }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        addMissingConfigKeys();
        PapiHook.init();

        data = new Data(this);
        data.load();

        restartTask = new RestartTask(this);
        restartTask.start();

        stats = new ServerStats(this);
        stats.start();

        registerCommands();
        registerListeners();
        getLogger().info("LavaCore enabled.");
    }

    @Override
    public void onDisable() {
        if (restartTask != null) restartTask.stop();
        if (stats != null) stats.stop();
        if (data != null) data.saveSync();
    }

    public void reload() {
        reloadConfig();
        addMissingConfigKeys();
        restartTask.start();
        stats.start();
        for (Player p : Bukkit.getOnlinePlayers()) p.updateCommands();
    }

    /** Writes options added in newer versions into an existing config.yml, without touching values already set. */
    private void addMissingConfigKeys() {
        getConfig().options().copyDefaults(true);
        saveConfig();
    }

    private void registerListeners() {
        PluginManager pm = getServer().getPluginManager();
        Listener[] listeners = {
                new AntiSpamListener(this),
                new ChatListener(this),
                new JoinQuitListener(this),
                new DeathListener(this),
                new SpawnListener(this),
                new CommandWhitelistListener(this),
                new MotdListener(this),
                new DispenserListener(this),
                new BlockPlaceListener(this),
                new IllegalItemListener(this),
                new DamageListener(this),
                new NetherLimitListener(this),
                new Salc1Listener(this)
        };
        for (Listener l : listeners) pm.registerEvents(l, this);
    }

    private void registerCommands() {
        helpCommand = new HelpCommand("help", "help-command");
        register("help", helpCommand);
        register("stats", new HelpCommand("stats", "stats-command"));
        register("bed", new BedCommand());
        register("discord", new DiscordCommand());
        register("joindate", new JoindateCommand());
        register("ping", new PingCommand());
        register("toggledeathmsgs", new ToggleCommand("toggledeathmsgs", ToggleCommand.Type.DEATHS));
        register("togglejoinmsgs", new ToggleCommand("togglejoinmsgs", ToggleCommand.Type.JOINS));
        register("corereload", new ReloadCommand());
        register("kill", new KillCommand());
        register("nc", new NameColorCommand());
        register("mute", new MuteCommand("mute", MuteCommand.Mode.MUTE));
        register("unmute", new MuteCommand("unmute", MuteCommand.Mode.UNMUTE));
        register("mutelist", new MuteCommand("mutelist", MuteCommand.Mode.LIST));
        register("msg", new MsgCommand());
        register("reply", new ReplyCommand());
    }

    private void register(String name, LavaCommand executor) {
        PluginCommand cmd = getCommand(name);
        if (cmd == null) {
            getLogger().severe("Command /" + name + " is missing from plugin.yml");
            return;
        }
        cmd.setExecutor(executor);
        cmd.setTabCompleter(executor);
    }
}
