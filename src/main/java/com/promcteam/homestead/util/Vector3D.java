package com.promcteam.homestead.util;

/*
 * Vector3D UTil
 *
 * Copyright 2012 Kristian S. Stangeland (Comphenix)
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library. If not, see <[url]http://www.gnu.org/licenses/>[/url].
 */

import org.bukkit.Location;
import org.bukkit.util.Vector;

public class Vector3D {
    /**
     * Represents the null (0, 0, 0) origin.
     */
    public static final Vector3D ORIGIN = new Vector3D(0, 0, 0);

    // Use protected members, like Bukkit
    public final double x;
    public final double y;
    public final double z;

    /**
     * Construct an immutable 3D vector.
     */
    public Vector3D(final double x, final double y, final double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    /**
     * Construct an immutable floating point 3D vector from a location object.
     *
     * @param location - the location to copy.
     */
    public Vector3D(final Location location) {
        this(location.toVector());
    }

    /**
     * Construct an immutable floating point 3D vector from a mutable Bukkit vector.
     *
     * @param vector - the mutable real Bukkit vector to copy.
     */
    public Vector3D(final Vector vector) {
        if (vector == null) {
            throw new IllegalArgumentException("Vector cannot be NULL.");
        }
        this.x = vector.getX();
        this.y = vector.getY();
        this.z = vector.getZ();
    }

    /**
     * Convert this instance to an equivalent real 3D vector.
     *
     * @return Real 3D vector.
     */
    public Vector toVector() {
        return new Vector(this.x, this.y, this.z);
    }

    /**
     * Adds the current vector and a given position vector, producing a result vector.
     *
     * @param other - the other vector.
     * @return The new result vector.
     */
    public Vector3D add(final Vector3D other) {
        if (other == null) {
            throw new IllegalArgumentException("other cannot be NULL");
        }
        return new Vector3D(this.x + other.x, this.y + other.y, this.z + other.z);
    }

    /**
     * Adds the current vector and a given vector together, producing a result vector.
     *
     * @param x - the x coordinate.
     * @param y - the y coordinate.
     * @param z - the z coordinate.
     * @return The new result vector.
     */
    public Vector3D add(final double x, final double y, final double z) {
        return new Vector3D(this.x + x, this.y + y, this.z + z);
    }

    /**
     * Subtracts the current vector and a given vector, producing a result position.
     *
     * @param other - the other position.
     * @return The new result position.
     */
    public Vector3D subtract(final Vector3D other) {
        if (other == null) {
            throw new IllegalArgumentException("other cannot be NULL");
        }
        return new Vector3D(this.x - other.x, this.y - other.y, this.z - other.z);
    }

    /**
     * Subtracts the current vector and a given vector together, producing a result vector.
     *
     * @param x - the x coordinate.
     * @param y - the y coordinate.
     * @param z - the z coordinate.
     * @return The new result vector.
     */
    public Vector3D subtract(final double x, final double y, final double z) {
        return new Vector3D(this.x - x, this.y - y, this.z - z);
    }

    /**
     * Multiply each dimension in the current vector by the given factor.
     *
     * @param factor - multiplier.
     * @return The new result.
     */
    public Vector3D multiply(final int factor) {
        return new Vector3D(this.x * factor, this.y * factor, this.z * factor);
    }

    /**
     * Multiply each dimension in the current vector by the given factor.
     *
     * @param factor - multiplier.
     * @return The new result.
     */
    public Vector3D multiply(final double factor) {
        return new Vector3D(this.x * factor, this.y * factor, this.z * factor);
    }

    /**
     * Divide each dimension in the current vector by the given divisor.
     *
     * @param divisor - the divisor.
     * @return The new result.
     */
    public Vector3D divide(final int divisor) {
        if (divisor == 0) {
            throw new IllegalArgumentException("Cannot divide by null.");
        }
        return new Vector3D(this.x / divisor, this.y / divisor, this.z / divisor);
    }

    /**
     * Divide each dimension in the current vector by the given divisor.
     *
     * @param divisor - the divisor.
     * @return The new result.
     */
    public Vector3D divide(final double divisor) {
        if (divisor == 0) {
            throw new IllegalArgumentException("Cannot divide by null.");
        }
        return new Vector3D(this.x / divisor, this.y / divisor, this.z / divisor);
    }

    /**
     * Retrieve the absolute value of this vector.
     *
     * @return The new result.
     */
    public Vector3D abs() {
        return new Vector3D(Math.abs(this.x), Math.abs(this.y), Math.abs(this.z));
    }

    @Override
    public String toString() {
        return String.format("[x: %s, y: %s, z: %s]", this.x, this.y, this.z);
    }
}