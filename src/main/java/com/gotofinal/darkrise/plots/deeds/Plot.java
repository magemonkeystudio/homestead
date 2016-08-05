package com.gotofinal.darkrise.plots.deeds;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.gotofinal.darkrise.plots.util.TimeUtil;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;

/**
 * Represents a Plot that can be owned by a player.
 */
public class Plot
{

    private final String   name;
    private       Deed     type;
    private       Location signLocation;
    private       String   owner;
    private       Location home;
    private final List<String> players = new ArrayList<>(7);
    private       long         expiry  = - 1;

    public Plot(final String name)
    {
        this.name = name;
    }

    public Location getHome()
    {
        if (this.home == null)
        {
            this.home = this.signLocation.clone().add(1,0,0);
        }
        return this.home;
    }

    public void setHome(final Location home)
    {
        this.home = home;
    }

    /**
     * Updates a {@link org.bukkit.block.Sign} with this Plot's information.
     *
     * @param sign sign to update
     */
    public void updateSign(final Sign sign)
    {

        final List<String> text = this.toSignText();

        for (int i = 0; i < text.size(); i++)
        {
            sign.setLine(i, text.get(i));
        }

        sign.update();
    }

    /**
     * Resets this Plot to an idle state, removing all players and expiry time.
     */
    public void reset(final ProtectedRegion region)
    {

        this.removeAll(region);
        this.owner = null;
        this.players.clear();
        this.expiry = - 1;
    }

    /**
     * Gets a List with a minimum of 2 entries and maximum of 4 of informative values belonging to this Plot.
     *
     * @return a List of Strings
     */
    public List<String> toSignText()
    {
        final List<String> list = new ArrayList<>(4);
        list.add((this.name.length() > 16) ? this.name.substring(0, 16) : this.name);
        list.add((this.type.getDisplayName() != null) ? this.type.getDisplayName() : this.type.getName());
        if (this.expiry > - 1)
        {
            list.add("Exp: " + TimeUtil.getSingleTimeUnit(this.expiry - System.currentTimeMillis()));
            list.add(this.owner);
        }
        else
        {
            list.add("Tax: " + this.type.getTax());
            list.add(ChatColor.GREEN + "Available");
        }
        return list;
    }

    /**
     * Checks whether this Plot is owned.
     *
     * @return true if this Plot is owned.
     */
    public boolean isOwned()
    {
        return this.owner != null;
    }

    /**
     * Checks if a player owns this Plot
     *
     * @param player player to check
     *
     * @return true if this Plot is owned by the {@code player}
     */
    public boolean isOwner(final Player player)
    {
        return this.isOwner(player.getName());
    }

    /**
     * Checks if a player owns this Plot
     *
     * @param player player to check
     *
     * @return true if this Plot is owned by the {@code player}
     */
    public boolean isOwner(final String player)
    {
        return this.isOwned() && this.owner.equalsIgnoreCase(player);
    }

    /**
     * Gets the {@link com.sk89q.worldguard.protection.regions.ProtectedRegion} object that this Plot belongs to.
     *
     * @return the ProtectedRegion if it exists, otherwise null
     */
    public ProtectedRegion getProtectedRegion()
    {

        if (this.signLocation == null)
        {
            return null;
        }

        final RegionManager regionManager = WorldGuardPlugin.inst().getRegionManager(this.signLocation.getWorld());
        if (regionManager == null)
        {
            return null;
        }
        return regionManager.getRegion(this.name);
    }

    /**
     * Gets the name of this deed. The name is the name of the WorldGuard region
     *
     * @return the name of this deed
     */
    public String getName()
    {
        return this.name;
    }

    /**
     * Gets this Plot's {@link Deed}.
     *
     * @return a Deed object
     */
    public Deed getDeed()
    {
        return this.type;
    }

    /**
     * Sets this Plot's {@link Deed}.
     *
     * @param deed the Deed to set
     */
    public void setDeed(final Deed deed)
    {
        this.type = deed;
    }

    /**
     * Gets this Plot's owner.
     *
     * @return the name of the owner, will return null if the plot is not owned
     */
    public String getOwner()
    {
        return this.owner;
    }

