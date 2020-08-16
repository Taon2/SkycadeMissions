package net.skycade.skycademissions;

import net.skycade.SkycadeCore.SkycadePlugin;
import net.skycade.skycademissions.command.MissionsCommand;
import net.skycade.skycademissions.command.MissionsDebugCommand;
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
    public static boolean v18;

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

        v18 = Bukkit.getServer().getClass().getPackage().getName().contains("1_8");

        typesManager = new TypesManager(this);
        missionManager = new MissionManager();
        missionManager.loadMissions();
        missionManager.loadRewards();
        missionsUserManager = new MissionsUserManager();

        Bukkit.getPluginManager().registerEvents(new PlayerListener(missionsUserManager), this);

        Messages.init();

        new MissionsCommand();
        new MissionsDebugCommand();
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }

    public MissionManager getMissionManager() {
        return missionManager;
    }

    public TypesManager getTypesManager() {
        return typesManager;
    }
}
