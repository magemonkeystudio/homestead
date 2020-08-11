package com.gotofinal.darkrise.plots.util.bungee;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import com.gotofinal.darkrise.plots.DarkRisePlots;
import com.gotofinal.darkrise.plots.config.ConfigHandler;
import com.gotofinal.darkrise.plots.deeds.GlobalPlotsManager;
import com.gotofinal.darkrise.plots.deeds.Plot;
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
            String pl = in.readUTF();
            String plotName = in.readUTF();

            Player p = Bukkit.getPlayer(pl);
            if (p != null && p.isOnline()) {
                GlobalPlotsManager pm = DarkRisePlots.getInstance().getGlobalPlotsManager();
                Plot plot = pm.getPlot(plotName);

                if (plot == null) return;

                Location plotLoc = plot.getHome();
                player.teleport(plotLoc);

                OfflinePlayer targetPlayer = Bukkit.getPlayer(plot.getOwner());
                if (plot.getOwner().equalsIgnoreCase(player.getName())) {
                    MessageUtil.sendMessage("plots.commands.plot.home.own", player, new MessageData("player", player), new MessageData("plot", plot));
                } else {
                    MessageUtil.sendMessage("plots.commands.plot.home.other", player, new MessageData("player", player), new MessageData("target", targetPlayer), new MessageData("plot", plot));
                }
            } else
                JoinListener.addTPers(pl, plotName);
        });

        handlers.put("PlotMessage", (in, player) -> {
            String pname = in.readUTF();
            Player receiver = Bukkit.getPlayer(pname);
            if (receiver == null || !receiver.isOnline())
                return;

            String message = in.readUTF();
            if (message.startsWith("$")) {
//                HashMap<String, Object> replacements = new HashMap<>();
                ArrayList<MessageData> replacements = new ArrayList<>();
                String rep;
                try {
                    while ((rep = in.readUTF()) != null) {
                        String type = in.readUTF();
                        String data = type.substring(type.indexOf(':') + 1);
                        Object obj = null;
                        if (type.startsWith("p:"))
                            obj = Bukkit.getPlayer(data);
                        else if (type.startsWith("w:"))
                            obj = Bukkit.getWorld(data);
                        else if (type.startsWith("plot:")) {
                            System.out.println("Attempting to find plot " + data);
                            obj = DarkRisePlots.getInstance().getGlobalPlotsManager().getPlot(data);
                            Plot pl = ((Plot) obj);
                            System.out.println("Region: " + pl.getProtectedRegion());
                            System.out.println("Players: " + pl.getProtectedRegion().getMembers());
                        } else if (obj == null)
                            DarkRisePlots.getInstance().getLogger().severe("Could not create mapping for " + type);


                        replacements.add(new MessageData(rep, obj));
                        System.out.println("Will map " + rep + " to " + type);
                    }
                } catch (Exception e) {
                } finally {
                    replacements.add(new MessageData("sellDistance", DarkRisePlots.getInstance().getConfigHandler().getInt(ConfigHandler.SELL_DISTANCE)));
                    MessageUtil.sendMessage(message.substring(1), player,
                            replacements.toArray(new MessageData[0]));
                }
                return;
            }

            receiver.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
        });

        handlers.put("PlotsAddToRegion", (in, player) -> {
            Plot plot = DarkRisePlots.getInstance().getGlobalPlotsManager().getPlot(in.readUTF());
            if (plot == null)
                return;

            ProtectedRegion region = plot.getProtectedRegion();
            plot.addPlayer(region, in.readUTF());
        });

        handlers.put("PlotsRemoveFromRegion", (in, player) -> {
            Plot plot = DarkRisePlots.getInstance().getGlobalPlotsManager().getPlot(in.readUTF());
            if (plot == null)
                return;

            ProtectedRegion region = plot.getProtectedRegion();
            plot.removePlayer(region, in.readUTF());
        });

        handlers.put("PlotsSetOwner", (in, player) -> {
            Plot plot = DarkRisePlots.getInstance().getGlobalPlotsManager().getPlot(in.readUTF());
            if (plot == null)
                return;

            plot.setOwner(plot.getProtectedRegion(), in.readUTF());
        });

        handlers.put("FETCHPlotInfo", (in, player) -> {
            for (Plot plot : DarkRisePlots.getInstance().getGlobalPlotsManager().getAllPlots()) {
                System.out.println("Sending data for plot " + plot.getName());
                BungeeUtil.sendMessage(plot.dataToArray().toArray(new String[0]));
            }
        });

        handlers.put("PlotRunCommand", (in, player) -> {
            Player pl = Bukkit.getPlayer(in.readUTF());
            String cmd = in.readUTF();
            System.out.println("Received command: " + cmd);
            pl.performCommand(cmd);
        });
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!channel.equals(BungeeUtil.CHANNEL))
            return;

        ByteArrayDataInput in = ByteStreams.newDataInput(message);
        UUID id = UUID.fromString(in.readUTF()); // We really don't need to do anything with this, aside from sending a response.
        String sender = in.readUTF();
        BungeeUtil.sendResponse(id, "PlotsReceived");
        String one = in.readUTF();
        System.out.println("Received " + one);
        if (handlers.containsKey(one)) {
            handlers.get(one).run(in, player);
        } else
            DarkRisePlots.getInstance().getLogger().warning("Unknown Bungee Message: " + one);
    }
}
