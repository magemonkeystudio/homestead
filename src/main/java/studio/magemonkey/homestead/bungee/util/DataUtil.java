package studio.magemonkey.homestead.bungee.util;

import net.md_5.bungee.config.Configuration;
import studio.magemonkey.homestead.bungee.Bridge;

import java.util.List;

public class DataUtil {

    public static void removePlayer(String server, String plot, String player) {
        Configuration data = Bridge.getData().getSection("plots");
        if (data == null)
            throw new NullPointerException("Plot information doesn't exist.");

        List<String> players = data.getStringList(plot + ".players");
        if (!players.contains(player))
            return;
        players.remove(player);
        data.set(plot + ".players", players);
        data.set(plot + ".server", server);
        Bridge.saveData();
    }

    public static void addPlayer(String server, String plot, String player) {
        Configuration data = Bridge.getData().getSection("plots");
        if (data == null)
            throw new NullPointerException("Plot information doesn't exist.");

        List<String> players = data.getStringList(plot + ".players");
        if (players.contains(player))
            return;
        players.add(player);
        data.set(plot + ".players", players);
        data.set(plot + ".server", server);
        Bridge.saveData();
    }

    public static void updateOwner(String server, String plot, String owner) {
        Configuration data = Bridge.getData().getSection("plots");
        if (data == null)
            throw new NullPointerException("Plot information doesn't exist.");

        data.set(plot + ".owner", owner);
        data.set(plot + ".server", server);
        Bridge.saveData();
    }


    public static String getHomeServer(String player) {
        player = player.toLowerCase();
        Configuration data = Bridge.getData().getSection("plots");
        if (data == null)
            throw new NullPointerException("Plot information doesn't exist.");

        for (String key : data.getKeys()) {
            if (data.getList(key + ".players").contains(player) && data.getString(key + ".owner")
                    .equalsIgnoreCase(player))
                return data.getString(key + ".server");
        }

        return "notFound";
    }

    public static String getPlotName(String player) {
        player = player.toLowerCase();
        Configuration data = Bridge.getData().getSection("plots");
        if (data == null)
            throw new NullPointerException("Plot information doesn't exist.");

        for (String key : data.getKeys()) {
            if (data.getList(key + ".players").contains(player) && data.getString(key + ".owner")
                    .equalsIgnoreCase(player))
                return key;
        }

        return "notFound";
    }
}
