package com.promcteam.homestead.config;

public class Property<T> implements Comparable<Property<T>> {
    private final String path;
    private final String name;
    private final T      value;

    public Property(final String path, final T t) {
        if ((path == null) || (t == null)) {
            throw new IllegalArgumentException("path or t can not be null.");
        }
        this.path = path;
        this.value = t;
        final int index = path.lastIndexOf('.');
        if (index < 0) {
            this.name = path;
        } else {
            this.name = path.substring(index + 1);
        }
    }

    /**
     * Gets the path of this property.
     *
     * @return the path
     */
    public String getPath() {
        return this.path;
    }

    /**
     * Gets the name of this property.
     *
     * @return the name
     */
    public String getName() {
        return this.name;
    }

    /**
     * Gets the default value of this property.
     *
     * @return the default value
     */
    public T getDefaultValue() {
        return this.value;
    }

    /**
     * Compares this property with another.
     *
     * @param o property to compare with
     * @return a negative integer, zero, or a positive integer as the specified String is greater than, equal to, or less than this String, ignoring
     * case considerations.
     */
    @Override
    public int compareTo(final Property<T> o) {
        return this.name.compareToIgnoreCase(o.getName());
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Property)) {
            return false;
        }

        final Property<?> property = (Property<?>) o;

        return this.path.equals(property.path) && this.name.equals(property.name) && this.value.equals(property.value);

    }

    @Override
    public int hashCode() {
        int result = this.path.hashCode();
        result = (31 * result) + (this.name.hashCode());
        result = (31 * result) + (this.value.hashCode());
        return result;
    }

    @Override
    public String toString() {
        return "Property{name=" + this.name + "}";
    }
}
