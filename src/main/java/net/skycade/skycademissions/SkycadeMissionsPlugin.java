package net.skycade.skycademissions;

import net.skycade.SkycadeCore.SkycadePlugin;
import net.skycade.skycademissions.command.MissionsCommand;
import net.skycade.skycademissions.missions.MissionManager;
import net.skycade.skycademissions.util.Messages;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.Map;
import java.util.TreeMap;

public class SkycadeMissionsPlugin extends SkycadePlugin {

    private static SkycadeMissionsPlugin instance;

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

        MissionManager.loadMissions();

        Bukkit.getPluginManager().registerEvents(new MissionManager(), this);

        Messages.init();

        new MissionsCommand();
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }
}