    /**
     * Sets this Plot's owner.
     *
     * @param owner this name of the owner
     */
    public void setOwner(final ProtectedRegion region, final String owner)
    {

        if (this.owner != null)
        {
            region.getOwners().removePlayer(this.owner);
        }
        region.getOwners().addPlayer(owner);
        this.owner = owner;
    }

    /**
     * Gets a list of players the owner has added to this area.
     *
     * @return the List of player names
     */
    public List<String> getPlayers()
    {
        return this.players;
    }

    /**
     * Checks whether a {@link org.bukkit.entity.Player} is in this Plot's player List.
     *
     * @param player player to check
     *
     * @return true if the {@code player} is in the player List, otherwise false
     */
    public boolean hasPlayer(final Player player)
    {
        return this.hasPlayer(player.getName());
    }

    /**
     * Checks whether a player is in this Plot's player List.
     *
     * @param player name of the player to check
     *
     * @return true if the {@code player} is in the player List, otherwise false
     */
    public boolean hasPlayer(final String player)
    {
        return this.players.contains(player.toLowerCase());
    }

    /**
     * Adds a {@link org.bukkit.entity.Player} to this Plot's player List.
     *
     * @param player name of the player to add
     *
     * @return true if the player was added, otherwise false
     */
    public boolean addPlayer(final ProtectedRegion region, final Player player)
    {
        return this.addPlayer(region, player.getName());
    }

    /**
     * Adds a player to this Plot's player List.
     *
     * @param player name of the player to add
     *
     * @return true if the player was added, otherwise false
     */
    public boolean addPlayer(final ProtectedRegion region, final String player)
    {
        final boolean result = ! this.hasPlayer(player);
        if (result)
        {
            this.players.add(player.toLowerCase());
        }

        if (! region.getMembers().contains(player))
        {
            region.getMembers().addPlayer(player);
        }
        return result;
    }

    /**
     * Removes a player from this Plot's player List.
     *
     * @param player name of the player to remove
     *
     * @return true if the player was removed, otherwise false
     */
    public boolean removePlayer(final ProtectedRegion region, final String player)
    {
        region.getMembers().removePlayer(player);
        return this.players.remove(player.toLowerCase());
    }

    public void removeAll(final ProtectedRegion region)
    {

        if (this.owner != null)
        {
            region.getOwners().removePlayer(this.owner);
        }
        for (final String player : this.players)
        {
            region.getMembers().removePlayer(player);
        }
    }

    /**
     * Gets the time (in milliseconds) that this deed will expire.
     *
     * @return -1 if it there is no expiry time, otherwise the time it will expire
     */
    public long getExpiry()
    {
        return this.expiry;
    }

    /**
     * Sets the time (in milliseconds) that this deed will expire.
     *
     * @param expiry the expiry time, otherwise -1 if it should not expire
     */
    public void setExpiry(final long expiry)
    {
        this.expiry = expiry;
    }

    /**
     * Extends the expiry time by the {@link Deed#getExtensionTime()}.
     */
    public void extendExpiry()
    {
        this.extendExpiry(TimeUnit.HOURS.toMillis(this.type.getExtensionTime()));
    }

    /**
     * Extends the expiry time.
     *
     * @param milliseconds milliseconds to extend the expiry time by
     */
    public void extendExpiry(final long milliseconds)
    {
        this.expiry += milliseconds;
    }

    /**
     * Gets the {@link org.bukkit.Location} of the sign belonging to this Plot.
     *
     * @return the Location object
     */
    public Location getSignLocation()
    {
        return this.signLocation;
    }

    /**
     * Sets the {@link org.bukkit.Location} of the sign belonging to this Plot.
     *
     * @param signLocation the Location object to set
     */
    public void setSignLocation(final Location signLocation)
    {
        this.signLocation = signLocation;
        this.getHome();
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE).appendSuper(super.toString()).append("name", this.name).append("players", this.players).append("type", this.type).append("owner", this.owner).append("expiry", this.expiry).append("signLocation", this.signLocation).toString();
    }
}
