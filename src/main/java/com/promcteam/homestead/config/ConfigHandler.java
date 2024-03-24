package com.promcteam.homestead.config;

import java.util.List;

import com.promcteam.homestead.Homestead;
import com.promcteam.homestead.events.ConfigReloadEvent;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Represents a Config file belonging to this Plugin.
 */
public class ConfigHandler {

    public static final Property<Integer> PLOT_SIGN_INTERACTION_COOLDOWN =
            new Property<>("deeds.sign-interaction-cooldown", 1000);
    public static final Property<Integer> PLOT_SIGN_CONFIRMATION         =
            new Property<>("deeds.sign-confirmation", 30);
    public static final Property<Integer> PLOT_SIGN_UPDATE_INTERVAL      =
            new Property<>("deeds.sign-update-interval", 10);
    public static final Property<Integer> PLOT_SIGN_UPDATE_PER_TASK      =
            new Property<>("deeds.sign-update-per-task", 5);
    public static final Property<Integer> SELL_DISTANCE                  = new Property<>("plots.sell.distance", 8);
    public static final Property<Integer> GRACE_PERIOD                   =
            new Property<>("plots.expiry.grace_period", 48);
    public static final Property<Integer> WARN_INTERVAL                  =
            new Property<>("plots.expiry.warn_interval", 30);
    public static final Property<Integer> HOME_COOLDOWN                  = new Property<>("home-cooldown", 10);
    public static final Property<Integer> HOME_WARMUP                    = new Property<>("home-warmup", 5);
    public final        boolean           IS_BUNGEE;
    public final        String            BUNGEE_ID;
    //public static final Property<List<String>> CHEST_COMMANDS = new Property<>("chest-commands", Collections.<String>emptyList());

    private final CustomYaml yaml;

    /**
     * Constructs a new ConfigHandler instance that handles the config.yml file in the {@code instance}'s data folder.
     *
     * @param instance instance of the plugin creating this
     */
    public ConfigHandler(final Homestead instance) {
        this.yaml = new CustomYaml(instance, "config.yml");
        this.reloadConfig();
        IS_BUNGEE = yaml.getConfig().getBoolean("bungee");
        BUNGEE_ID = yaml.getConfig().getString("bungee-id");
    }

    /**
     * Reloads the configuration file.
     */
    public void reloadConfig() {
        this.yaml.saveDefaultConfig();
        this.yaml.reloadConfig();
        Bukkit.getPluginManager().callEvent(new ConfigReloadEvent(this));
    }

    /**
     * Gets a String value from the {@code property}.
     *
     * @param property property to get
     * @return the value
     */
    public String getString(final Property<String> property) {
        return this.getConfig().getString(property.getPath(), property.getDefaultValue());
    }

    /**
     * Gets a Boolean value from the {@code property}.
     *
     * @param property property to get
     * @return the value
     */
    public Boolean getBoolean(final Property<Boolean> property) {
        return this.getConfig().getBoolean(property.getPath(), property.getDefaultValue());
    }

    /**
     * Gets an Integer value from the {@code property}.
     *
     * @param property property to get
     * @return the value
     */
    public Integer getInt(final Property<Integer> property) {
        return this.getConfig().getInt(property.getPath(), property.getDefaultValue());
    }

    /**
     * Gets a Double value from the {@code property}.
     *
     * @param property property to get
     * @return the value
     */
    public Double getDouble(final Property<Double> property) {
        return this.getConfig().getDouble(property.getPath(), property.getDefaultValue());
    }

    /**
     * Gets a List value from the {@code property}.
     *
     * @param property property to get
     * @return the value
     */
    public List<String> getList(final Property<List<String>> property) {
        return this.getConfig().getStringList(property.getPath());
    }

    /**
     * Gets the FileConfiguration.
     *
     * @return FileConfiguration object.
     */
    public FileConfiguration getConfig() {
        return this.yaml.getConfig();
    }

    /**
     * Gets the ConfigObject.
     *
     * @return the CustomYaml object
     */
    private CustomYaml getCustomYaml() {
        return this.yaml;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE).appendSuper(super.toString())
                .append("yaml", this.yaml)
                .toString();
    }
}
