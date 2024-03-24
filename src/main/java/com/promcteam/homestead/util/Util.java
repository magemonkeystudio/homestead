package com.promcteam.homestead.util;


import me.travja.darkrise.core.legacy.util.Vault;
import org.bukkit.ChatColor;

public final class Util {

    private Util() {
    }

    /**
     * Gets the message for deduction from balance.
     *
     * @param amount amount to send
     * @return the message
     */
    public static String getDeductedMessage(final double amount) {
        return String.format("%s%s deducted from your balance.", ChatColor.YELLOW, Vault.format(amount));
    }

    /**
     * Gets the message for addition to balance.
     *
     * @param amount amount to send
     * @return the message
     */
    public static String getAddedMessage(final double amount) {
        return String.format("%s%s added to your balance.", ChatColor.YELLOW, Vault.format(amount));
    }
}
