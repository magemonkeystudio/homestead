package studio.magemonkey.homestead.util.bungee;

import studio.magemonkey.codex.util.messages.MessageData;
import studio.magemonkey.codex.util.messages.MessageUtil;
import studio.magemonkey.homestead.Homestead;
import studio.magemonkey.homestead.deeds.GlobalPlotsManager;
import studio.magemonkey.homestead.deeds.Plot;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.HashMap;

public class JoinListener implements Listener {

    private static HashMap<String, String> toJoin = new HashMap<>();

    protected static void addTPers(String player, String plotHome) {
        toJoin.put(player, plotHome);
    }

    @EventHandler
    public void join(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!toJoin.containsKey(player.getName())) return;

        GlobalPlotsManager pm     = Homestead.getInstance().getGlobalPlotsManager();
        String             target = toJoin.get(player.getName());
        Plot               plot   = pm.getPlot(Bukkit.getPlayer(target));

        if (plot == null) return;

        Location plotLoc = plot.getHome();
        player.teleport(plotLoc);

        OfflinePlayer targetPlayer = Bukkit.getPlayer(plot.getOwner());
        if (plot.getOwner().equalsIgnoreCase(player.getName())) {
            MessageUtil.sendMessage("plots.commands.plot.home.own",
                    player,
                    new MessageData("player", player),
                    new MessageData("plot", plot));
        } else {
            MessageUtil.sendMessage("plots.commands.plot.home.other",
                    player,
                    new MessageData("player", player),
                    new MessageData("target", targetPlayer),
                    new MessageData("plot", plot));
        }
    }

}
