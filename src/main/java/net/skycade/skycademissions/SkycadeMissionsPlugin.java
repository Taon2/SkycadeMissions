package net.skycade.skycademissions;

import net.skycade.SkycadeCore.SkycadePlugin;
import net.skycade.skycademissions.command.MissionsCommand;
import net.skycade.skycademissions.listeners.PlayerListener;
import net.skycade.skycademissions.missions.MissionManager;
import net.skycade.skycademissions.missions.types.TypesManager;
import net.skycade.skycademissions.util.Messages;
import org.bukkit.Bukkit;

import java.util.Map;
import java.util.TreeMap;

public class SkycadeMissionsPlugin extends SkycadePlugin {

    private static SkycadeMissionsPlugin instance;
    private MissionManager missionManager;
    private MissionsUserManager missionsUserManager;
    private TypesManager typesManager;

    public SkycadeMissionsPlugin() {
        instance = this;
    }

    public static SkycadeMissionsPlugin getInstance() {
        return instance;
    }

    private void defaults() {
        Map<String, Object> defaults = new TreeMap<>();
        defaults.put("refresh-missions", true);

        setConfigDefaults(defaults);
        loadDefaultConfig();
    }

    @Override
    public void onEnable() {
        super.onEnable();
        defaults();

        missionManager = new MissionManager();
        missionsUserManager = new MissionsUserManager();
        new TypesManager(this);

        Bukkit.getPluginManager().registerEvents(new PlayerListener(missionsUserManager), this);

        Messages.init();

        new MissionsCommand();
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }

    public MissionManager getMissionManager() {
        return missionManager;
    }
}
