package com.promcteam.homestead.bungee;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.plugin.PluginManager;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class Bridge extends Plugin {

    private static      Configuration     config;
    private static      Configuration     data;
    private static      Bridge            instance;
    public static final String            CHANNEL = "travja:darkrise";
    private static      BungeePlotManager plotManager;

    @Override
    public void onEnable() {
        instance = this;
        loadConfig();

        register();

        plotManager = new BungeePlotManager(this);
        plotManager.loadAll();

        getLogger().info("ProMCHousing-Bungee has been enabled!");
    }

    @Override
    public void onDisable() {
        BungeeListener.save();
    }

    private void register() {
        PluginManager pm = getProxy().getPluginManager();
        pm.registerListener(this, new BungeeListener());
        pm.registerCommand(this, new BungeePlotCommands("house", "",
                ChatColor.RED + "/house <add> <player>\n" +
                        "       <players|buy|sell|list|home|admin|remove>\n",
                "bplot"));
        getProxy().registerChannel(CHANNEL);
        getLogger().info("Registered channel: " + CHANNEL);
    }

    public static Bridge getInstance() {
        return instance;
    }

    public static BungeePlotManager getPlotManager() {
        return plotManager;
    }

    public static Configuration getConfig() {
        if (config == null)
            loadConfig();

        return config;
    }

    private static void loadConfig() {
        try {
            File    file    = new File(instance.getDataFolder(), "config.yml");
            boolean newFile = !file.exists();
            if (newFile) {
                file.getParentFile().mkdirs();
                file.createNewFile();
            }
            config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(file);
            if (newFile) { //set defaults
                config.set("home-cooldown", 10);
                config.set("home-warmup", 5);
                saveConfig();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void saveConfig() {
        try {
            ConfigurationProvider.getProvider(YamlConfiguration.class)
                    .save(config, new File(instance.getDataFolder(), "config.yml"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Configuration getData() {
        if (data == null)
            loadData();

        return data;
    }

    private static void loadData() {
        try {
            File file = new File(instance.getDataFolder(), "data.yml");
            if (!file.exists()) {
                file.getParentFile().mkdirs();
                file.createNewFile();
            }
            data = ConfigurationProvider.getProvider(YamlConfiguration.class).load(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void saveData() {
        try {
            ConfigurationProvider.getProvider(YamlConfiguration.class)
                    .save(data, new File(instance.getDataFolder(), "data.yml"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
