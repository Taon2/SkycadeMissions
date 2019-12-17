package net.skycade.skycademissions.command;

import net.skycade.SkycadeCore.utility.command.SkycadeCommand;
import net.skycade.SkycadeCore.utility.command.addons.NoConsole;
import net.skycade.skycademissions.gui.MissionGui;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@NoConsole
public class MissionsCommand extends SkycadeCommand {

    public MissionsCommand() {
        super("missions");
    }

    @Override
    public void onCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) return;

        new MissionGui().open((Player) sender);
    }
}
