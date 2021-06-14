package com.gotofinal.darkrise.plots.deeds;

import com.gotofinal.darkrise.plots.DarkRisePlots;
import com.gotofinal.darkrise.plots.commands.PlotCommands;
import com.gotofinal.darkrise.plots.config.ConfigHandler;
import com.gotofinal.darkrise.plots.events.ConfigReloadEvent;
import com.gotofinal.darkrise.plots.events.PlotUpdateEvent;
import com.gotofinal.darkrise.plots.util.Util;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import me.travja.darkrise.core.bungee.BungeeUtil;
import me.travja.darkrise.core.legacy.util.Vault;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * A Listener class that listeners for Plot related events.
 */
public class PlotsListener implements Listener {

    private final Map<Player, Plot> confirmingPlayers = new HashMap<>(7);
    private final DarkRisePlots plugin;
    /**
     * The cooldown of interaction, the reason I created a variable for this and not directly retrieve from the
     * configuration is because I use it whenever adding players and thought it would be better than getting the
     * config value every time.
     */
    private int cooldown;
    /**
     * This list caches player names that are on cooldown. The reason for using String instead of Player object,
     * is so I can keep the player in the list even if they leave the game without doing a slight impact on the
     * server performance, this mainly depends on the user's configuration, but I do not rely on the configuration
     * for performance, I shall do my best to give them their freedom.
     */
    private final List<String> cooldownPlayers = new ArrayList<>(7);

    public PlotsListener(final DarkRisePlots instance) {
        this.plugin = instance;
        this.cooldown = this.plugin.getConfigHandler().getInt(ConfigHandler.PLOT_SIGN_INTERACTION_COOLDOWN);
        // Just a simple way for easy if statements, instead of checking the value is larger than 200,
        // just check if it's larger than 0. There's no interfering there, unless this code is somehow executed back
        // in the 70s!! :O
        this.cooldown = (this.cooldown > 200) ? (this.cooldown / 50) : 0;
    }

    @EventHandler
    public void update(PlotUpdateEvent event) {
        Plot plot = event.getPlot();
        ArrayList<String> data = plot.dataToArray();
        BungeeUtil.sendMessage(data.toArray(new String[0]));
    }

    @EventHandler
    public void onConfigReload(final ConfigReloadEvent event) {

        this.cooldown = event.getConfig().getInt(ConfigHandler.PLOT_SIGN_INTERACTION_COOLDOWN);
        this.cooldown = (this.cooldown > 200) ? (this.cooldown / 50) : 0;
        SignUpdater.setInterval(event.getConfig().getInt(ConfigHandler.PLOT_SIGN_UPDATE_INTERVAL));
    }

    @EventHandler
    public void onSignClick(final PlayerInteractEvent event) {
        if (event.getHand() == EquipmentSlot.OFF_HAND) {
            return;
        }
        if ((event.getAction() != Action.LEFT_CLICK_BLOCK) && (event.getAction() != Action.RIGHT_CLICK_BLOCK)) {
            return;
        }

        final Block block = event.getClickedBlock();
        if (!block.getType().toString().contains("SIGN"))
            return;

        final Player player = event.getPlayer();

        if (this.isOnCooldown(player)) {
            return;
        }

        final PlotManager mgr = this.plugin.getGlobalPlotsManager().getPlotManager(player.getWorld());
        if (mgr == null) {
            return;
        }
        final Sign sign = (Sign) block.getState();

        final Plot plot = this.getPlotFromSign(mgr, player, sign);
        if (plot == null) {
            return;
        }

        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            final ItemStack item = event.getItem();

            // If the player is already confirming a purchase, then no need to go on
            if (this.isConfirming(player)) {
                this.purchase(player, mgr, item);
                return;
            }

            if (plot.isOwned()) { // Already owned/ Extending rent time
                if (!plot.getOwner().equalsIgnoreCase(player.getName())) {
                    player.sendMessage(ChatColor.RED + "This plot is owned by " + ChatColor.YELLOW + plot.getOwner());
                    this.addCooldownPlayer(player);
                } else { // The owner of the plot
                    if ((item != null) && item.isSimilar(plot.getDeed().toItemStack())) {
                        player.sendMessage(ChatColor.RED + "You already own this plot.");
                        this.addCooldownPlayer(player);
                    } else {
                        final long expiry = TimeUnit.MILLISECONDS.toHours(plot.getExpiry() - System.currentTimeMillis());
                        if (expiry >= plot.getDeed().getMaximumExtensionTime()) {
                            player.sendMessage(ChatColor.RED + "You can not extend this plot's rent any further.");
                            this.addCooldownPlayer(player);
                        } else {
                            if (!this.canAffordPlot(player, plot)) {
                                this.addCooldownPlayer(player);
                                return;
                            }

                            final double tax = plot.getDeed().getTax();
                            Vault.pay(player, tax);
                            player.sendMessage(Util.getDeductedMessage(tax));

                            plot.extendExpiry();
                            player.sendMessage(ChatColor.YELLOW + "You've successfully extended your rent.");
                        }
                    }
                }
            } else { // Purchasing

                final Plot ownedPlot = this.getPlot(player);
                if (ownedPlot != null) {
                    player.sendMessage(ChatColor.RED + "You already own a plot called '" + ownedPlot.getName() + "'.");
                    return;
                }

                final ItemStack deedItem = plot.getDeed().toItemStack();
                if ((item != null) && item.isSimilar(deedItem)) {
                    if (!this.canAffordPlot(player, plot)) {
                        this.addCooldownPlayer(player);
                        return;
                    }

                    this.addConfirmingPlot(player, plot);
                } else {
                    player.sendMessage(ChatColor.RED + "You need a Deed to purchase this plot.");
                    this.addCooldownPlayer(player);
                }
            }
        }

