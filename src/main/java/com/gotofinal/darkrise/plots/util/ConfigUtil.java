package com.gotofinal.darkrise.plots.util;

import org.bukkit.configuration.ConfigurationSection;

/**
 * Class containing {@link org.bukkit.configuration.file.FileConfiguration} utility methods.
 */
public final class ConfigUtil
{

    private ConfigUtil()
    {
    }

    /**
     * Gets or creates a {@link org.bukkit.configuration.ConfigurationSection} out of the {@code path}.
     *
     * @param cs   configuration section to operate in
     * @param path path of the ConfigurationSection to get
     *
     * @return the ConfigurationSection
     */
    public static ConfigurationSection getOrCreateConfigurationSection(final ConfigurationSection cs, final String path)
    {
        return cs.isConfigurationSection(path) ? cs.getConfigurationSection(path) : cs.createSection(path);
    }

    public static <T> T getOrSet(final ConfigurationSection cs, final String path, final T value)
    {

        if (cs == null)
        {
            throw new IllegalArgumentException("cs can not be null.");
        }
        if (! cs.isSet(path))
        {
            cs.set(path, value);
        }
        //noinspection unchecked
        return (T) cs.get(path, value);
    }
}
