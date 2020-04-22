package com.gotofinal.darkrise.plots.commands;

import com.gotofinal.darkrise.plots.DarkRisePlots;
import com.gotofinal.darkrise.plots.deeds.Deed;
import com.sk89q.minecraft.util.commands.*;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class DeedCommands {

    private static final String ADD_LIMIT = "add.limit";
    private final DarkRisePlots plugin;

    public DeedCommands(final DarkRisePlots instance) {
        this.plugin = instance;
    }

    @Command(aliases = {"give"}, desc = "Gives a player a deed type.", usage = "<type> [amount] [player]", min = 1, max = 3)
    @CommandPermissions(value = {"deeds.give"})
    public void give(final CommandContext args, final CommandSender sender) throws CommandException {

        Player player = null;

        if (args.argsLength() < 3) {
            if (!(sender instanceof Player)) {
                throw new CommandException("This command can only be used by players, " + "please read the command documentation.");
            }
        }

        final Deed deed = this.getDeed(args.getString(0));

        if (!sender.hasPermission("deeds.give.type." + deed.getName())) {
            throw new CommandPermissionsException();
        }

        final ItemStack item = deed.toItemStack();

        // Set the player to the command sender if the argument length is smaller than 3
        if (args.argsLength() == 1) {
            player = (Player) sender;
        }

        // Set the ItemStack size if the amount argument was provided.
        if (args.argsLength() >= 2) {
            item.setAmount(args.getInteger(1));
        }

        if (args.argsLength() == 3) {
            if (!sender.hasPermission("deeds.give.others")) {
                throw new CommandPermissionsException();
            }
            player = Bukkit.getPlayer(args.getString(2));
        }

        if (player == null) {
            throw new CommandException("That player is offline.");
        }

        player.getInventory().addItem(deed.toItemStack());
    }


    @Command(aliases = {"reload", "rl"}, desc = "Reloads the Deeds configuration.", help = "Reloads the Deeds configuration.", min = 0, max = 0)
    @CommandPermissions("pmco.deeds.reload")
    public void reload(final CommandContext args, final CommandSender sender) {
        this.plugin.getGlobalPlotsManager().reloadConfig();
        sender.sendMessage(ChatColor.YELLOW + "You successfully reloaded Deeds configuration file.");
    }

    @Command(aliases = {"save"}, desc = "Saves the Deeds configuration.", help = "Saves the Deeds configuration.", min = 0, max = 0)
    @CommandPermissions("pmco.deeds.save")
    public void save(final CommandContext args, final CommandSender sender) {
        this.plugin.getGlobalPlotsManager().saveAll();
        sender.sendMessage(ChatColor.YELLOW + "You successfully saved Deeds configuration file.");
    }

    private Deed getDeed(final String name) throws CommandException {

        final Deed deed = this.plugin.getGlobalPlotsManager().getDeedType(name);

        if (deed == null) {
            throw new CommandException("No deed found by that name.");
        }

        return deed;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE).appendSuper(super.toString()).append("plugin", this.plugin).toString();
    }
}
