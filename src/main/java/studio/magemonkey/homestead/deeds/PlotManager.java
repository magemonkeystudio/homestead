package studio.magemonkey.homestead.deeds;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.managers.storage.StorageException;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import studio.magemonkey.homestead.Homestead;
import studio.magemonkey.homestead.util.ConfigUtil;
import studio.magemonkey.homestead.util.Serialization;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a Manager for {@link Plot}. A PlotManager is per world,
 * so an instance will always have a world variable that can be accessed for convenience.
 */
public class PlotManager {

    private final World             world;
    private final Map<String, Plot> plots = new HashMap<>(100);
    private final Homestead         plugin;
    private final FileConfiguration config;
    private       RegionManager     regionManager;
    private       SignUpdater       signUpdater;

    /**
     * Constructs a new PlotsManager.
     *
     * @param instance instance of PlayMCOnline
     * @param world    world this manager should be accessing
     */
    public PlotManager(final Homestead instance, final World world, final FileConfiguration config) {
        this.plugin = instance;
        this.world = world;
        this.config = config;
        this.loadAll();
    }

    public void loadAll() {

        ConfigurationSection cs = ConfigUtil.getOrCreateConfigurationSection(this.config, "plots");

        cs = ConfigUtil.getOrCreateConfigurationSection(cs, this.world.getName());

//        this.regionManager = WorldGuardPlugin.inst().getRegionContainer().get(this.world);
        this.regionManager =
                WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(this.world));

        // Lets make sure WorldGuard is enabled in this world.
        if (this.regionManager == null) {
            this.plugin.getLogger()
                    .severe("WorldGuard is not enabled in world '" + this.world.getName() + "', "
                            + "therefore HouseDeeds is not going to listen for that world.");
            return;
        }

        this.plots.clear();

