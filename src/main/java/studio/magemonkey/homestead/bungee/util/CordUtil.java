package studio.magemonkey.homestead.bungee.util;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import studio.magemonkey.homestead.bungee.Bridge;
import studio.magemonkey.homestead.bungee.MessageData;

public class CordUtil {

    public static void sendMessage(CommandSender sender, String message, String... args) {
        ProxiedPlayer player = (ProxiedPlayer) sender;

        ServerInfo server = player.getServer().getInfo();
        String[]   data   = new String[args.length + 3];
        int        i      = 0;
        data[i++] = "PlotMessage";
        data[i++] = player.getName();
        data[i++] = message;
        for (String arg : args)
            data[i++] = arg;
        server.sendData(Bridge.CHANNEL, createMessage(data).toByteArray(), true);
    }

    public static void sendBungeeMessage(ProxiedPlayer target, String... data) {
        sendBungeeMessage(target.getServer().getInfo(), data);
    }

    public static void sendBungeeMessage(ServerInfo destination, String... data) {
        MessageData msg = createMessage(data);
        if (destination == null || data == null || msg == null)
            return;
        destination.sendData(Bridge.CHANNEL, msg.toByteArray(), true);
    }

    private static MessageData createMessage(String... data) {
        return new MessageData("Bungee", data);
    }

    public static void broadcast(String message) {
        MessageData data = new MessageData("Bungee", message);
        for (ServerInfo server : Bridge.getInstance().getProxy().getServers().values()) {
            System.out.println("Sending broadcast of " + message + " to " + server.getName());
            server.sendData(Bridge.CHANNEL, data.toByteArray());
        }
    }
}
