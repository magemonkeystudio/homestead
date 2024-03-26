package com.promcteam.homestead;

import com.promcteam.codex.bungee.BungeeUtil;
import com.promcteam.codex.config.legacy.LegacyConfigManager;
import com.promcteam.codex.legacy.placeholder.PlaceholderRegistry;
import com.promcteam.codex.legacy.placeholder.PlaceholderType;
import com.promcteam.codex.util.messages.MessageUtil;
import com.promcteam.homestead.commands.GeneralCommands;
import com.promcteam.homestead.config.ConfigHandler;
import com.promcteam.homestead.deeds.Deed;
import com.promcteam.homestead.deeds.GlobalPlotsManager;
import com.promcteam.homestead.deeds.Plot;
import com.promcteam.homestead.deeds.PlotManager;
import com.promcteam.homestead.util.bungee.BungeeListener;
import com.promcteam.homestead.util.bungee.JoinListener;
import com.sk89q.bukkit.util.CommandsManagerRegistration;
import com.sk89q.minecraft.util.commands.*;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import lombok.Getter;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;

public class Homestead extends JavaPlugin {
    @Getter
    private static Homestead                      instance;
    @Getter
    private        GlobalPlotsManager             globalPlotsManager;
    @Getter
    private        ConfigHandler                  configHandler;
    private        CommandsManager<CommandSender> commandsManager;

    private void setupCommands() {
        this.commandsManager = new CommandsManager<CommandSender>() {
            @Override
            public boolean hasPermission(final CommandSender sender, final String permission) {
                return sender.hasPermission(permission);
            }
        };
        this.commandsManager.setInjector(new SimpleInjector(this));
        final CommandsManagerRegistration registration = new CommandsManagerRegistration(this, this.commandsManager);
        registration.register(GeneralCommands.class);
    }

    {
        instance = this;
    }

    public Plot getPlot(final Player player) {
        final PlotManager mgr = getGlobalPlotsManager().getPlotManager(player.getWorld());
        if (mgr == null) {
            return null;
        }
        for (final Plot plot : mgr.getPlots().values()) {
            if (plot.isOwner(player)) {
                return plot;
            }
        }
        return null;
    }

    @Override
    public void reloadConfig() {
        this.configHandler = new ConfigHandler(this);
    }

    public static final PlaceholderType<ProtectedRegion> PROTECTED_REGION =
            PlaceholderType.create("wgregion", ProtectedRegion.class);
    public static final PlaceholderType<Plot>            PLOT             = PlaceholderType.create("plot", Plot.class);
    public static final PlaceholderType<Deed>            DEED             = PlaceholderType.create("deed", Deed.class);

    @Override
    public void onLoad() {
        reloadConfig();
        this.getServer().getMessenger().registerIncomingPluginChannel(this, BungeeUtil.CHANNEL, new BungeeListener());
        PROTECTED_REGION.registerItem("id", ProtectedRegion::getId);
        PROTECTED_REGION.registerItem("priority", ProtectedRegion::getPriority);
        PROTECTED_REGION.registerItem("type", r -> r.getType().getName());
        PROTECTED_REGION.registerItem("members", r -> r.getMembers().toUserFriendlyString());
        PROTECTED_REGION.registerItem("owners", r -> r.getOwners().toUserFriendlyString());
        PLOT.registerItem("name", Plot::getName);
        PLOT.registerItem("owner", Plot::getOwner);
        PLOT.registerItem("deedName", p -> p.getDeed().getDisplayName());
        PLOT.registerItem("players", p -> p.getPlayers().toString());
        PLOT.registerItem("isOwned", Plot::isOwned);
        PLOT.registerItem("members", p -> p.getProtectedRegion().getMembers().toUserFriendlyString());
        PLOT.registerItem("owners", p -> p.getProtectedRegion().getOwners().toUserFriendlyString());
        DEED.registerItem("name", Deed::getName);
        DEED.registerItem("description", Deed::getDescription);
        DEED.registerItem("displayName", Deed::getDisplayName);
        DEED.registerItem("dropChance", Deed::getDropChance);
        DEED.registerItem("extensionTime", Deed::getExtensionTime);
        DEED.registerItem("friends", Deed::getFriends);
        DEED.registerItem("initialExtensionTime", Deed::getInitialExtensionTime);
        DEED.registerItem("maximumExtensionTime", Deed::getMaximumExtensionTime);
        DEED.registerItem("tax", Deed::getTax);

        PROTECTED_REGION.registerChild("parent", PROTECTED_REGION, ProtectedRegion::getParent);
        PLOT.registerChild("region", PROTECTED_REGION, Plot::getProtectedRegion);
        PLOT.registerChild("home", PlaceholderRegistry.LOCATION, Plot::getHome);
        PLOT.registerChild("sign", PlaceholderRegistry.LOCATION, Plot::getSignLocation);
        PLOT.registerChild("region", DEED, Plot::getDeed);
        FileConfiguration lang =
                LegacyConfigManager.loadConfigFile(new File(getDataFolder() + File.separator + "lang", "lang_en.yml"),
                        getResource("lang/lang_en.yml"));
        MessageUtil.load(lang, this);
        super.onLoad();
    }

    @Override
    public boolean onCommand(final @NotNull CommandSender sender,
                             final Command cmd,
                             final String commandLabel,
                             final String[] args) {
        try {
            this.commandsManager.execute(cmd.getName(), args, sender, sender);
        } catch (final CommandPermissionsException e) {
            sender.sendMessage(ChatColor.RED + "You don't have permission.");
        } catch (final MissingNestedCommandException e) {
            sender.sendMessage(ChatColor.RED + e.getUsage());
        } catch (final CommandUsageException e) {
            sender.sendMessage(ChatColor.RED + e.getMessage());
            sender.sendMessage(ChatColor.RED + e.getUsage());
        } catch (final WrappedCommandException e) {
            if (e.getCause() instanceof NumberFormatException) {
                sender.sendMessage(ChatColor.RED + "Number expected, string received instead.");
            } else {
                sender.sendMessage(ChatColor.RED + "An error has occurred. See console.");
                e.printStackTrace();
            }
        } catch (final CommandException e) {
            sender.sendMessage(ChatColor.RED + e.getMessage());
        }

        return true;
    }


    @Override
    public void onEnable() {
        super.onEnable();
        this.setupCommands();
        this.globalPlotsManager = new GlobalPlotsManager(this);
        this.globalPlotsManager.reloadConfig();
        this.getServer().getPluginManager().registerEvents(new JoinListener(), this);
    }

    @Override
    public void onDisable() {
        if (this.globalPlotsManager != null) {
            this.globalPlotsManager.unloadAll();
        }
        super.onDisable();
    }
}
