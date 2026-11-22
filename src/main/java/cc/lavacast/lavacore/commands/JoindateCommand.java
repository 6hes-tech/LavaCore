package cc.lavacast.lavacore.commands;

import cc.lavacast.lavacore.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public final class JoindateCommand extends LavaCommand {

    public JoindateCommand() {
        super("joindate");
    }

    @Override
    protected void run(CommandSender sender, String[] args) {
        if (args.length == 0) {
            Player p = requirePlayer(sender);
            if (p == null) return;
            Msg.send(p, Msg.cfg("joindate.joindate-msg").replace("<jd>", format(p.getFirstPlayed())));
            return;
        }
        OfflinePlayer target = Bukkit.getPlayerExact(args[0]);
        if (target == null) target = Bukkit.getOfflinePlayerIfCached(args[0]);
        if (target == null || target.getFirstPlayed() <= 0) {
            Msg.send(sender, Msg.cfg("joindate.joindate-notfound"));
            return;
        }
        String name = target.getName() == null ? args[0] : target.getName();
        Msg.send(sender, Msg.cfg("joindate.joindate-other")
                .replace("<jd>", format(target.getFirstPlayed()))
                .replace("<p>", name));
    }

    private static String format(long millis) {
        String pattern = Msg.cfg("joindate.date-format");
        SimpleDateFormat fmt;
        try {
            fmt = new SimpleDateFormat(pattern.isEmpty() ? "EEEE, d MMMM yyyy - HH:mm:ss z" : pattern);
        } catch (IllegalArgumentException e) {
            fmt = new SimpleDateFormat("EEEE, d MMMM yyyy - HH:mm:ss z");
        }
        return fmt.format(new Date(millis));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return args.length == 1 ? onlineNames(sender, args[0]) : List.of();
    }
}
