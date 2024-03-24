package com.promcteam.homestead.util;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

/**
 * Contains methods for serialization.
 */
public final class Serialization {

    private Serialization() {
    }

    /**
     * Serializes {@code location} to the provided configuration section. <br />doesn't set world.
     *
     * @param cs        configuration section to serialize location to
     * @param location  location to serialize
     * @param direction true if direction should be serialized.
     */
    public static void serializeLocation(final ConfigurationSection cs,
                                         final Location location,
                                         final boolean direction) {
        if ((cs == null) || (location == null)) {
            throw new IllegalArgumentException("cs and location can not be null.");
        }

        cs.set("x", location.getX());
        cs.set("y", location.getY());
        cs.set("z", location.getZ());
        if (direction) {
            if (location.getYaw() != 0) {
                cs.set("yaw", location.getYaw());
            }
            if (location.getPitch() != 0) {
                cs.set("pitch", location.getPitch());
            }
        }
    }

    /**
     * Deserializes a location.
     *
     * @param cs configuration section to retrieve serialized location from
     * @return deserialized location
     */
    public static Location deserializeLocation(final ConfigurationSection cs, final World world) {
        if (cs == null) {
            throw new IllegalArgumentException("cs can not be null.");
        }

        if (!cs.isSet("x") || !cs.isSet("y") || !cs.isSet("z")) {
            return null;
        }

        final double x     = cs.getDouble("x");
        final double y     = cs.getDouble("y");
        final double z     = cs.getDouble("z");
        final float  yaw   = cs.isSet("yaw") ? (float) cs.getDouble("yaw") : 0;
        final float  pitch = cs.isSet("pitch") ? (float) cs.getDouble("pitch") : 0;
        return new Location(world, x, y, z, yaw, pitch);
    }
}
