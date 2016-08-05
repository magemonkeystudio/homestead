package com.gotofinal.darkrise.plots.deeds;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.gotofinal.darkrise.plots.DarkRisePlots;
import com.gotofinal.darkrise.plots.config.CustomYaml;
import com.gotofinal.darkrise.plots.util.ConfigUtil;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.material.MaterialData;

/**
 * A class that manages {@link PlotManager}s.
 */
public class GlobalPlotsManager
{

    private final CustomYaml deedsConfig;
    private final Map<World, PlotManager> managers = new HashMap<>(5);
    private final Map<String, Deed>       types    = new HashMap<>(10);
    private final DarkRisePlots plugin;

    public GlobalPlotsManager(final DarkRisePlots instance)
    {
        this.plugin = instance;
        this.deedsConfig = new CustomYaml(this.plugin, "deeds.yml");
        this.plugin.getServer().getPluginManager().registerEvents(new PlotsListener(this.plugin), this.plugin);
    }

    /**
     * Reloads the plots database.
     */
    public void reloadConfig()
    {
        this.deedsConfig.saveDefaultConfig();
        this.deedsConfig.reloadConfig();
        this.loadAll();
    }

    /**
     * Loads all {@link PlotManager}s.
     */
    public void loadAll()
    {

        this.loadDeedTypes();
        if (this.types.isEmpty())
        {
            return;
        }
        if (! this.managers.isEmpty())
        {
            final Set<Map.Entry<World, PlotManager>> entrySet = this.managers.entrySet();
            for (final Map.Entry<World, PlotManager> e : entrySet)
            {
                final PlotManager mgr = e.getValue();
                mgr.getSignUpdater().cancel();
                mgr.setSignUpdater(null);
                this.managers.remove(e.getKey());
            }
        }
        final List<String> worlds = this.deedsConfig.getConfig().getStringList("enabled-worlds");
        for (final String worldName : worlds)
        {
            final World world = Bukkit.getWorld(worldName);
            if (world == null)
            {
                this.plugin.getLogger().severe("'" + worldName + " is not a loaded world, ignoring it.");
                continue;
            }
            this.managers.put(world, new PlotManager(this.plugin, world, this.deedsConfig.getConfig()));
        }

        this.plugin.getLogger().info("Loaded " + this.managers.size() + " worlds for HouseDeeds.");
    }

    /**
     * Loads all the DeedTypes from the configuration file.
     */
    public void loadDeedTypes()
    {

        //TODO create a better reloading system where it doesn't clear and it "updates" loaded Deed values
        this.types.clear();

        final FileConfiguration config = this.deedsConfig.getConfig();
        final ConfigurationSection cs = config.getConfigurationSection("types");

        if (cs == null)
        {
            this.plugin.getLogger().severe("No Plot types found, please define some");
            return;
        }

        for (final String typeName : cs.getKeys(false))
        {
            this.loadDeedType(cs, typeName);
        }
    }

    /**
     * Loads a Deed by name from the configuration.
     *
     * @param typeName
     *         name of the Deed to load
     *
     * @return the loaded Deed if it is valid, otherwise null
     */
    public Deed loadDeedType(final String typeName)
    {
        final ConfigurationSection cs = this.deedsConfig.getConfig().getConfigurationSection("types");
        if (cs == null)
        {
            this.plugin.getLogger().severe("No Plot types found, please define some");
            return null;
        }

        return this.loadDeedType(cs, typeName);
    }

