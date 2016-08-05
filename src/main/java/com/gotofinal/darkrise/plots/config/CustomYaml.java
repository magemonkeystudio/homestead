package com.gotofinal.darkrise.plots.config;

import java.io.File;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

/**
 * Represents a custom configurable yaml file.
 */
public class CustomYaml
{

    private final File              file;
    private       FileConfiguration config;

    private final Plugin plugin;

    /**
     * Constructs a new CustomYaml object.
     *
     * @param instance instance of the TenJava plugin creating this file
     * @param fileName the file name
     */
    public CustomYaml(final Plugin instance, final String fileName)
    {
        this.plugin = instance;
        this.file = new File(this.plugin.getDataFolder(), fileName);
    }

    /**
     * Reloads the FileConfiguration.
     */
    public void reloadConfig()
    {

        this.config = YamlConfiguration.loadConfiguration(this.file);
    }

    /**
     * Saves the FileConfiguration.
     */
    public void saveConfig()
    {
        if (this.config == null)
        {
            return;
        }
        try
        {
            this.config.save(this.file);
        } catch (final Exception e)
        {
            this.plugin.getLogger().severe("Error occurred while saving file: " + e.getMessage());
            e.printStackTrace();
        }

    }

    /**
     * Gets the FileConfiguration. If it is null, it calls {@link #reloadConfig()}.
     *
     * @return the FileConfiguration object
     */
    public FileConfiguration getConfig()
    {
        return this.getConfig(false);
    }

    /**
     * Gets the FileConfiguration. If it is null, it calls {@link #reloadConfig()}.
     *
     * @param reload true if the configuration should reload, otherwise false
     *
     * @return the FileConfiguration object
     */
    public FileConfiguration getConfig(final boolean reload)
    {
        if ((this.config == null) || reload)
        {
            this.reloadConfig();
        }
        return this.config;
    }

    public boolean exists()
    {
        return this.file.exists();
    }

    /**
     * Saves the default configuration file
     */
    public void saveDefaultConfig()
    {
        if (this.exists())
        {
            return;
        }
        this.plugin.saveResource(this.file.getName(), false);
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE).appendSuper(super.toString()).append("file", this.file).append("config", this.config).append("plugin", this.plugin).toString();
    }
}
