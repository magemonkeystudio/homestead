package com.promcteam.homestead.util;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.WordUtils;
import org.bukkit.Material;

/**
 * Contains {@link org.bukkit.Material} related utility methods.
 */
public final class MaterialUtil {

    private MaterialUtil() {
    }

    /**
     * Gets the human readable name of a Material.
     *
     * @param material material name to get
     * @return the human readable name
     */
    public static String friendlyName(final Material material) {
        if (material == null) {
            throw new IllegalArgumentException("material can not be null.");
        }

        return StringUtils.replace(WordUtils.capitalize(material.name().toLowerCase()), "_", " ");
    }
}
