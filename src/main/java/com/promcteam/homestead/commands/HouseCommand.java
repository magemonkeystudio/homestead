package com.promcteam.homestead.commands;

import com.promcteam.homestead.Homestead;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandPermissions;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class HouseCommand {

    private final Homestead plugin;

    public HouseCommand(final Homestead instance) {
        this.plugin = instance;
    }

    @Command(aliases = {"reload", "rl"}, desc = "Reloads the configuration files.", usage = "[a]", help =
            "Reloads the configuration files.\n"
                    + "The -a flag is used to determine whether all modules should be reloaded.", min = 0, max = 0, flags = "a")
    @CommandPermissions("homestead.reload")
    public void reload(final CommandContext args, final CommandSender sender) {

        this.plugin.reloadConfig();
        sender.sendMessage(ChatColor.YELLOW + "Configuration file reloaded successfully.");
        if (args.hasFlag('a')) {
            this.plugin.getGlobalPlotsManager().reloadConfig();
            sender.sendMessage(ChatColor.YELLOW + "All configuration files reloaded successfully.");
        }
    }

    @Command(aliases = {"saveall", "save"}, desc = "Saves all the configuration files.", help = "Saves all the configuration files.", min = 0, max = 0)
    @CommandPermissions("homestead.saveall")
    public void saveAll(final CommandContext args, final CommandSender sender) {
        this.plugin.getGlobalPlotsManager().saveAll();
        sender.sendMessage(ChatColor.YELLOW + "All configuration files saved successfully.");
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE).appendSuper(super.toString())
                .append("plugin", this.plugin)
                .toString();
    }
}