        if (sign != null) {
            plot.updateSign(sign);
        }
    }

    @EventHandler
    public void onSignBreak(final BlockBreakEvent event) {

        final Player player = event.getPlayer();
        final Block block = event.getBlock();
        if (!event.getBlock().getType().toString().contains("SIGN") || player.hasPermission("pmch.deeds.build")) return;

        final PlotManager mgr = this.plugin.getGlobalPlotsManager().getPlotManager(player.getWorld());
        if (mgr == null) {
            return;
        }
        final Sign sign = (Sign) block.getState();

        final Plot plot = this.getPlotFromSign(mgr, player, sign);
        if (plot == null) {
            return;
        }
        event.setCancelled(true);
        event.getPlayer().sendMessage(ChatColor.RED + "You do not have permission to destroy this sign.");
    }

    @EventHandler
    public void onBlockPhysics(final BlockPhysicsEvent event) {

        // Disallow sign blocks belonging to a plot from breaking
        final Block block = event.getBlock();
        if (!event.getBlock().getType().toString().contains("SIGN")) return;

        final PlotManager mgr = this.plugin.getGlobalPlotsManager().getPlotManager(block.getWorld());
        final Sign sign = (Sign) block.getState();
        if (mgr == null) {
            return;
        }
        final Plot plot = mgr.getPlot(sign.getLine(0));
        if (plot != null) {
            event.setCancelled(true);
            return;
        }
        mgr.getPlots().values().stream().filter(plots -> plots.getSignLocation().equals(sign.getLocation())).findFirst().ifPresent(p -> event.setCancelled(true));
    }

//    @EventHandler
//    public void onBlockPlace(final BlockPlaceEvent event) {

//        final Player player = event.getPlayer();
//        final Block block = event.getBlock();
//        final World world = block.getWorld();
//        final PlotManager mgr = this.plugin.getGlobalPlotsManager().getPlotManager(world);
//
//        if (mgr == null) {
//            return;
//        }
//
//        final RegionManager regionManager = mgr.getRegionManager();
//
//        if (regionManager == null) {
//            return;
//        }

    // TODO Implement a system that stores the player's last plot interaction instead of having to loop
    // through all regions and so on.

