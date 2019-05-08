package net.skycade.skycademissions;

import net.skycade.SkycadeCore.SkycadePlugin;
import net.skycade.skycademissions.command.MissionsCommand;
import net.skycade.skycademissions.missions.MissionManager;
import net.skycade.skycademissions.util.Messages;

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

        Messages.init();

        new MissionsCommand();
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }
}