        for (final String name : cs.getKeys(false)) {
            if (this.load(cs, name) == null) {
                this.plugin.getLogger().severe("Failed to load Plot '" + name + "'.");
            }
        }
        if (this.signUpdater == null) {
            this.signUpdater = new SignUpdater(this.plugin, this);
            this.signUpdater.schedule();
        }
    }

    public void unloadAll() {

        final ConfigurationSection cs = this.config.getConfigurationSection("plots." + this.world.getName());
        for (final Plot plot : this.plots.values()) {
            this.save(cs, plot);
        }
        this.plots.clear();
        try {
            this.regionManager.save();
        } catch (final StorageException e) {
            e.printStackTrace();
        }
        this.signUpdater.cancel();
        this.signUpdater = null;
    }

    /**
     * Saves all the {@link Plot}s safely.
     */
    public void saveAll() {
        this.config.set("plots." + this.world.getName(), null);
        final ConfigurationSection cs = this.config.createSection("plots." + this.world.getName());
        for (final Plot plot : this.plots.values()) {
            this.save(cs, plot);
        }
        try {
            this.regionManager.save();
        } catch (final StorageException e) {
            e.printStackTrace();
        }
    }

    /**
     * Loads all plots in this world.
     */
    public Plot load(final String name) {
        final ConfigurationSection cs = this.config.getConfigurationSection("plots." + this.world.getName());

        if (cs == null) {
            this.plugin.getLogger().severe("No Plots found, please create some.");
            return null;
        }

        return this.load(cs, name);
    }

    /**
     * Loads a Plot by name from the configuration.
     *
     * @param cs   ConfigurationSection to operate in
     * @param name name of the Plot to load
     * @return the loaded deed
     */
    public Plot load(final ConfigurationSection cs, String name) {

        if ((cs == null) || (name == null)) {
            throw new IllegalArgumentException("cs and name can not be null.");
        }

        if (this.hasPlot(name)) {
            this.plugin.getLogger().severe("Tried to load a Plot called '" + name + "' but there " + "already is one " +
                    "by that name.");
            return null;
        }

        final ConfigurationSection temp = cs.getConfigurationSection(name);
        final Plot                 plot = new Plot(name);
        name = name.toLowerCase();

        // Get or set the default required values
        final String typeName = ConfigUtil.getOrSet(temp,
                "type",
                this.plugin.getGlobalPlotsManager().getTypes().keySet().iterator().next());
        final Deed type = this.plugin.getGlobalPlotsManager().getDeedType(typeName);
        if (type == null) {
            this.plugin.getLogger().severe("Plot '" + name + "' has an invalid type called '" + typeName + "'.");
            return null;
        }
        plot.setDeed(type);

        if (temp.isSet("sign")) {
            plot.setSignLocation(Serialization.deserializeLocation(temp.getConfigurationSection("sign"), this.world));
        }

        if (temp.isSet("home")) {
            plot.setHome(Serialization.deserializeLocation(temp.getConfigurationSection("home"), this.world));
        } else if (plot.getSignLocation() != null) {
            plot.setHome(plot.getSignLocation().clone().add(1, 0, 0));
        }

        if (temp.isSet("expiry-time")) {
            plot.setExpiry(temp.getLong("expiry-time"));
            if (plot.getExpiry() > -1) {

                final ProtectedRegion region = plot.getProtectedRegion();
                if (region != null) {
                    region.getOwners().removeAll();
                }

                if (region != null) {
                    region.getMembers().removeAll();
                }

                if (temp.isSet("owner")) {
                    plot.setOwner(region, temp.getString("owner").toLowerCase());
                }

                if (temp.isSet("players")) {
                    for (final String player : temp.getStringList("players")) {
                        plot.addPlayer(region, player.toLowerCase());
                    }
                }
            }
        }
        this.addPlot(plot);
        return plot;
    }

    /**
     * Saves a {@link Plot}.
     *
     * @param cs   ConfigurationSection to save {@code plot} to
     * @param plot plot to save
     */
    public void save(final ConfigurationSection cs, final Plot plot) {
        if ((cs == null) || (plot == null)) {
            throw new IllegalArgumentException("cs and plot can not be null.");
        }

        final String               name = plot.getName();
        final ConfigurationSection temp = ConfigUtil.getOrCreateConfigurationSection(cs, name);

        temp.set("type", plot.getDeed().getName().toLowerCase());

        if (plot.getSignLocation() != null) {
            Serialization.serializeLocation(ConfigUtil.getOrCreateConfigurationSection(temp, "sign"),
                    plot.getSignLocation(),
                    false);
        }
        if (plot.getHome() != null) {
            Serialization.serializeLocation(ConfigUtil.getOrCreateConfigurationSection(temp, "home"),
                    plot.getHome(),
                    false);
        }

        temp.set("expiry-time", plot.getExpiry());
        if (plot.getExpiry() > -1) {
            temp.set("owner", plot.getOwner());
            temp.set("players", plot.getPlayers());
        } else {
            temp.set("owner", null);
            temp.set("players", null);
        }
    }

    /**
     * Gets the {@link org.bukkit.World} this {@link PlotManager} was created with.
     *
     * @return instance of World
     */
    public World getWorld() {
        return this.world;
    }

    /**
     * Gets a Map of {@link Plot} names and their objects belonging to this
     * {@link PlotManager}.
     *
     * @return a Map of plots
     */
    public Map<String, Plot> getPlots() {
        return this.plots;
    }

    /**
     * Checks if a {@link Plot}, by name, is loaded
     *
     * @param name name of the Plot to check
     * @return true if a deed by the {@code name} is loaded, otherwise false
     */
    public boolean hasPlot(final String name) {
        return this.plots.containsKey(name.toLowerCase());
    }

    /**
     * Gets a {@link Plot} by name.
     *
     * @param name name of the Plot to get
     * @return a Plot object called {@code name} if it exists, otherwise null
     */
    public Plot getPlot(final String name) {
        return this.plots.get(name.toLowerCase());
    }

    /**
     * Adds a {@link Plot} to the Map of loaded Plots,
     * this does not load the Plot, it merely adds it to a collection.
     *
     * @param plot plot to add
     */
    public void addPlot(final Plot plot) {
        if (plot == null) {
            throw new IllegalArgumentException("plot can not be null.");
        }
        this.plots.put(plot.getName().toLowerCase(), plot);
        this.plugin.getGlobalPlotsManager().saveAll();
    }

    /**
     * Removes a {@link Plot} from the Map of loaded Plots,
     * this does not save its values.
     *
     * @param plot plot to remove
     * @return the removed Plot, otherwise null if no plot was found
     */
    public boolean removePlot(final Plot plot) {
        if (plot == null) {
            throw new IllegalArgumentException("plot can not be null.");
        }
        return this.removePlot(plot.getName());
    }

    /**
     * Removes a {@link Plot} from the Map of loaded Plots,
     * this does not save its values.
     *
     * @param plot name of the plot to remove
     * @return the removed Plot, otherwise null if no deed was found
     */
    public boolean removePlot(final String plot) {
        boolean removed = this.plots.remove(plot.toLowerCase()) != null;
        this.plugin.getGlobalPlotsManager().saveAll();
        return removed;
    }

    public RegionManager getRegionManager() {
        return this.regionManager;
    }

    public SignUpdater getSignUpdater() {
        return this.signUpdater;
    }

    public void setSignUpdater(final SignUpdater signUpdater) {
        this.signUpdater = signUpdater;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE).appendSuper(super.toString())
                .append("world", this.world)
                .append("plots", this.plots)
                .toString();
    }
}
