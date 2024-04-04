package studio.magemonkey.homestead.bungee;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.Connection;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.event.EventHandler;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;
import java.util.logging.Logger;

import static studio.magemonkey.homestead.bungee.util.DataUtil.getHomeServer;
import static studio.magemonkey.homestead.bungee.util.DataUtil.getPlotName;

public class BungeeListener implements Listener {

    private        Logger                       log;
    private static HashMap<String, MessageData> cached = new HashMap<>();
    private static ArrayList<String>            recent = new ArrayList<>();

    {
        log = Bridge.getInstance().getLogger();
        Configuration config = Bridge.getConfig();
        Configuration cache  = config.getSection("cache");
        for (String key : cache.getKeys()) {
            Configuration section = cache.getSection(key);
            MessageData data =
                    new MessageData(section.getString("server"), section.getStringList("data").toArray(new String[0]));
            cached.put(key, data);


            for (ServerInfo info : Bridge.getInstance().getProxy().getServers().values()) {
                if (info.getName().equals(data.getServer()))
                    continue;

                info.sendData(Bridge.CHANNEL, data.toByteArray(), true);
                log.info("Sending cached data to " + info.getName());
            }
        }

        config.set("cache", null);
        Bridge.saveConfig();
    }

    public static void save() {
        Configuration config = Bridge.getConfig();
        for (String key : cached.keySet()) {
            config.set("cache." + key + ".server", cached.get(key).getServer());
            config.set("cache." + key + ".data", cached.get(key).getData());
        }
        Bridge.saveConfig();
    }

    @EventHandler
    public void send(PluginMessageEvent event) {
        Connection sender   = event.getSender();
        Connection receiver = event.getReceiver();
        byte[]     data     = event.getData();
        String     tag      = event.getTag();

        if (!tag.equals(Bridge.CHANNEL))
            return;


        String dat = "";

        ByteArrayDataInput in       = ByteStreams.newDataInput(data);
        byte[]             msgbytes = new byte[data.length];
        in.readFully(msgbytes);

        DataInputStream msgin = new DataInputStream(new ByteArrayInputStream(msgbytes));
        String          read;
        try {
            String idstr   = msgin.readUTF();
            UUID   id      = UUID.fromString(idstr);
            String server  = msgin.readUTF();
            String command = msgin.readUTF();

            System.out.println("Received DarkRise command of: " + command);

            if (command.equals("PlotHome")) {

                String     dest       = msgin.readUTF();
                String     serverStr  = getHomeServer(dest);
                ServerInfo destServer = Bridge.getInstance().getProxy().getServerInfo(serverStr);
                if (!serverStr.equals(server)) {
                    if (serverStr.equals("notFound") || destServer == null) {
                        MessageData msg =
                                new MessageData("Bungee", "PlotMessage", receiver.toString(), "Home was not found.");
                        ServerInfo info = Bridge.getInstance().getProxy().getServerInfo(server);
                        info.sendData(Bridge.CHANNEL, msg.toByteArray(), true);
                        return;
                    }

                    ProxiedPlayer player = Bridge.getInstance().getProxy().getPlayer(receiver.toString());
                    if (player == null)
                        return;

                    player.connect(destServer);
                }

                ByteArrayDataOutput out = ByteStreams.newDataOutput();

                out.writeUTF("TPPlotHome" + idstr);
                out.writeUTF(receiver.toString());
                out.writeUTF(getPlotName(dest)); //This is the destination
                destServer.sendData(Bridge.CHANNEL, out.toByteArray());
                return;
            } else if (command.equals("PlotsReceived")) {
                String key = id.toString() + "~" + ((Server) sender).getInfo().getName();
                if (!cached.containsKey(key))
                    return;
                ArrayList<String> copy = (ArrayList<String>) cached.get(key).getData().clone();
                copy.remove(0);//Remove the id and the server.
                copy.remove(0);
                String str = join(copy);

                recent.add(str);
                removeDelay(str);
                cached.remove(key);
            } else if (command.equalsIgnoreCase("PUTPlotInfo")) {
                ProxyPlot plot = new ProxyPlot(msgin.readUTF());
                while (msgin.available() > 0) {
                    String reading = msgin.readUTF();
                    String value   = reading.substring(reading.indexOf(":") + 1);
                    if (reading.startsWith("owner"))
                        plot.setOwner(value);
                    else if (reading.startsWith("players")) {
                        String rawPlayers = value;
                        if (!rawPlayers.trim().isEmpty()) {
                            if (rawPlayers.contains(",")) {
                                String[] players = rawPlayers.split(",");
                                for (String pl : players)
                                    plot.addPlayer(pl);
                            } else
                                plot.addPlayer(rawPlayers);
                        }
                    } else if (reading.startsWith("deed"))
                        plot.setDeed(value);
                    else if (reading.startsWith("friends"))
                        plot.setFriends(Integer.parseInt(value));
                    else if (reading.startsWith("region"))
                        plot.setRegionId(value);
                    else if (reading.startsWith("expiry"))
                        plot.setExpiry(Long.parseLong(value));
                    else if (reading.startsWith("final_expiry"))
                        plot.setFinalExpiry(Long.parseLong(value));
                }
                plot.setServer(((Server) sender).getInfo());
                Bridge.getPlotManager().addPlot(plot);
            } else if (command.equalsIgnoreCase("CANCEL_HOME")) {
                BungeePlotCommands.cancelWarmup(ProxyServer.getInstance().getPlayer(msgin.readUTF()));
            } else {
                ArrayList<String> messages = new ArrayList<>();
                messages.add(idstr);
                messages.add(server);
                messages.add(command);

                while (msgin.available() > 0) {
                    read = msgin.readUTF();
                    messages.add(read);
                    dat += read + ", ";
                }

                if (dat.length() >= 2) {
                    dat = dat.substring(0, dat.lastIndexOf(","));
                }


                log.info("Sender: " + server + ", Receiver: " + receiver.toString() + ", Tag: " + tag + ", Command: "
                        + command + ", Data: " + dat);
                if (!recent.contains(dat)) { //If this data hasn't come through in the past 0.5 seconds, we'll send it on.
                    MessageData msgdata = new MessageData(command, messages.toArray(new String[0]));
                    for (ServerInfo info : Bridge.getInstance().getProxy().getServers().values()) {
                        if (info.getName().equals(((Server) sender).getInfo().getName()))
                            continue;

                        info.sendData(Bridge.CHANNEL, msgdata.toByteArray(), true);
                        log.info("Forwarding data to " + info.getName() + " (" + dat + ")");
                        recent.add(dat);
                        removeDelay(dat);
                        cached.put(id.toString() + "~" + info.getName(), msgdata);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String join(ArrayList<String> list) {
        String ret = "";
        for (String str : list) {
            ret += str + ", ";
        }

        return ret.substring(0, ret.lastIndexOf(","));
    }

    private void removeDelay(String cache) {
        new Thread() {
            @Override
            public void run() {
                try {
                    this.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                recent.remove(cache);
            }
        }.start();
    }

}
