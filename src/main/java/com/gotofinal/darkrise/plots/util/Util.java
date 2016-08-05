package com.gotofinal.darkrise.plots.util;

import com.gotofinal.darkrise.spigot.core.Vault;

import org.bukkit.ChatColor;

import net.milkbowl.vault.economy.Economy;

public final class Util
{

    private Util()
    {
    }

    /**
     * Gets the message for deduction from balance.
     *
     * @param economy economy instance to retrieve currency names from
     * @param amount  amount to send
     *
     * @return the message
     */
    public static String getDeductedMessage( final double amount)
    {
        return String.format("%s%s deducted from your balance.", ChatColor.YELLOW, Vault.format(amount));
    }

    /**
     * Gets the message for addition to balance.
     *
     * @param economy economy instance to retrieve currency names from
     * @param amount  amount to send
     *
     * @return the message
     */
    public static String getAddedMessage(final double amount)
    {
        return String.format("%s%s added to your balance.", ChatColor.YELLOW, Vault.format(amount));
    }
}
