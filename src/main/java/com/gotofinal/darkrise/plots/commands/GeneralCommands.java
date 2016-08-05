package com.gotofinal.darkrise.plots.commands;

import com.gotofinal.darkrise.plots.DarkRisePlots;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.NestedCommand;

import org.bukkit.command.CommandSender;

public class GeneralCommands
{
    @SuppressWarnings("UnusedParameters") // meh, WG command API
    public GeneralCommands(final DarkRisePlots instance)
    {
    }

    @Command(aliases = {"pmco"}, desc = "PlayMCOnline main commands")
    @NestedCommand(PMCOCommand.class)
    public void pmco(final CommandContext args, final CommandSender sender)
    {
    }

    @Command(aliases = {"plot"}, desc = "Plot management commands")
    @NestedCommand(PlotCommands.class)
    public void plot(final CommandContext args, final CommandSender sender)
    {
    }

    @Command(aliases = {"deed"}, desc = "Deed management commands")
    @NestedCommand(DeedCommands.class)
    public void deed(final CommandContext args, final CommandSender sender)
    {
    }
}
