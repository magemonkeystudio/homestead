package studio.magemonkey.homestead.bungee;

import com.sk89q.minecraft.util.commands.CommandPermissions;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import studio.magemonkey.homestead.bungee.util.CordUtil;
import studio.magemonkey.homestead.bungee.util.DataUtil;
import studio.magemonkey.homestead.commands.PlotCommands;
import studio.magemonkey.homestead.util.StringUtil;
import studio.magemonkey.homestead.util.bungee.BungeeCommandException;
import studio.magemonkey.homestead.util.bungee.BungeeSimplePaginatedResult;

import java.util.Arrays;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class BungeePlotCommands extends Command {

    private static final String                                     ADD_LIMIT    = "add.limit";
    private final        HashMap<String, PlotCommands.PlotPurchase> plotsForSale = new HashMap<>(50);
    private              String                                     usage;
    private              Bridge                                     plugin;

    public BungeePlotCommands(String name, String permission, String usage, String... aliases) {
        super(name, permission, aliases);
        this.usage = usage;
        this.plugin = Bridge.getInstance();
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            CordUtil.sendMessage(sender, this.usage);
            return;
        }
        String   subCommand = args[0];
        String[] left       = new String[args.length - 1];
        for (int i = 0; i < args.length - 1; i++) {
            left[i] = args[i + 1];
        }
        try {
            if (subCommand.equalsIgnoreCase("add"))
                addPlayer(sender, left);
            else if (subCommand.equalsIgnoreCase("remove"))
                removePlayer(sender, left);
            else if (subCommand.equalsIgnoreCase("players"))
                players(sender, left);
            else if (subCommand.equalsIgnoreCase("sell"))
                sell(sender, left);
            else if (subCommand.equalsIgnoreCase("home"))
                home(sender, left);
            else if (subCommand.equalsIgnoreCase("cancelhome"))
                cancelWarmup((ProxiedPlayer) sender);
            else if (subCommand.equalsIgnoreCase("list"))
                list(sender, left);
            else {//If it's not something that should execute on the bungee itself, send it to the server
                String command = this.getName() + " ";
                command += StringUtil.join(Arrays.asList(args), " ");
                System.out.println("Sending command to server to handle. " + command);
                CordUtil.sendBungeeMessage((ProxiedPlayer) sender, "PlotRunCommand", sender.getName(), command);
            }
        } catch (BungeeCommandException e) {
            CordUtil.sendMessage(sender, ChatColor.RED + e.getMessage());
        }
    }

    public void addPlayer(CommandSender sender, String[] args) throws BungeeCommandException {
        ProxiedPlayer   player = (ProxiedPlayer) sender;
        final ProxyPlot plot   = this.getPlot(player);
        final int       limit  = plot.getFriends();

        if (plot.getPlayers().size() >= limit)
            throw new BungeeCommandException("You can not add anymore players to your plot.");

        final String playerToAdd = args[0];

        if (plot.isOwner(playerToAdd))
            throw new BungeeCommandException("You can not add the owner to the plot.");

        if (plot.hasPlayer(playerToAdd))
            throw new BungeeCommandException("That player is already added to your plot.");

        CordUtil.sendBungeeMessage(player, "PlotsAddToRegion", plot.getName(), playerToAdd);
        DataUtil.addPlayer(player.getServer().getInfo().getName(), plot.getName(), playerToAdd);
        CordUtil.sendMessage(player,
                ChatColor.YELLOW + "You successfully added " + ChatColor.AQUA + playerToAdd + ChatColor.YELLOW
                        + " to your plot.");
        final ProxiedPlayer targetPlayer = Bridge.getInstance().getProxy().getPlayer(playerToAdd);

        if (targetPlayer != null && targetPlayer.isConnected()) {
            CordUtil.sendMessage(targetPlayer, ChatColor.YELLOW + player.getName() + " added you to his plot.");
        }
    }

    @CommandPermissions("homestead.cmd.plot.remove")
    public void removePlayer(CommandSender sender, String[] args) throws BungeeCommandException {
        ProxiedPlayer   player         = (ProxiedPlayer) sender;
        final ProxyPlot plot           = this.getPlot(player);
        final String    playerToRemove = args[0];
        if (!plot.hasPlayer(playerToRemove)) {
            throw new BungeeCommandException("That player is not added to your plot.");
        }

        CordUtil.sendBungeeMessage(player, "PlotsRemoveFromRegion", plot.getName(), playerToRemove);
        DataUtil.removePlayer(player.getServer().getInfo().getName(), plot.getName(), playerToRemove);
        CordUtil.sendMessage(player,
                ChatColor.YELLOW + "You successfully removed " + ChatColor.AQUA + playerToRemove + ChatColor.YELLOW
                        + " from your plot.");
        final ProxiedPlayer targetPlayer = Bridge.getInstance().getProxy().getPlayer(playerToRemove);

        if (targetPlayer != null && targetPlayer.isConnected()) {
            CordUtil.sendMessage(targetPlayer, ChatColor.YELLOW + player.getName() + " removed you from his plot.");
        }
    }

    @CommandPermissions("homestead.cmd.plot.players")
    public void players(CommandSender sender, String[] args) throws BungeeCommandException {
        ProxiedPlayer player = (ProxiedPlayer) sender;

        ProxyPlot plot = this.getPlot(player);
        if (plot == null)
            throw new BungeeCommandException("Looks like you don't belong to a plot.");

        CordUtil.sendMessage(sender, "$plots.commands.plot.players.list", "plot", "plot:" + plot.getName());
    }

    @CommandPermissions("homestead.cmd.plot.home")
    public void home(CommandSender sender, String[] args) throws BungeeCommandException {
        ProxiedPlayer player = (ProxiedPlayer) sender;
        ProxiedPlayer target;
        if (args.length == 0)
            target = player;
        else
            target = Bridge.getInstance().getProxy().getPlayer(args[0]);

        if (target == null || !target.isConnected())
            throw new BungeeCommandException("That player is not online.");


        final ProxyPlot plot = this.getPlot(target);
        if (!(plot.hasPlayer(player.getName()) || plot.isOwner(player.getName()))) {
            CordUtil.sendMessage(player,
                    "$plots.commands.plot.home.notMember",
                    "plot",
                    "plot:" + plot.getName(),
                    "player",
                    "p:" + target.getName());
            return;
        }

        if (getCooldown(player) > 0) {
            CordUtil.sendMessage(player, "$plots.commands.plot.home.cooldown",
                    "plot", "plot:" + plot.getName(), "player", "p:" + player.getName(),
                    "target", "p:" + target.getName(), "time", String.valueOf(getCooldown(player)));
            return;
        }

        startWarmup(player, target, plot);
    }

    private        HashMap<UUID, Long>    cooldown = new HashMap<>();
    private static HashMap<UUID, Integer> warmup   = new HashMap<>();

    public long getCooldown(ProxiedPlayer player) {
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

    private void teleport(ProxiedPlayer player, ProxiedPlayer target, ProxyPlot plot) {
        String     serverStr  = DataUtil.getHomeServer(target.getName());
        ServerInfo destServer = Bridge.getInstance().getProxy().getServerInfo(serverStr);
        if (!serverStr.equals(player.getServer().getInfo().getName())) {
            if (serverStr.equals("notFound") || destServer == null) {
                CordUtil.sendMessage(player, "Home was not found.");
                return;
            }

            player.connect(destServer);
        }

        CordUtil.sendBungeeMessage(destServer, "TPPlotHome", player.getName(), plot.getName());
    }

    public void startWarmup(ProxiedPlayer player, ProxiedPlayer target, ProxyPlot plot) {
        if (warmup.containsKey(player.getUniqueId())) { //Cancel any previous task before starting a new one
            ProxyServer.getInstance().getScheduler().cancel(warmup.get(player.getUniqueId()));
            warmup.remove(player.getUniqueId());
        }

        int warmupTime = Bridge.getConfig().getInt("home-warmup");

        if (warmupTime > 0 && !player.hasPermission("homestead.cmd.home.warmup.bypass")) {
            CordUtil.sendMessage(player,
                    "$plots.commands.plot.home.warmup",
                    "plot",
                    "plot:" + plot.getName(),
                    "player",
                    "p:" + player.getName(),
                    "target",
                    "p:" + target.getName(),
                    "time",
                    String.valueOf(warmupTime));

            CordUtil.sendBungeeMessage(player, "PlotsWarmingUp");
            int task = ProxyServer.getInstance().getScheduler().schedule(Bridge.getInstance(), () -> {
                teleport(player, target, plot);
                warmup.remove(player.getUniqueId());

                if (!player.hasPermission("homestead.cmd.home.cooldown.bypass")) {
                    int  cooldownTime = Bridge.getConfig().getInt("home-cooldown");
                    long expiry       = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(cooldownTime);
                    cooldown.put(player.getUniqueId(), expiry);
                }
            }, warmupTime, TimeUnit.SECONDS).getId();
            warmup.put(player.getUniqueId(), task);
        } else {
            teleport(player, target, plot);
        }
    }

    public static void cancelWarmup(ProxiedPlayer player) {
        if (warmup.containsKey(player.getUniqueId())) { //Cancel any previous task before starting a new one
            ProxyServer.getInstance().getScheduler().cancel(warmup.get(player.getUniqueId()));
            warmup.remove(player.getUniqueId());
            CordUtil.sendMessage(player, "$plots.commands.plot.home.warmup-cancelled",
                    "player", "p:" + player.getName());
        }
    }

    @CommandPermissions("homestead.cmd.plot.sell")
    public void sell(CommandSender sender, String[] args) throws BungeeCommandException {
        if (args.length == 0) {
            throw new BungeeCommandException("/" + getName() + " sell <player> [price]");
        }
        ProxiedPlayer   player     = (ProxiedPlayer) sender;
        final String    playerName = player.getName();
        final ProxyPlot plot       = this.getPlot(player);

        // Lets check if the plot has no players added to it to prevent grief.
        if (!plot.getPlayers().isEmpty() && plot.getPlayers().size() > 1) {
            throw new BungeeCommandException("Please remove all players to sell your plot.");
        }

        final String  buyerName = args[0];
        ProxiedPlayer buyer     = Bridge.getInstance().getProxy().getPlayer(buyerName);

        // Lets make sure the sender isn't trying to sell to an offline player.
        if (buyer == null || !buyer.isConnected()) {
            throw new BungeeCommandException("That player is not online.");
        }

        // Lets make sure the buyer doesn't already own a plot.
        if (getPlot(buyer) != null && getPlot(buyer).isOwner(buyer.getName()))
            throw new BungeeCommandException("That player already owns a plot.");

        if (!plot.getPlayers().isEmpty() || plot.getPlayers().size() > 1) {
            throw new BungeeCommandException("Please remove all your friends before selling the plot.");
        }

        if (!player.getServer().getInfo().getName().equalsIgnoreCase(buyer.getServer().getInfo().getName())) {
            CordUtil.sendMessage(player, "$plots.commands.plot.sell.distance", "buyer", buyer.getName());
        } else {
            String command = this.getName() + " sell ";
            command += StringUtil.join(Arrays.asList(args), " ");
            System.out.println("Sending command to server to handle. " + command);
            CordUtil.sendBungeeMessage((ProxiedPlayer) sender, "PlotRunCommand", sender.getName(), command);
        }
    }

    @CommandPermissions("homestead.cmd.plot.list")
    public void list(CommandSender sender, String[] args) throws BungeeCommandException {

        int    page   = 1;
        String server = "GLOBAL";
        // Check which server to use from arguments
        if (args.length > 0) {
            int next = 0;
            server = args[next++];
            if (Bridge.getInstance().getProxy().getServerInfo(server) == null) {
                server = "GLOBAL";
                next--;
            }
            if (args.length > next) {
                try {
                    page = Integer.parseInt(args[next]);
                } catch (NumberFormatException e) {
                    throw new BungeeCommandException("Could not parse " + args[next] + " to a page number.");
                }
            }
        }

        new BungeeSimplePaginatedResult<ProxyPlot>("Plots in '" + server + "'") {
            @Override
            public String format(final ProxyPlot entry, final int index) {
                String result = ChatColor.YELLOW.toString() + (index + 1) + ". " + ChatColor.BOLD + entry.getName();
                result += ChatColor.AQUA + " (" + entry.getHomeServer().getName() + ")";
                //TODO Implement location...
//                final ProxyLocation loc = entry.getSignLocation();
//                if (loc != null) {
//                    result = result + ChatColor.AQUA + " - x: " + loc.getBlockX() + ", " + "y: " + loc.getBlockY() + ", " + "z: " + loc.getBlockZ();
//                }
                return result;
            }
        }.display((ProxiedPlayer) sender, Bridge.getPlotManager().getPlots(server), page);
    }

    private ProxyPlot getPlot(final ProxiedPlayer player) throws BungeeCommandException {
        BungeePlotManager mgr = Bridge.getPlotManager();

        ProxyPlot plot = mgr.getPlot(player);

        if (plot == null)
            throw new BungeeCommandException(player.getName() + " does not belong to a plot.");

        return plot;
    }

    private ProxyPlot getPlot(final String id) throws BungeeCommandException {
        BungeePlotManager mgr  = Bridge.getPlotManager();
        final ProxyPlot   plot = mgr.getPlotById(id);
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
    private int getAddLimit(final ProxiedPlayer player) {
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

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE).appendSuper(super.toString())
                .append("plotsForSale", this.plotsForSale)
                .toString();
    }


}
