package net.skycade.skycademissions.command;

import net.skycade.SkycadeCore.utility.command.SkycadeCommand;
import net.skycade.SkycadeCore.utility.command.addons.Permissible;
import net.skycade.skycademissions.SkycadeMissionsPlugin;
import net.skycade.skycademissions.missions.DailyMissionManager;
import net.skycade.skycademissions.missions.Mission;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@Permissible("skycade.missions.debug")
public class MissionsDebugCommand extends SkycadeCommand {

    public MissionsDebugCommand() {
        super("missionsdebug");
    }

    @Override
    public void onCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) return;

        SkycadeMissionsPlugin.getInstance().getMissionManager().isDebug = !SkycadeMissionsPlugin.getInstance().getMissionManager().isDebug;

        StringBuilder builder = new StringBuilder();
        for (Mission mission : DailyMissionManager.getInstance().getCurrent()) {
            builder.append(mission.getHandle())
                    .append(DailyMissionManager.getInstance().getCurrent().indexOf(mission) == DailyMissionManager.getInstance().getCurrent().size() - 1
                            ? "" : ", ");
        }
        Bukkit.getLogger().info("Current Missions: " + builder.toString());

        builder = new StringBuilder();
        for (Mission mission : SkycadeMissionsPlugin.getInstance().getTypesManager().getCurrentCountableMissions()) {
            builder.append(mission.getHandle())
                    .append(SkycadeMissionsPlugin.getInstance().getTypesManager().getCurrentCountableMissions().indexOf(mission) == SkycadeMissionsPlugin.getInstance().getTypesManager().getCurrentCountableMissions().size() - 1
                            ? "" : ", ");
        }
        Bukkit.getLogger().info("Current Countable Types: " + builder.toString());
    }
}