    /**
     * Loads a Deed by name from the configuration.
     *
     * @param cs
     *         ConfigurationSection to operate in
     * @param typeName
     *         name of the Deed to load
     *
     * @return the loaded Deed if it is valid, otherwise null
     */
    @SuppressWarnings("deprecation")
    public Deed loadDeedType(ConfigurationSection cs, String typeName)
    {

        if (this.isValidType(typeName))
        {
            this.plugin.getLogger().severe("Tried to load a Plot Type called '" + typeName + "' but there " + "already is one by that name.");
            return null;
        }
        cs = cs.getConfigurationSection(typeName);
        typeName = typeName.toLowerCase();
        final Deed type = new Deed(typeName);

        // Get or set the default required values
        type.setDisplayName(ChatColor.translateAlternateColorCodes('&', ConfigUtil.getOrSet(cs, "display-name", typeName)));
        type.setDescription(cs.getString("description", ""));
        type.setFriends(ConfigUtil.getOrSet(cs, "friends", 10));
        type.setTax(ConfigUtil.getOrSet(cs, "tax", 100D));
        type.setDropChance(cs.getDouble("drop-chance", 10D));
        type.setMaximumExtensionTime(ConfigUtil.getOrSet(cs, "maximum-extension-time", 24));
        type.setInitialExtensionTime(ConfigUtil.getOrSet(cs, "initial-extension-time", 24));
        type.setExtensionTime(ConfigUtil.getOrSet(cs, "extension-time", 24));

        cs = cs.getConfigurationSection("limited-blocks");
        if (cs != null)
        {
            // Load the limited-blocks section from the deed type for limiting block placement.
            for (final String blocks : cs.getKeys(false))
            {
                final Material material;
                try
                {
                    material = Material.valueOf(blocks.toUpperCase());
                }
                catch (final IllegalArgumentException e)
                {
                    this.plugin.getLogger().severe("Deed '" + typeName + "' has an invalid material called '" + blocks + "'");
                    continue;
                }

                if (! material.isBlock())
                {
                    this.plugin.getLogger().severe("Deed '" + typeName + "' has a material that isn't a block '" + blocks + "'");
                    continue;
                }

                Integer amount;
                byte data;
                if (cs.isConfigurationSection(blocks))
                {
                    amount = ConfigUtil.getOrSet(cs, blocks + ".amount", 0);
                    data = ConfigUtil.getOrSet(cs, blocks + ".data", 0).byteValue();
                }
                else
                {
                    data = - 1;
                    amount = cs.getInt(blocks);
                }
                final MaterialData materialData = new MaterialData(material, data);
                type.addLimitedBlock(materialData, amount);
            }
        }
        this.types.put(typeName.toLowerCase(), type);
        return type;
    }

    public void saveAll()
    {
        this.managers.values().forEach(PlotManager::saveAll);
        this.deedsConfig.saveConfig();
    }

    /**
     * Unloads all DeedManagers safely.
     */
    public void unloadAll()
    {

        this.managers.values().forEach(PlotManager::unloadAll);
        this.deedsConfig.saveConfig();
    }

    /**
     * Gets the Deeds {@link CustomYaml} instance.
     *
     * @return instance of CustomYaml
     */
    public CustomYaml getDeedsConfig()
    {
        return this.deedsConfig;
    }

    /**
     * Gets the Map of {@link PlotManager}s.
     *
     * @return map of DeedManagers
     */
    public Map<World, PlotManager> getManagers()
    {
        return this.managers;
    }

    /**
     * Gets a {@link PlotManager} by world.
     *
     * @param world
     *         world to get the DeedManager from
     *
     * @return instance of DeedManager belonging to {@code world} if it exists, otherwise null
     */
    public PlotManager getPlotManager(final World world)
    {
        return this.managers.get(world);
    }

    /**
     * Gets the Map of {@link Deed}s.
     *
     * @return map of DeedTypes.
     */
    public Map<String, Deed> getTypes()
    {
        return this.types;
    }

    /**
     * Checks if the {@code name} is a valid {@link Deed}.
     *
     * @param name
     *         name to check
     *
     * @return true if {@code name} is a valid type, otherwise false
     */
    public boolean isValidType(final String name)
    {
        return this.types.containsKey(name.toLowerCase());
    }

    /**
     * Gets a {@link Deed} called {@code name}.
     *
     * @param name
     *         name of the Deed to get
     */
    public Deed getDeedType(final String name)
    {
        return this.types.get(name.toLowerCase());
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE).appendSuper(super.toString()).append("deedsConfig", this.deedsConfig).append("managers", this.managers).append("types", this.types).toString();
    }
}
