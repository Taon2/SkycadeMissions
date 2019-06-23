package net.skycade.skycademissions.missions.types;

import net.skycade.skycademissions.missions.Mission;
import net.skycade.skycademissions.missions.MissionManager;
import net.skycade.skycademissions.missions.Result;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import java.util.UUID;

public abstract class MissionType implements Listener {

    MissionType() {
        MissionManager.addMissions(this);
    }

    public abstract Result validate(Player player, ConfigurationSection params);

    public abstract Result validate(Player player, ConfigurationSection params, Mission mission);

    public abstract Type getType();

    public abstract int getCurrentCount(UUID uuid, Mission mission, String countedThing);

    public void postComplete(Player player, ConfigurationSection params) {}
}
