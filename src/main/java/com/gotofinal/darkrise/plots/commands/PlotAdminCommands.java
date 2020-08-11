package com.gotofinal.darkrise.plots.commands;

import com.gotofinal.darkrise.plots.DarkRisePlots;
import com.gotofinal.darkrise.plots.deeds.Deed;
import com.gotofinal.darkrise.plots.deeds.Plot;
import com.gotofinal.darkrise.plots.deeds.PlotManager;
import com.gotofinal.darkrise.plots.util.bungee.BungeeCommandException;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.minecraft.util.commands.CommandPermissions;
import com.sk89q.worldguard.protection.managers.RegionManager;
import me.travja.darkrise.core.legacy.util.message.MessageData;
import me.travja.darkrise.core.legacy.util.message.MessageUtil;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Set;
import java.util.concurrent.TimeUnit;

public class PlotAdminCommands {

    private final DarkRisePlots plugin;

    public PlotAdminCommands(final DarkRisePlots instance) {
        this.plugin = instance;
    }

    @Command(aliases = {"create", "c"}, desc = "Creates a new plot for deeds", usage = "<id> <deed>", help = "The <id> argument is the id to create the plot with.\n" + "The <deed> argument is the deed to create the plot with.\n" + "The -w flag is the world to create the plot in.", min = 2, max = 2, flags = "w:")
    @CommandPermissions("pmco.cmd.plot.admin.create")
    public void create(final CommandContext args, final CommandSender sender) throws BungeeCommandException {

        final String id = args.getString(0);
        final PlotManager mgr = this.getManager(args, sender, 'w');
        final RegionManager wgMgr = mgr.getRegionManager();

        if (wgMgr == null) {
            throw new BungeeCommandException("WorldGuard is not enabled in '" + mgr.getWorld().getName() + "'.");
        }

        if (!wgMgr.hasRegion(id)) {
            throw new BungeeCommandException("No WorldGuard region found by id '" + id + "'.");
        }

        if (mgr.hasPlot(id)) {
            throw new BungeeCommandException("A plot with the id '" + id + "' already exists.");
        }

        final String deedName = args.getString(1);
        final Deed deed = this.plugin.getGlobalPlotsManager().getDeedType(deedName);
        if (deed == null) {
            throw new BungeeCommandException("No deed called '" + deedName + "' could be found.");
        }

        final Plot plot = new Plot(id);
        plot.setDeed(deed);
        mgr.addPlot(plot);
        sender.sendMessage(ChatColor.YELLOW + "You created a new plot with the id '" + id + "'.");
    }


    @Command(aliases = {"remove", "rm"}, desc = "Removes a plot.", usage = "<id>", help = "The <id> argument is the id to remove the plot with.\n" + "The -w flag is the world to check for the plot in.", min = 1, max = 1, flags = "w:")
    @CommandPermissions("pmco.cmd.plot.admin.remove")
    public void remove(final CommandContext args, final CommandSender sender) throws BungeeCommandException {

        final String id = args.getString(0);
        final PlotManager mgr = this.getManager(args, sender, 'w');
        this.getPlot(mgr, id);

        mgr.removePlot(id);
        sender.sendMessage(ChatColor.YELLOW + "You removed plot with the id '" + id + "'.");
    }

    // added by GotoFinal start

    @Command(aliases = {"sethome"}, desc = "Set home location of plot.", usage = "<id>", help = "The <id> argument is the id to create the plot with.", min = 1, max = 1)
    @CommandPermissions("pmco.cmd.plot.admin.sethome")
    public void sethome(final CommandContext args, final CommandSender sender) throws BungeeCommandException {
        if (!(sender instanceof Player)) {
            throw new BungeeCommandException("Only players can use this command.");
        }
        final Player player = (Player) sender;
        final String id = args.getString(0);
        final PlotManager mgr = this.getManager(args, sender, 'w');
        final Location loc = player.getLocation().clone();
        Plot plot = this.getPlot(mgr, id);
        plot.setHome(loc);
        MessageUtil.sendMessage("plots.commands.plot.admin.setHome", sender, new MessageData("plot", plot), new MessageData("location", loc));
    }

    // added by GotoFinal stop

