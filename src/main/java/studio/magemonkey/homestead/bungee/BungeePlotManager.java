package studio.magemonkey.homestead.bungee;

import studio.magemonkey.homestead.bungee.util.CordUtil;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.config.Configuration;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class BungeePlotManager {

    private ArrayList<ProxyPlot> plots = new ArrayList<>();

    private Bridge plugin;

    public BungeePlotManager(Bridge instance) {
        this.plugin = instance;
    }

    public void loadAll() {
        Configuration plotSection = plugin.getData().getSection("plots");
        for (String pName : plotSection.getKeys()) {
            ProxyPlot plot = new ProxyPlot(pName, plotSection.getSection(pName));
            plots.add(plot);
        }
        plugin.getProxy()
                .getScheduler()
                .schedule(plugin, () -> CordUtil.broadcast("FETCHPlotInfo"), 1L, TimeUnit.SECONDS);
    }

    public void save() {
        Configuration data = plugin.getData().getSection("plots");
        for (ProxyPlot plot : plots) {
            String name = plot.getName();
            data.set(name + ".server", plot.getHomeServer().getName());
            data.set(name + ".owner", plot.getOwner());
            data.set(name + ".players", plot.getPlayers());
            data.set(name + ".deed", plot.getDeed());
            data.set(name + ".friends", plot.getFriends());
            data.set(name + ".region", plot.getRegionId());
            data.set(name + ".expiry", plot.getExpiry());
        }
        plugin.getData().set("plots", data);
        plugin.saveData();
    }

    public void addPlot(ProxyPlot plot) {
        removePlotByName(plot.getName());
        plots.add(plot);
        save();
    }

    public void removePlotByName(String name) {
        ProxyPlot toRemove = null;
        for (ProxyPlot plot : plots) {
            if (plot.getName().equalsIgnoreCase(name))
                toRemove = plot;
        }

        if (toRemove != null)
            plots.remove(toRemove);
    }

    public ArrayList<ProxyPlot> getPlots() {
        return plots;
    }

    public ArrayList<ProxyPlot> getPlots(String server) {
        if (server.equals("GLOBAL"))
            return plots;

        ArrayList<ProxyPlot> ret = new ArrayList<>();
        for (ProxyPlot plot : plots) {
            if (plot.getHomeServer().getName().equalsIgnoreCase(server))
                ret.add(plot);
        }

        return ret;
    }

    public ProxyPlot getPlot(ProxiedPlayer player) {
        for (ProxyPlot plot : plots) {
            if (plot.isOwner(player) || plot.hasPlayer(player))
                return plot;
        }
        return null;
    }

    public ProxyPlot getPlotById(String id) {
        for (ProxyPlot plot : plots) {
            if (plot.getName().equalsIgnoreCase(id))
                return plot;
        }
        return null;
    }

}
