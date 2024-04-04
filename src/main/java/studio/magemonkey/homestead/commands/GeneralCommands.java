package studio.magemonkey.homestead.commands;

import studio.magemonkey.homestead.Homestead;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.NestedCommand;
import org.bukkit.command.CommandSender;

public class GeneralCommands {
    @SuppressWarnings("UnusedParameters") // meh, WG command API
    public GeneralCommands(final Homestead instance) {
    }

    @Command(aliases = {"homestead"}, desc = "Homestead main commands")
    @NestedCommand(HouseCommand.class)
    public void homestead(final CommandContext args, final CommandSender sender) {
    }

    @Command(aliases = {"house"/*"plot*/}, desc = "Plot management commands")
    @NestedCommand(PlotCommands.class)
    public void plot(final CommandContext args, final CommandSender sender) {
    }

    @Command(aliases = {"deed"}, desc = "Deed management commands")
    @NestedCommand(DeedCommands.class)
    public void deed(final CommandContext args, final CommandSender sender) {
    }
}