    @SuppressWarnings("deprecation")
    @Command(aliases = {"sign"}, desc = "Sets the sign location of a plot.", usage = "<id>", help = "Sets the sign location of a plot.\n" + "The <id> argument is the id of the plot to modify.", min = 1, max = 1)
    @CommandPermissions("pmco.cmd.plot.admin.sign")
    public void sign(final CommandContext args, final CommandSender sender) throws BungeeCommandException {

        if (!(sender instanceof Player)) {
            throw new BungeeCommandException("Only players can use this command.");
        }

        final Player player = (Player) sender;
        final PlotManager mgr = this.getManager(player.getWorld());
        final String id = args.getString(0);

        final Plot plot = this.getPlot(mgr, id);

        final Block block = player.getTargetBlock(null, 6);

        if (block == null || !block.getType().toString().contains("SIGN")) {
            throw new BungeeCommandException("You are not looking at a sign.");
        }

        plot.setSignLocation(block.getLocation());
        sender.sendMessage(ChatColor.YELLOW + "You successfully set the sign location for plot '" + id + "'.");
    }

    @Command(aliases = {"deed", "type"}, desc = "Sets the deed type of a plot.", usage = "<id> <deed>", help = "Sets the deed type of a plot.\n" + "The <id> argument is the id of the plot to modify.\n" + "The <deed> argument is the new deed this plot should be assigned.\n" + "The -w flag is the world to check for the plot in.", min = 2, max = 2, flags = "w:")
    @CommandPermissions("pmco.cmd.plot.admin.deed")
    public void deed(final CommandContext args, final CommandSender sender) throws BungeeCommandException {

        final PlotManager mgr = this.getManager(args, sender, 'w');
        final String id = args.getString(0);
        final Plot plot = this.getPlot(mgr, id);
        final Deed deed = this.plugin.getGlobalPlotsManager().getDeedType(args.getString(1));

        if (deed == null) {
            throw new BungeeCommandException("No deed called '" + args.getString(1) + "' could be found.");
        }

        plot.setDeed(deed);
        sender.sendMessage(ChatColor.YELLOW + "You set '" + plot.getName() + "'s deed to '" + deed.getName() + "'.");
    }

    @Command(aliases = {"expiry"}, desc = "Sets the expiry time of a plot.", usage = "<id> <seconds>", help = "Sets the expiry time of a plot.\n" + "The <id> argument is the id of the plot to modify.\n" + "The <seconds> argument is how many seconds from the time the command is executed, " + "the plot should expire, considering it is going to expire.\n" + "The -w flag is the world to check for the plot in.", min = 2, max = 2, flags = "w:")
    @CommandPermissions("pmco.cmd.plot.admin.expiry")
    public void expiry(final CommandContext args, final CommandSender sender) throws BungeeCommandException {

        final PlotManager mgr = this.getManager(args, sender, 'w');
        final String id = args.getString(0);
        final Plot plot = this.getPlot(mgr, id);

        if (!plot.isOwned()) {
            throw new BungeeCommandException("That plot is not owned by anyone.");
        }
        final int seconds = args.getInteger(1);
        final long expiryTime = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(seconds);
        plot.setExpiry(expiryTime);
        plot.setFinalExpiry(expiryTime);
        sender.sendMessage(ChatColor.YELLOW + "'" + plot.getName() + "' is going to expire in " + seconds + ".");
    }

    @Command(aliases = {"owner"}, desc = "Sets the owner of a plot.", usage = "<id> <owner>", help = "Sets the owner of a plot.\n" + "The <id> argument is the id of the plot to modify.\n" + "The <owner> argument is the name of the player you wish to give owner perms for the plot.\n" + "The -w flag is the world to check for the plot in.", min = 2, max = 2, flags = "w:")
    @CommandPermissions("pmco.cmd.plot.admin.owner")
    public void owner(final CommandContext args, final CommandSender sender) throws BungeeCommandException {

        final PlotManager mgr = this.getManager(args, sender, 'w');
        final String id = args.getString(0);
        final Plot plot = this.getPlot(mgr, id);
        final String owner = args.getString(1);

        plot.setOwner(plot.getProtectedRegion(), owner);
        plot.setExpiry(System.currentTimeMillis());
        plot.extendExpiry();

        sender.sendMessage(ChatColor.YELLOW + "'" + plot.getName() + "'s new owner is '" + owner + "'.");
    }

