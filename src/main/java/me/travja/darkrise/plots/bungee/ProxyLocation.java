package me.travja.darkrise.plots.bungee;

import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.config.Configuration;

public class ProxyLocation implements Cloneable {

    private ServerInfo server;
    private String world;

    double x, y, z;

    public ProxyLocation(ServerInfo server, String world, double x, double y, double z) {
        this.server = server;
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public ProxyLocation(Configuration section) {
        this.server = Bridge.getInstance().getProxy().getServerInfo(section.getString("server"));
        if(server == null)
            throw new NullPointerException("The server, " + section.getString("server") + " is offline.");
        this.world = section.getString("world");
        this.x = section.getInt("x");
        this.y = section.getInt("y");
        this.z = section.getInt("z");
    }

    public ServerInfo getServer() {
        return server;
    }

    public String getWorld() {
        return world;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public ProxyLocation add(double x, double y, double z) {
        this.x += x;
        this.y += y;
        this.z += z;

        return this;
    }

    @Override
    public ProxyLocation clone() {
        return new ProxyLocation(server, world, x, y, z);
    }
}
