package net.skycade.skycademissions;

import net.skycade.SkycadeCore.SkycadePlugin;
import net.skycade.skycademissions.command.MissionsCommand;
import net.skycade.skycademissions.missions.MissionManager;
import net.skycade.skycademissions.util.Messages;
import org.bukkit.Bukkit;

public class SkycadeMissionsPlugin extends SkycadePlugin {

    private static SkycadeMissionsPlugin instance;

    public SkycadeMissionsPlugin() {
        instance = this;
    }

    public static SkycadeMissionsPlugin getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        super.onEnable();

        MissionManager.load();

        Bukkit.getPluginManager().registerEvents(new MissionManager(), this);

        Messages.init();

        new MissionsCommand();
    }

    @Override
    public void onDisable() {
        MissionManager.saveCompletedConfig();
        super.onDisable();
    }
}