//        final ApplicableRegionSet regions = regionManager.getApplicableRegions(BukkitAdapter.asBlockVector(block.getLocation()));
//
//        for (final ProtectedRegion region : regions) {
//            final Plot plot = mgr.getPlot(region.getId());
//            if (plot == null) {
//                return;
//            }
//
////            final Map<MaterialData, Integer> limitedBlocks = plot.getDeed().getLimitedBlocks();
//
////            if (limitedBlocks.isEmpty()) {
////                return;
////            }
////
////            for (final Map.Entry<MaterialData, Integer> entry : limitedBlocks.entrySet()) {
////                final MaterialData data = entry.getKey();
////                final Integer amount = entry.getValue();
////                if (this.countMaterial(region, world, data) > amount) {
////                    event.setCancelled(true);
////                    player.sendMessage(ChatColor.RED + "You can not place anymore " + ChatColor.GRAY + MaterialUtil.friendlyName(data.getItemType()) + "s" + ChatColor.RED + " in this plot.");
////                    return;
////                }
////            }
//        }
//    }

    @EventHandler
    public void onPlayerQuit(final PlayerQuitEvent event) {
        this.removeConfirmingPlot(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChunkLoad(final ChunkLoadEvent event) {
        final Chunk chunk = event.getChunk();
        final long key = (((long) chunk.getX()) << 32) | (chunk.getZ() & 0xffffffffL);
        final Set<Plot> unupdatedPlots = SignUpdater.getUnupdated().get(key);
        if ((unupdatedPlots == null) || unupdatedPlots.isEmpty()) {
            return;
        }
        Bukkit.getScheduler().runTaskLater(this.plugin, () ->
        {
            if (!chunk.isLoaded()) {
                return;
            }
            for (final Plot plot : unupdatedPlots) {
                if ((plot.getExpiry() > -1) && (System.currentTimeMillis() >= plot.getFinalExpiry())) {
                    plot.reset(plot.getProtectedRegion());
                }
                final Location location = plot.getSignLocation();
                if (location == null) {
                    continue;
                }

                final BlockState state = location.getBlock().getState();
                if (state instanceof Sign) {
                    final Sign sign = (Sign) state;
                    plot.updateSign(sign);
                } else {
                    this.plugin.getLogger().severe("[HouseDeeds] plot id '" + plot.getName() + "' sign location is not a " + "sign!");
                }
            }
            SignUpdater.getUnupdated().remove(key);
        }, 20);
    }

    @EventHandler
    public void cancelWarmup(PlayerMoveEvent event) {
        if (!PlotCommands.isWarmup(event.getPlayer()))
            return;

        Block from = event.getFrom().getBlock();
        Block to = event.getTo().getBlock();
        if (!from.equals(to)) {
            if (DarkRisePlots.getInstance().getConfigHandler().IS_BUNGEE) {
                BungeeUtil.sendMessage(BungeeUtil.CHANNEL, event.getPlayer(), "CANCEL_HOME", event.getPlayer().getName());
                PlotCommands.removeWarmup(event.getPlayer());
            } else
                PlotCommands.cancelWarmup(event.getPlayer());
        }
    }

    public Plot getPlotFromSign(final PlotManager manager, final Player player, final Sign sign) {

        // Lets check the clicked sign for the plot name first instead of loop through all deeds
        Plot plot = manager.getPlot(sign.getLine(0));

        if ((plot == null) || !sign.getLocation().equals(plot.getSignLocation())) {
            // Lets check if the player is confirming and get that deed
            if ((player != null) && this.isConfirming(player)) {
                plot = this.getConfirmingPlot(player);
            }
            if ((plot == null) || !sign.getLocation().equals(plot.getSignLocation())) {
                for (final Plot plots : manager.getPlots().values()) {
                    final Location signLocation = plots.getSignLocation();
                    if ((signLocation != null) && signLocation.equals(sign.getLocation())) {
                        plot = plots;
                        return plot;
                    }
                }
            }
        }
        return plot;
    }

    public Plot getPlot(final Player player) {

        // Lets check the world the player is in, before looping through all the plot managers.
        final PlotManager mgr = this.plugin.getGlobalPlotsManager().getPlotManager(player.getWorld());

        if (mgr != null) {
            for (final Plot plot : mgr.getPlots().values()) {
                if (plot.isOwner(player)) {
                    return plot;
                }
            }
        }

        for (final PlotManager manager : this.plugin.getGlobalPlotsManager().getManagers().values()) {
            for (final Plot plot : manager.getPlots().values()) {
                if (plot.isOwner(player)) {
                    return plot;
                }
            }
        }
        return null;
    }

    /**
     * Purchases a plot for a player.
     *
     * @param player player purchasing
     */
    private void purchase(final Player player, final PlotManager manager, final ItemStack item) {

        final Plot plot = this.removeConfirmingPlot(player);
        if ((plot == null) || (item == null)) {
            return;
        }

        if (plot.isOwned()) { // Already owned
            player.sendMessage(ChatColor.RED + "This plot is owned by " + ChatColor.YELLOW + plot.getOwner());
            return;
        }
        final double tax = plot.getDeed().getTax();
        if (!this.canAffordPlot(player, plot)) {
            return;
        }
        Vault.pay(player, tax);
        player.sendMessage(Util.getDeductedMessage(tax));
        if (item.getAmount() <= 1) {
            player.getInventory().remove(item);
        } else {
            item.setAmount(item.getAmount() - 1);
        }
        player.sendMessage(ChatColor.YELLOW + "You have purchased plot '" + plot.getName() + "'. Enjoy!");
        final ProtectedRegion region = plot.getProtectedRegion();
        plot.setOwner(region, player.getName());
        plot.setExpiry(System.currentTimeMillis() + TimeUnit.HOURS.toMillis(plot.getDeed().getInitialExtensionTime()));
        if (plot.getSignLocation() == null) {
            return;
        }
        final BlockState state = plot.getSignLocation().getBlock().getState();
        if (!(state instanceof Sign)) {
            return;
        }
        plot.updateSign((Sign) state);
    }

    public boolean canAffordPlot(final Player player, final Plot plot) {
        final double tax = plot.getDeed().getTax();
        if (!Vault.canPay(player, tax)) {
            if (player.getName().equalsIgnoreCase(plot.getOwner())) {
                player.sendMessage(ChatColor.RED + "You can not afford rent for this plot.");
            } else {
                player.sendMessage(ChatColor.RED + "You can not afford this plot.");
            }
            return false;
        }
        return true;
    }

    /**
     * Counts a {@link org.bukkit.material.MaterialData} in {@link com.sk89q.worldguard.protection.regions
     * .ProtectedRegion}.
     *
     * @param region region to count in
     * @param world  world to check in
     * @param data   the MaterialData to check
     * @return the amount of {@code data}
     */
    @SuppressWarnings("deprecation")
    public int countMaterial(final ProtectedRegion region, final World world, final MaterialData data) {

        int counter = 0;
        final BlockVector3 min = region.getMinimumPoint();
        final BlockVector3 max = region.getMaximumPoint();

        for (int x = min.getBlockX(); x <= max.getBlockX(); x++) {
            for (int y = min.getBlockY(); y <= max.getBlockY(); y++) {
                for (int z = min.getBlockZ(); z <= max.getBlockZ(); z++) {
                    final Block block = world.getBlockAt(x, y, z);
                    if (block.getType().equals(data.getItemType()) && ((data.getData() == -1) || (block.getData() == data.getData()))) {
                        counter++;
                    }
                }
            }
        }
        return counter;
    }

    /**
     * Checks if a player is in the list of players on cooldown, meaning players that are not allowed to interact
     * with a
     * deed sign for a specific time.
     *
     * @param player player to check
     * @return true if the {@code player} is in the List, otherwise false
     */
    private boolean isOnCooldown(final Player player) {
        return this.cooldownPlayers.contains(player.getName());
    }

    /**
     * Adds a player to the players on cooldown List and removes them after a certain amount of ticks.
     *
     * @param player player to add
     */
    private void addCooldownPlayer(final Player player) {

        if (this.cooldown <= 0) {
            return;
        }
        this.cooldownPlayers.add(player.getName());
        new BukkitRunnable() {

            @Override
            public void run() {
                PlotsListener.this.cooldownPlayers.remove(player.getName());
            }
        }.runTaskLater(this.plugin, this.cooldown);
    }

    /**
     * Checks if a player is confirming an action.
     *
     * @param player player to check
     * @return true if the player is confirming
     */
    private boolean isConfirming(final Player player) {
        return this.confirmingPlayers.containsKey(player);
    }

    /**
     * Gets a {@link Plot} that a player is confirming an action for.
     *
     * @param player player to get
     * @return the Plot instance, will return null if the player is not confirming any actions
     */
    private Plot getConfirmingPlot(final Player player) {
        return this.confirmingPlayers.get(player);
    }

    /**
     * Adds a Player as confirming a {@link Plot}.
     *
     * @param player player to add
     * @param plot   plot to assign to the {@code player}
     */
    private void addConfirmingPlot(final Player player, final Plot plot) {

        player.sendMessage(ChatColor.YELLOW + "Please right click again to confirm.");
        this.confirmingPlayers.put(player, plot);
    }

    /**
     * Removes a Player from confirming a {@link Plot}.
     *
     * @param player player to remove
     * @return the Plot the {@code player} was confirming, otherwise null if there is nothing the {@code player} is
     * confirming.
     */
    private Plot removeConfirmingPlot(final Player player) {
        return this.confirmingPlayers.remove(player);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE).appendSuper(super.toString()).append("confirmingPlayers", this.confirmingPlayers).append("cooldown", this.cooldown).append("cooldownPlayers", this.cooldownPlayers).toString();
    }
}