    @Command(aliases = {"addplayer", "add"}, desc = "Adds a player to a plot.", usage = "<id> <player>", help = "Adds a player to a plot.\n" + "The <id> argument is the id of the plot to modify.\n" + "The <player> argument is the name of the player you wish to add to the plot.\n" + "The -w flag is the world to check for the plot in.", min = 2, max = 2, flags = "w:")
    @CommandPermissions("pmco.cmd.plot.admin.addplayer")
    public void addPlayer(final CommandContext args, final CommandSender sender) throws BungeeCommandException {

        final PlotManager mgr = this.getManager(args, sender, 'w');
        final String id = args.getString(0);
        final Plot plot = this.getPlot(mgr, id);
        final String playerToAdd = args.getString(1);

        if (plot.hasPlayer(playerToAdd)) {
            throw new BungeeCommandException(playerToAdd + " is already added to plot '" + id + "'.");
        }

        plot.addPlayer(plot.getProtectedRegion(), playerToAdd);
        sender.sendMessage(ChatColor.YELLOW + "You successfully added '" + playerToAdd + "' to plot '" + id + "'.");
    }

    @Command(aliases = {"removeplayer", "rmplayer", "rmp"}, desc = "Removes a player from a plot.", usage = "<id> <player>", help = "Removes a player from a plot.\n" + "The <id> argument is the id of the plot to modify.\n" + "The <player> argument is the name of the player you wish to remove from the plot.\n" + "The -w flag is the world to check for the plot in.", min = 2, max = 2, flags = "w:")
    @CommandPermissions("pmco.cmd.plot.admin.removeplayer")
    public void removePlayer(final CommandContext args, final CommandSender sender) throws BungeeCommandException {

        final PlotManager mgr = this.getManager(args, sender, 'w');
        final String id = args.getString(0);
        final Plot plot = this.getPlot(mgr, id);
        final String playerToRemove = args.getString(1);

        if (!plot.hasPlayer(playerToRemove)) {
            throw new BungeeCommandException(playerToRemove + " is not added to plot '" + id + "'.");
        }

        plot.removePlayer(plot.getProtectedRegion(), playerToRemove);
        sender.sendMessage(ChatColor.YELLOW + "You successfully removed '" + playerToRemove + "' from plot '" + id + "'" + ".");
    }

    @Command(aliases = {"update"}, desc = "Updates a plot's sign.", usage = "<id>", help = "Updates a plot's sign.\n" + "The <id> argument is the id of the plot to modify.\n" + "The -w flag is the world to check for the plot in.", min = 1, max = 1, flags = "w:")
    @CommandPermissions("pmco.cmd.plot.admin.update")
    public void update(final CommandContext args, final CommandSender sender) throws BungeeCommandException {

        final PlotManager mgr = this.getManager(args, sender, 'w');
        final String id = args.getString(0);
        final Plot plot = this.getPlot(mgr, id);

        if (plot.getSignLocation() == null) {
            throw new BungeeCommandException(plot.getName() + " does not have a sign.");
        }

        final BlockState state = plot.getSignLocation().getBlock().getState();

        if (!(state instanceof Sign)) {
            throw new BungeeCommandException("The sign location is not a sign, please update the sign location.");
        }
        plot.updateSign((Sign) state);
        sender.sendMessage(ChatColor.YELLOW + plot.getName() + "'s sign updated.");
    }

    private PlotManager getManager(final CommandContext args, final CommandSender sender, final char flag) throws BungeeCommandException {

        final World world;
        if (!args.hasFlag('w')) {
            if (!(sender instanceof Player)) {
                throw new BungeeCommandException("Please specify a world.");
            } else {
                world = ((Player) sender).getWorld();
            }
        } else {
            final String name = args.getFlag(flag);
            world = Bukkit.getWorld(name);
            if (world == null) {
                throw new BungeeCommandException("'" + name + "' is not a valid world.");
            }
        }

        return this.getManager(world);
    }

    private PlotManager getManager(final World world) throws BungeeCommandException {

        final PlotManager mgr = this.plugin.getGlobalPlotsManager().getPlotManager(world);

        if (mgr == null) {
            throw new BungeeCommandException("House deeds is not enabled in '" + world.getName() + "'");
        }

        return mgr;
    }

    private Plot getPlot(final PlotManager mgr, final String id) throws BungeeCommandException {

        final Plot plot = mgr.getPlot(id);
        if (plot == null) {
            throw new BungeeCommandException("No plot found by that id.");
        }
        return plot;
    }


    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE).appendSuper(super.toString()).append("plugin", this.plugin).toString();
    }
}
