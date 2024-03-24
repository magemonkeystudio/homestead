package com.promcteam.homestead.util.bungee;

import com.google.common.io.ByteArrayDataInput;
import org.bukkit.entity.Player;

public interface MessageHandler {

    void run(ByteArrayDataInput in, Player player);

}
