package net.skycade.skycademissions.command;

import net.skycade.SkycadeCore.utility.command.SkycadeCommand;
import net.skycade.SkycadeCore.utility.command.addons.NoConsole;
import net.skycade.skycademissions.missions.MissionManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@NoConsole
public class MissionsCommand extends SkycadeCommand {

    public MissionsCommand() {
        super("missions");
    }

    @Override
    public void onCommand(CommandSender sender, String[] args) {
        final Player player = (Player) sender;
        if (player.getUniqueId() == null) {
            return;
        }

        MissionManager.openMissionGui(player);
    }
}
