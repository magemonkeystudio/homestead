package com.promcteam.homestead.deeds;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Represents a Deed that can be used for Plots.
 */
public class Deed {

    private final String       name;
    //    private final Map<MaterialData, Integer> limitedBlocks = new HashMap<>(20);
    private       String       displayName;
    private       List<String> description;
    private       int          friends;
    private       double       tax;
    private       double       dropChance;
    private       int          maximumExtensionTime;
    private       int          initialExtensionTime;
    private       int          extensionTime;
    private       int          customData;

    /**
     * Constructs a new Deed with {@code name} and default values. Please see the setter methods.
     *
     * @param name name of this deed type
     */
    public Deed(final String name) {
        this.name = name;
    }

    /**
     * Creates an {@link org.bukkit.inventory.ItemStack} out of this Deed's values.
     *
     * @return an ItemStack object with amount set to 1
     * @see #toItemStack(int)
     */
    public ItemStack toItemStack() {
        return this.toItemStack(1);
    }

    /**
     * Creates an {@link org.bukkit.inventory.ItemStack} out of this Deed's values.
     *
     * @param amount amount to create this ItemStack with
     * @return an ItemStack object
     */
    public ItemStack toItemStack(final int amount) {
        final ItemStack item = new ItemStack(Material.PAPER, amount);
        final ItemMeta  im   = item.getItemMeta();
        im.setDisplayName(this.displayName);
        if (this.description != null && !this.description.isEmpty()) {
            im.setLore(this.description);
        }

        if (this.customData != -1) {
            im.setCustomModelData(customData);
        }

        item.setItemMeta(im);
        return item;
    }

    /**
     * Gets the name of this Deed.
     *
     * @return name of this Deed
     */
    public String getName() {
        return this.name;
    }

    /**
     * Gets the display name of this Deed. Don't get this confused with {@link #getName()}. The display name is a
     * human-readable name.
     *
     * @return the display name
     */
    public String getDisplayName() {
        return this.displayName;
    }

    /**
     * Sets the display name of this Deed. The display name is a human-readable name.
     *
     * @param displayName the display name to set
     */
    public void setDisplayName(final String displayName) {
        this.displayName = displayName;
    }

    public List<String> getDescription() {
        return this.description;
    }

    public void setDescription(final List<String> description) {
        this.description = description.stream()
                .map(str -> ChatColor.translateAlternateColorCodes('&', str))
                .collect(Collectors.toList());
    }

    public int getFriends() {
        return this.friends;
    }

    public void setFriends(final int friends) {
        this.friends = friends;
    }

    /**
     * Gets the tax of the Deed.
     *
     * @return the tax to set
     */
    public double getTax() {
        return this.tax;
    }

    /**
     * Sets the tax of the Deed.
     *
     * @param tax tax to set
     */
    public void setTax(final double tax) {
        this.tax = tax;
    }

    /**
     * Gets the drop chance of this Deed.
     *
     * @return double value of the drop chance
     */
    public double getDropChance() {
        return this.dropChance;
    }

    /**
     * Sets the drop chance of this Deed.
     *
     * @param dropChance the drop chance to set
     */
    public void setDropChance(final double dropChance) {
        this.dropChance = dropChance;
    }

    /**
     * Gets the maximum extension time for this Deed.
     *
     * @return the maximum extension time
     */
    public int getMaximumExtensionTime() {
        return this.maximumExtensionTime;
    }

    /**
     * Sets the maximum extension time for this Deed.
     *
     * @param maximumExtensionTime the maximum extension time to set
     */
    public void setMaximumExtensionTime(final int maximumExtensionTime) {
        this.maximumExtensionTime = maximumExtensionTime;
    }

    /**
     * Gets the initial extension time for this Deed.
     *
     * @return the initial extension time
     */
    public int getInitialExtensionTime() {
        return this.initialExtensionTime;
    }

    /**
     * Sets the initial extension time for this Deed.
     *
     * @param initialExtensionTime the initial extension time to set
     */
    public void setInitialExtensionTime(final int initialExtensionTime) {
        this.initialExtensionTime = initialExtensionTime;
    }

    /**
     * Gets the extension time for this Deed.
     *
     * @return the extension time
     */
    public int getExtensionTime() {
        return this.extensionTime;
    }

    /**
     * Sets the extension time for this Deed.
     *
     * @param extensionTime the initial extension time to set
     */
    public void setExtensionTime(final int extensionTime) {
        this.extensionTime = extensionTime;
    }

//    /**
//     * Gets a Map of blocks that are limited for this Deed.
//     *
//     * @return a Map of MaterialData and the limit
//     */
//    public Map<MaterialData, Integer> getLimitedBlocks()
//    {
//        return this.limitedBlocks;
//    }

//    /**
//     * Adds the {@code data} to the Map of limited blocks.
//     *
//     * @param data   the MaterialData to limit
//     * @param amount amount to limit
//     */
//    public void addLimitedBlock(final MaterialData data, final Integer amount)
//    {
//
//        for (final MaterialData current : this.limitedBlocks.keySet())
//        {
//            if (data.equals(current))
//            {
//                return;
//            }
//        }
//        this.limitedBlocks.put(data, amount);
//    }

//    /**
//     * Removes the {@code data} from the Map of limited blocks.
//     *
//     * @param data the MaterialData to remove
//     *
//     * @return true if the Map has been modified
//     */
//    public boolean removeLimitedBlock(final MaterialData data)
//    {
//        return this.removeLimitedBlock(data, true);
//    }

//    /**
//     * Removes the {@code data} from the Map of limited blocks.
//     *
//     * @param data  the MaterialData to remove
//     * @param exact true if the {@code data} object should be removed. Otherwise false if a similar
//     *              object should be removed, ex: if an object has the same type id and data it will
//     *              remove that
//     *
//     * @return true if the Map has been modified
//     */
//    public boolean removeLimitedBlock(final MaterialData data, final boolean exact)
//    {
//
//        if (exact)
//        {
//            // remove() returns null if data does not exist in the map.
//            return this.limitedBlocks.remove(data) != null;
//        }
//        for (final MaterialData other : this.limitedBlocks.keySet())
//        {
//            if (data.equals(other))
//            {
//                this.limitedBlocks.remove(other);
//                return true;
//            }
//        }
//        return false;
//    }

    public void setCustomData(int data) {
        this.customData = data;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE).appendSuper(super.toString())
                .append("name", this.name)
                /*.append("limitedBlocks", this.limitedBlocks)*/.append("displayName", this.displayName)
                .append("description", String.join("\n\t", this.description))
                .append("friends", this.friends)
                .append("tax", this.tax)
                .append("dropChance", this.dropChance)
                .append("maximumExtensionTime", this.maximumExtensionTime)
                .append("initialExtensionTime", this.initialExtensionTime)
                .append("extensionTime", this.extensionTime)
                .toString();
    }
}
