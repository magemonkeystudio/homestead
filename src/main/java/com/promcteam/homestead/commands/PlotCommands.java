package com.promcteam.homestead.commands;

import com.promcteam.codex.CodexEngine;
import com.promcteam.homestead.Homestead;
import com.promcteam.homestead.config.ConfigHandler;
import com.promcteam.homestead.deeds.Plot;
import com.promcteam.homestead.deeds.PlotManager;
import com.promcteam.homestead.util.Util;
import com.promcteam.homestead.util.bungee.BungeeCommandException;
import com.promcteam.homestead.util.pagination.SimplePaginatedResult;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandPermissions;
import com.sk89q.minecraft.util.commands.NestedCommand;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.promcteam.risecore.legacy.util.message.MessageData;
import com.promcteam.risecore.legacy.util.message.MessageUtil;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class PlotCommands {

    private static final String                        ADD_LIMIT    = "add.limit";
    private final        Homestead                     plugin;
    private final        HashMap<String, PlotPurchase> plotsForSale = new HashMap<>(50);

    public PlotCommands(final Homestead instance) {
        this.plugin = instance;
    }

    @Command(aliases = {"add"}, desc = "This command adds a player to the command sender's plot.", usage = "<player>", min = 1, max = 1)
    public void addPlayer(final CommandContext args, final CommandSender sender) throws BungeeCommandException {

        if (!(sender instanceof Player)) {
            throw new BungeeCommandException("Only players can use this command.");
        }

        final Player player = (Player) sender;
        final Plot   plot   = this.getPlot(player);
        final int    limit  = plot.getDeed().getFriends();

        if (plot.getPlayers().size() >= limit) {
            throw new BungeeCommandException("You can not add anymore players to your plot.");
        }

        final String playerToAdd = args.getString(0);

        if (plot.isOwner(playerToAdd)) {
            throw new BungeeCommandException("You can not add the owner to the plot.");
        }

        if (plot.hasPlayer(playerToAdd)) {
            throw new BungeeCommandException("That player is already added to your plot.");
        }

        final ProtectedRegion region = plot.getProtectedRegion();
        plot.addPlayer(region, playerToAdd);
        player.sendMessage(
                ChatColor.YELLOW + "You successfully added " + ChatColor.AQUA + playerToAdd + ChatColor.YELLOW
                        + " to your plot.");
        final Player targetPlayer = Bukkit.getPlayer(playerToAdd);

        if (targetPlayer != null) {
            targetPlayer.sendMessage(ChatColor.YELLOW + player.getName() + " added you to his plot.");
        }
    }

    @Command(aliases = {"remove"}, desc = "This command removes a player from the command sender's plot.", usage = "<player>", min = 1, max = 1)
    @CommandPermissions("homestead.cmd.plot.remove")
    public void removePlayer(final CommandContext args, final CommandSender sender) throws BungeeCommandException {

        if (!(sender instanceof Player)) {
            throw new BungeeCommandException("Only players can use this command.");
        }
        final Player player         = (Player) sender;
        final Plot   plot           = this.getPlot(player);
        final String playerToRemove = args.getString(0);
        if (!plot.hasPlayer(playerToRemove)) {
            throw new BungeeCommandException("That player is not added to your plot.");
        }
        final ProtectedRegion region = plot.getProtectedRegion();
        plot.removePlayer(region, playerToRemove);
        player.sendMessage(
                ChatColor.YELLOW + "You successfully removed " + ChatColor.AQUA + playerToRemove + ChatColor.YELLOW
                        + " from your plot.");
        final Player targetPlayer = Bukkit.getPlayer(playerToRemove);

        if (targetPlayer != null) {
            targetPlayer.sendMessage(ChatColor.YELLOW + player.getName() + " removed you from his plot.");
        }
    }

    // added by GotoFinal start.

    @Command(aliases = {"players"}, desc = "This command print list of players added to the command sender's plot.", min = 0, max = 0)
    @CommandPermissions("homestead.cmd.plot.players")
    public void players(final CommandContext args, final CommandSender sender) throws BungeeCommandException {
        if (!(sender instanceof Player)) {
            throw new BungeeCommandException("Only players can use this command.");
        }
        final Player player = (Player) sender;

        Plot plot = this.getPlot(player);
        MessageUtil.sendMessage("plots.commands.plot.players.list", sender, new MessageData("plot", plot));
    }

    @Command(aliases = {"home"}, desc = "Teleport the command sender's plot.", usage = "[player]", min = 0, max = 1)
    @CommandPermissions("homestead.cmd.plot.home")
    public void home(final CommandContext args, final CommandSender sender) throws BungeeCommandException {
        if (!(sender instanceof Player)) {
            throw new BungeeCommandException("Only players can use this command.");
        }
        final Player player = (Player) sender;
        final Player target;
        if (args.argsLength() == 0) {
            target = player;
        } else {
            target = Bukkit.getPlayer(args.getString(0));
        }
        if (target == null) {
            throw new BungeeCommandException("That player is not online.");
        }

        final Plot plot = this.getPlot(target);
        if (!(plot.hasPlayer(player) || plot.isOwner(player))) {
            MessageUtil.sendMessage("plots.commands.plot.home.notMember",
                    player,
                    new MessageData("plot", plot),
                    new MessageData("player", target));
            return;
        }

        if (getCooldown(player) > 0) {
            MessageUtil.sendMessage("plots.commands.plot.home.cooldown", player,
                    new MessageData("plot", plot),
                    new MessageData("player", target),
                    new MessageData("time", getCooldown(player)));
            return;
        }

        startWarmup(player, target, plot);
    }

    private        HashMap<UUID, Long>    cooldown = new HashMap<>();
    private static HashMap<UUID, Integer> warmup   = new HashMap<>();

    public long getCooldown(Player player) {
        if (!cooldown.containsKey(player.getUniqueId()) || player.hasPermission("homestead.cmd.home.cooldown.bypass"))
            return 0;

        long expiration  = cooldown.get(player.getUniqueId());
        long left        = expiration - System.currentTimeMillis();
        long leftSeconds = TimeUnit.MILLISECONDS.toSeconds(left);

        if (left > 0)
            return leftSeconds <= 0 ? 1 : leftSeconds;
        else {
            cooldown.remove(player.getUniqueId());
            return 0;
        }
    }

    private void startWarmup(Player player, Player target, Plot plot) {
        if (warmup.containsKey(player.getUniqueId())) { //Cancel any previous task before starting a new one
            Bukkit.getScheduler().cancelTask(warmup.get(player.getUniqueId()));
            warmup.remove(player.getUniqueId());
        }

        int warmupTime = Homestead.getInstance().getConfigHandler().getInt(ConfigHandler.HOME_WARMUP);

        if (warmupTime > 0 && !player.hasPermission("homestead.cmd.home.warmup.bypass")) {
            MessageUtil.sendMessage("plots.commands.plot.home.warmup", player,
                    new MessageData("player", player),
                    new MessageData("time", warmupTime));

            int task = Bukkit.getScheduler().scheduleSyncDelayedTask(Homestead.getInstance(), () -> {
                player.teleport(plot.getHome());
                warmup.remove(player.getUniqueId());

                if (!player.hasPermission("homestead.cmd.home.cooldown.bypass")) {
                    int  cooldownTime = Homestead.getInstance().getConfigHandler().getInt(ConfigHandler.HOME_COOLDOWN);
                    long expiry       = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(cooldownTime);
                    cooldown.put(player.getUniqueId(), expiry);
                }

                //noinspection ObjectEquality
                if (target == player) {
                    MessageUtil.sendMessage("plots.commands.plot.home.own",
                            player,
                            new MessageData("player", player),
                            new MessageData("plot", plot));
                } else {
                    MessageUtil.sendMessage("plots.commands.plot.home.other",
                            player,
                            new MessageData("player", player),
                            new MessageData("target", target),
                            new MessageData("plot", plot));
                }
            }, warmupTime * 20);
            warmup.put(player.getUniqueId(), task);
        } else {
            player.teleport(plot.getHome());
            //noinspection ObjectEquality
            if (target == player) {
                MessageUtil.sendMessage("plots.commands.plot.home.own",
                        player,
                        new MessageData("player", player),
                        new MessageData("plot", plot));
            } else {
                MessageUtil.sendMessage("plots.commands.plot.home.other",
                        player,
                        new MessageData("player", player),
                        new MessageData("target", target),
                        new MessageData("plot", plot));
            }
        }
    }

    public static void cancelWarmup(Player player) {
        if (warmup.containsKey(player.getUniqueId())) { //Cancel any previous task before starting a new one
            Bukkit.getScheduler().cancelTask(warmup.get(player.getUniqueId()));
            warmup.remove(player.getUniqueId());
            MessageUtil.sendMessage("plots.commands.plot.home.warmup-cancelled", player,
                    new MessageData("player", player));
        }
    }

    public static void removeWarmup(Player player) {
        if (warmup.containsKey(player.getUniqueId())) { //Cancel any previous task before starting a new one
            Bukkit.getScheduler().cancelTask(warmup.get(player.getUniqueId()));
            warmup.remove(player.getUniqueId());
        }
    }

    public static void addWarmup(Player player) {
        warmup.put(player.getUniqueId(), -1);
    }

    public static boolean isWarmup(Player player) {
        return warmup.containsKey(player.getUniqueId());
    }


    // added by GotoFinal stop.
    @Command(aliases = {"sell"}, desc = "This command sells the command sender's plot to a player.", usage = "<player> <price>", min = 1, max = 2)
    @CommandPermissions("homestead.cmd.plot.sell")
    public void sell(final CommandContext args, final CommandSender sender) throws BungeeCommandException {

        if (!(sender instanceof Player)) {
            throw new BungeeCommandException("Only players can use this command.");
        }
        final Player player     = (Player) sender;
        final String playerName = player.getName();
        final Plot   plot       = this.getPlot(player);

        // Lets check if the plot has no players added to it to prevent grief.
        if (!plot.getPlayers().isEmpty()) {
            throw new BungeeCommandException("Please remove all players to sell your plot.");
        }

        final String buyerName = args.getString(0);
        final Player buyer     = Bukkit.getPlayer(buyerName);

        // Lets make sure the sender isn't trying to sell to an offline player.
        if (buyer == null) {
            throw new BungeeCommandException("That player is not online.");
        }

        // Lets make sure the buyer doesn't already own a plot.
        final PlotManager mgr = this.getManager(player.getWorld());
        for (final Plot plots : mgr.getPlots().values()) {
            if (plots.isOwner(buyerName)) {
                throw new BungeeCommandException("That player already owns a plot.");
            }
        }

        if (!plot.getPlayers().isEmpty()) {
            throw new BungeeCommandException("Please remove all your friends before selling the plot.");
        }

        int sellDist = Homestead.getInstance().getConfigHandler().getInt(ConfigHandler.SELL_DISTANCE);
        if (player.getLocation().distance(buyer.getLocation()) > sellDist) {
            MessageUtil.sendMessage("plots.commands.plot.sell.distance", player,
                    new MessageData("sellDistance", sellDist),
                    new MessageData("buyer", buyer.getName()));
            return;
        }

        double              price = 0;
        final StringBuilder sb    = new StringBuilder();
        sb.append(ChatColor.AQUA).append(player.getName()).append(ChatColor.YELLOW);
        sb.append(" is selling their plot for ");
        if (args.argsLength() > 1) {
            price = args.getDouble(1);
            if (price < 0) {
                throw new BungeeCommandException("Price must be 0 or higher.");
            }
            if (!CodexEngine.get().getVault().canPay(buyer, price)) {
                throw new BungeeCommandException("That player can not afford your deal.");
            }
            sb.append((price > 0) ? CodexEngine.get().getVault().format(price) : "free");
        } else {
            sb.append("free");
        }

        final PlotPurchase plotPurchase = new PlotPurchase(plot, buyerName, price);
        this.plotsForSale.put(player.getName().toLowerCase(), plotPurchase);
        buyer.sendMessage(sb.toString());
        player.sendMessage(ChatColor.YELLOW + "Started a deal with " + buyerName + ".");
        new BukkitRunnable() {
            @Override
            public void run() {
                final PlotPurchase purchase = PlotCommands.this.plotsForSale.remove(player.getName().toLowerCase());
                if (purchase != null) {
                    if (player.isOnline()) {
                        player.sendMessage(ChatColor.RED + "Deal with " + buyerName + " expired.");
                    }
                    if (buyer.isOnline()) {
                        buyer.sendMessage(ChatColor.RED + "Deal with " + playerName + " expired.");
                    }
                }
            }
        }.runTaskLater(this.plugin, 30 * 20);
    }

    @Command(aliases = {"buy"}, desc = "This command purchases a player's plot.", usage = "<player>", min = 1, max = 1)
    @CommandPermissions("homestead.cmd.plot.buy")
    public void buy(final CommandContext args, final CommandSender sender) throws BungeeCommandException {

        if (!(sender instanceof Player)) {
            throw new BungeeCommandException("Only players can use this command.");
        }
        final Player player       = (Player) sender;
        PlotPurchase plotPurchase = this.plotsForSale.get(args.getString(0).toLowerCase());
        if ((plotPurchase == null) || !plotPurchase.getBuyer().equalsIgnoreCase(player.getName())) {
            throw new BungeeCommandException("That player has not made a deal with you.");
        }
        plotPurchase = this.plotsForSale.remove(args.getString(0).toLowerCase());

        final PlotManager mgr = this.getManager(player.getWorld());
        for (final Plot plots : mgr.getPlots().values()) {
            if (plots.isOwner(player.getName())) {
                throw new BungeeCommandException("That player already owns a plot.");
            }
        }

        final Plot   plot      = plotPurchase.getPlot();
        final String plotOwner = plot.getOwner();
        final double price     = plotPurchase.getPrice();
        if (!CodexEngine.get().getVault().canPay(player, price)) {
            throw new BungeeCommandException("You can not afford to purchase this plot.");
        }

        CodexEngine.get().getVault().take(player, price);
        player.sendMessage(Util.getDeductedMessage(price));

        CodexEngine.get().getVault().give(Bukkit.getOfflinePlayer(plotOwner), price);
        final Player plotOwnerPlayer = Bukkit.getPlayer(plotOwner);
        if (plotOwnerPlayer != null) {
            plotOwnerPlayer.sendMessage(Util.getAddedMessage(price));
        }
        final ProtectedRegion region = plot.getProtectedRegion();
        plot.setOwner(region, player.getName());
        player.sendMessage(ChatColor.YELLOW + "You purchased " + plotOwner + "'s plot.");
        if (plotOwnerPlayer != null) {
            plotOwnerPlayer.sendMessage(ChatColor.YELLOW + player.getName() + " purchased your plot.");
        }
    }

    @Command(aliases = {"list"}, desc = "Lists all the plots.", usage = "[world]", help =
            "Lists all the plots in the command sender's world.\n "
                    + "The [world] argument determines which world to list.\n "
                    + "The -p flag determines which page to view.", min = 0, max = 1, flags = "p:")
    @CommandPermissions("homestead.cmd.plot.list")
    public void list(final CommandContext args, final CommandSender sender) throws BungeeCommandException {

        final World world;

        // Check which world to use from arguments
        if (args.argsLength() == 0) {
            // If the sender is a player, just use their world.
            if (sender instanceof Player) {
                world = ((Player) sender).getWorld();
            } else {
                // Console tried to execute this command without providing a world
                throw new BungeeCommandException(
                        "Only players can use this command. Please read the commands documentation.");
            }
        } else {
            // Console provided a world
            world = Bukkit.getWorld(args.getString(0));
        }

        if (world == null) {
            throw new BungeeCommandException("That world is not loaded.");
        }

        final PlotManager mgr = this.getManager(world);

        int page = 1;
        if (args.hasFlag('p')) {
            page = args.getFlagInteger('p');
        }

        new SimplePaginatedResult<Plot>("Plots in '" + world.getName() + "'") {

            @Override
            public String format(final Plot entry, final int index) {
                String         result =
                        ChatColor.YELLOW.toString() + (index + 1) + ". " + ChatColor.BOLD + entry.getName();
                final Location loc    = entry.getSignLocation();
                if (loc != null) {
                    result =
                            result + ChatColor.AQUA + " - x: " + loc.getBlockX() + ", " + "y: " + loc.getBlockY() + ", "
                                    + "z: " + loc.getBlockZ();
                }
                return result;
            }
        }.display(sender, mgr.getPlots().values(), page);
    }

    @Command(aliases = {"admin"}, desc = "Plot administrator commands.")
    @CommandPermissions("homestead.cmd.plot.admin")
    @NestedCommand(PlotAdminCommands.class)
    public void admin(final CommandContext args, final CommandSender sender) {
    }

    private PlotManager getManager(final CommandContext args, final CommandSender sender, final char flag) throws
            BungeeCommandException {

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

    private Plot getPlot(final Player player) throws BungeeCommandException {

        final PlotManager mgr = this.plugin.getGlobalPlotsManager().getPlotManager(player.getWorld());
        if (mgr == null) {
            throw new BungeeCommandException("HouseDeeds is not enabled in your world.");
        }
        for (final Plot plot : mgr.getPlots().values()) {
            if (plot.isOwner(player)) {
                return plot;
            }
        }
        throw new BungeeCommandException("You do not own a plot.");
    }

    private Plot getPlot(final PlotManager mgr, final String id) throws BungeeCommandException {

        final Plot plot = mgr.getPlot(id);
        if (plot == null) {
            throw new BungeeCommandException("No plot found by that id.");
        }
        return plot;
    }

    /**
     * Gets the player addition limit for a player.
     *
     * @param player player to get
     * @return the limit of players the {@code player} can add to their plot, otherwise -1 for unlimited.
     */
    private int getAddLimit(final Player player) {
        if (player.hasPermission("homestead.cmd.plot." + ADD_LIMIT + ".-1")) {
            return -1;
        }
        for (int i = 100; i >= 0; i--) {
            if (player.hasPermission(ADD_LIMIT + "." + i)) {
                return i;
            }
        }
        return -1;
    }

    public HashMap<String, PlotPurchase> getPlotsForSale() {
        return this.plotsForSale;
    }

    /**
     * Gets a PlotPurchase created by a player.
     *
     * @param player player that created the PlotPurchase instance
     * @return the PlotPurchase instance
     */
    public PlotPurchase getPlotPurchase(final Player player) {
        return this.getPlotPurchase(player.getName());
    }

    /**
     * Gets a PlotPurchase created by a player.
     *
     * @param player player name that created the PlotPurchase instance
     * @return the PlotPurchase instance
     */
    public PlotPurchase getPlotPurchase(final String player) {
        return this.plotsForSale.get(player.toLowerCase());
    }

    /**
     * Adds a PlotPurchase with its creator ({@link org.bukkit.entity.Player}.
     *
     * @param player player that created the PlotPurchase instance
     */
    public void addPlotPurchase(final Player player, final PlotPurchase plotPurchase) {
        this.plotsForSale.put(player.getName().toLowerCase(), plotPurchase);
    }

    /**
     * Adds a PlotPurchase with its creator.
     *
     * @param player player that created the PlotPurchase instance
     */
    public void addPlotPurchase(final String player, final PlotPurchase plotPurchase) {
        this.plotsForSale.put(player.toLowerCase(), plotPurchase);
    }

    /**
     * Removes a PlotPurchase by its creator ({@link org.bukkit.entity.Player}.
     *
     * @param player player that created the PlotPurchase instance
     * @return the removed PlotPurchase instance
     */
    public PlotPurchase removePlotPurchase(final Player player) {
        return this.plotsForSale.remove(player.getName().toLowerCase());
    }

    /**
     * Removes a PlotPurchase by its creator.
     *
     * @param player player that created the PlotPurchase instance
     * @return the removed PlotPurchase instance
     */
    public PlotPurchase removePlotPurchase(final String player) {
        return this.plotsForSale.remove(player.toLowerCase());
    }

    public static class PlotPurchase {
        private final Plot   plot;
        private final String buyer;
        private final double price;

        public PlotPurchase(final Plot plot, final String buyer, final double price) {
            this.plot = plot;
            this.buyer = buyer;
            this.price = price;
        }

        public Plot getPlot() {
            return this.plot;
        }

        public String getBuyer() {
            return this.buyer;
        }

        public double getPrice() {
            return this.price;
        }

        @Override
        public String toString() {
            return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE).appendSuper(super.toString())
                    .append("plot", this.plot)
                    .append("buyer", this.buyer)
                    .append("price", this.price)
                    .toString();
        }
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE).appendSuper(super.toString())
                .append("plotsForSale", this.plotsForSale)
                .toString();
    }
}
