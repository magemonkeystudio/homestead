package com.promcteam.homestead.util.bungee;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import com.promcteam.homestead.Homestead;
import com.promcteam.homestead.commands.PlotCommands;
import com.promcteam.homestead.config.ConfigHandler;
import com.promcteam.homestead.deeds.GlobalPlotsManager;
import com.promcteam.homestead.deeds.Plot;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import me.travja.darkrise.core.bungee.BungeeUtil;
import me.travja.darkrise.core.legacy.util.message.MessageData;
import me.travja.darkrise.core.legacy.util.message.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

public class BungeeListener implements PluginMessageListener {

    private HashMap<String, MessageHandler> handlers = new HashMap<>();

    {
        handlers.put("TPPlotHome", (in, player) -> {
            String pl       = in.readUTF();
            String plotName = in.readUTF();

            Player p = Bukkit.getPlayer(pl);
            if (p != null && p.isOnline()) {
                GlobalPlotsManager pm   = Homestead.getInstance().getGlobalPlotsManager();
                Plot               plot = pm.getPlot(plotName);

                if (plot == null) return;

                Location plotLoc = plot.getHome();
                player.teleport(plotLoc);
                PlotCommands.removeWarmup(player);

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
            } else
                JoinListener.addTPers(pl, plotName);
        });

        handlers.put("PlotMessage", (in, player) -> {
            String pname    = in.readUTF();
            Player receiver = Bukkit.getPlayer(pname);
            if (receiver == null || !receiver.isOnline())
                return;

            String message = in.readUTF();
            if (message.startsWith("$")) {
//                HashMap<String, Object> replacements = new HashMap<>();
                ArrayList<MessageData> replacements = new ArrayList<>();
                String                 rep;
                try {
                    while ((rep = in.readUTF()) != null) {
                        String type = in.readUTF();
                        String data = type.substring(type.indexOf(':') + 1);
                        Object obj  = null;
                        if (type.startsWith("p:"))
                            obj = Bukkit.getPlayer(data);
                        else if (type.startsWith("w:"))
                            obj = Bukkit.getWorld(data);
                        else if (type.startsWith("plot:")) {
                            obj = Homestead.getInstance().getGlobalPlotsManager().getPlot(data);
                            Plot pl = ((Plot) obj);
                        } else
                            obj = type;


                        if (obj == null)
                            Homestead.getInstance().getLogger().severe("Could not create mapping for " + type);


                        replacements.add(new MessageData(rep, obj));
                    }
                } catch (Exception e) {
                } finally {
                    replacements.add(new MessageData("sellDistance", Homestead.getInstance().getConfigHandler().getInt(
                            ConfigHandler.SELL_DISTANCE)));
                    MessageUtil.sendMessage(message.substring(1), player,
                            replacements.toArray(new MessageData[0]));
                }
                return;
            }

            receiver.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
        });

        handlers.put("PlotsAddToRegion", (in, player) -> {
            Plot plot = Homestead.getInstance().getGlobalPlotsManager().getPlot(in.readUTF());
            if (plot == null)
                return;

            ProtectedRegion region = plot.getProtectedRegion();
            plot.addPlayer(region, in.readUTF());
        });

        handlers.put("PlotsRemoveFromRegion", (in, player) -> {
            Plot plot = Homestead.getInstance().getGlobalPlotsManager().getPlot(in.readUTF());
            if (plot == null)
                return;

            ProtectedRegion region = plot.getProtectedRegion();
            plot.removePlayer(region, in.readUTF());
        });

        handlers.put("PlotsSetOwner", (in, player) -> {
            Plot plot = Homestead.getInstance().getGlobalPlotsManager().getPlot(in.readUTF());
            if (plot == null)
                return;

            plot.setOwner(plot.getProtectedRegion(), in.readUTF());
        });

        handlers.put("FETCHPlotInfo", (in, player) -> {
            for (Plot plot : Homestead.getInstance().getGlobalPlotsManager().getAllPlots()) {
                BungeeUtil.sendMessage(plot.dataToArray().toArray(new String[0]));
            }
        });

        handlers.put("PlotRunCommand", (in, player) -> {
            Player pl  = Bukkit.getPlayer(in.readUTF());
            String cmd = in.readUTF();
            pl.performCommand(cmd);
        });

        handlers.put("PlotsWarmingUp", (in, player) -> {
            PlotCommands.addWarmup(player);
        });
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!channel.equals(BungeeUtil.CHANNEL))
            return;

        ByteArrayDataInput in     = ByteStreams.newDataInput(message);
        UUID               id     =
                UUID.fromString(in.readUTF()); // We really don't need to do anything with this, aside from sending a response.
        String             sender = in.readUTF();
        BungeeUtil.sendResponse(id, "PlotsReceived");
        String one = in.readUTF();
        if (handlers.containsKey(one)) {
            handlers.get(one).run(in, player);
        } else
            Homestead.getInstance().getLogger().warning("Unknown Bungee Message: " + one);
    }
}
