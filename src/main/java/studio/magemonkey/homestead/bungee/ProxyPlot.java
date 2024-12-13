package studio.magemonkey.homestead.bungee;

import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.config.Configuration;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import studio.magemonkey.homestead.bungee.util.CordUtil;

import java.util.ArrayList;
import java.util.List;

public class ProxyPlot {

    private final String       name;
    private       String       deed;
    private       String       owner;
    private       ServerInfo   server;
    private       String       regionId;
    private       int          friends;
    private       List<String> players = new ArrayList<>(7);
    private       long         expiry  = -1, finalExpiry = -1;

    public ProxyPlot(final String name) {
        this.name = name;
    }

    public ProxyPlot(String name, Configuration section) {
        this.name = name;
        this.owner = section.getString("owner");
        this.players = section.getStringList("players");
        this.deed = section.getString("deed");
        this.regionId = section.getString("region");
        this.friends = section.getInt("friends");
        this.expiry = section.getLong("expiry");
        String serverID = section.getString("server");
        if (serverID != null && !serverID.trim().isEmpty())
            this.server = Bridge.getInstance().getProxy().getServerInfo(serverID);
    }

    public ServerInfo getHomeServer() {
        return server;
    }

    /**
     * Resets this Plot to an idle state, removing all players and expiry time.
     */
    public void reset() {
        this.owner = null;
        this.players.clear();
        this.expiry = -1;
    }

    public int getFriends() {
        return friends;
    }

    public void setFriends(int friends) {
        this.friends = friends;
    }

    /**
     * Checks whether this Plot is owned.
     *
     * @return true if this Plot is owned.
     */
    public boolean isOwned() {
        return this.owner != null;
    }

    /**
     * Checks if a player owns this Plot
     *
     * @param player player to check
     * @return true if this Plot is owned by the {@code player}
     */
    public boolean isOwner(final ProxiedPlayer player) {
        return this.isOwner(player.getName());
    }

    /**
     * Checks if a player owns this Plot
     *
     * @param player player to check
     * @return true if this Plot is owned by the {@code player}
     */
    public boolean isOwner(final String player) {
        return this.isOwned() && this.owner.equalsIgnoreCase(player);
    }

    /**
     * Gets the name of this deed. The name is the name of the WorldGuard region
     *
     * @return the name of this deed
     */
    public String getName() {
        return this.name;
    }

    /**
     * Gets this Plot's Deed.
     *
     * @return a Deed object
     */
    public String getDeed() {
        return this.deed;
    }

    /**
     * Sets this Plot's Deed.
     *
     * @param deed the Deed to set
     */
    public void setDeed(String deed) {
        this.deed = deed;
    }

    /**
     * Gets this Plot's owner.
     *
     * @return the name of the owner, will return null if the plot is not owned
     */
    public String getOwner() {
        return this.owner;
    }

    /**
     * Sets this Plot's owner.
     *
     * @param owner this name of the owner
     */
    public void setOwner(String owner) {
        this.owner = owner.toLowerCase();
        //Tell Bungee to update its records
        if (server != null)
            CordUtil.sendBungeeMessage(server, "PlotsSetOwner", this.getName(), this.owner);
    }

    /**
     * Gets a list of players the owner has added to this area.
     *
     * @return the List of player names
     */
    public List<String> getPlayers() {
        return this.players;
    }

    /**
     * Checks whether a {@link org.bukkit.entity.Player} is in this Plot's player List.
     *
     * @param player player to check
     * @return true if the {@code player} is in the player List, otherwise false
     */
    public boolean hasPlayer(final ProxiedPlayer player) {
        return this.hasPlayer(player.getName());
    }

    /**
     * Checks whether a player is in this Plot's player List.
     *
     * @param player name of the player to check
     * @return true if the {@code player} is in the player List, otherwise false
     */
    public boolean hasPlayer(final String player) {
        return this.players.contains(player.toLowerCase());
    }

    /**
     * Adds a {@link org.bukkit.entity.Player} to this Plot's player List.
     *
     * @param player name of the player to add
     * @return true if the player was added, otherwise false
     */
    public boolean addPlayer(final ProxiedPlayer player) {
        return this.addPlayer(player.getName());
    }

    /**
     * Adds a player to this Plot's player List.
     *
     * @param player name of the player to add
     * @return true if the player was added, otherwise false
     */
    public boolean addPlayer(final String player) {
        final boolean result = !this.hasPlayer(player);
        if (result) {
            this.players.add(player.toLowerCase());
            //Tell Bungee to update its records
            CordUtil.sendBungeeMessage(server, "PlotsAddToRegion", player);
        }

        return result;
    }

    /**
     * Removes a player from this Plot's player List.
     *
     * @param player name of the player to remove
     * @return true if the player was removed, otherwise false
     */
    public boolean removePlayer(final String player) {
        //Tell Bungee to update its records
        CordUtil.sendBungeeMessage(server, "PlotsRemoveFromRegion", player);
        return this.players.remove(player.toLowerCase());
    }

    /**
     * Gets the time (in milliseconds) that this deed will expire.
     *
     * @return -1 if it there is no expiry time, otherwise the time it will expire
     */
    public long getExpiry() {
        return this.expiry;
    }

    /**
     * Sets the time (in milliseconds) that this deed will expire.
     *
     * @param expiry the expiry time, otherwise -1 if it should not expire
     */
    public void setExpiry(final long expiry) {
        this.expiry = expiry;
    }

    public String getRegionId() {
        return regionId;
    }

    public void setRegionId(String regionId) {
        this.regionId = regionId;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE).appendSuper(super.toString())
                .append("name", this.name).append("players", this.players)
                .append("type", this.deed).append("owner", this.owner)
                .append("expiry", this.expiry).toString();
    }

    public void setServer(ServerInfo info) {
        server = info;
    }

    public long getFinalExpiry() {
        return finalExpiry;
    }

    public void setFinalExpiry(long expiry) {
        this.finalExpiry = expiry;
    }
}
