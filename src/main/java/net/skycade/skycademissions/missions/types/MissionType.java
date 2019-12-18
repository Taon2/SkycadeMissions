package net.skycade.skycademissions.missions.types;

import net.skycade.skycademissions.SkycadeMissionsPlugin;
import net.skycade.skycademissions.missions.Mission;
import net.skycade.skycademissions.missions.Result;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public abstract class MissionType implements Listener {

    public MissionType() {
        SkycadeMissionsPlugin.getInstance().getMissionManager().addMissions(this);
    }

    public abstract Result validate(Player player, List<Map<?, ?>> params);

    public abstract Result validate(Player player, List<Map<?, ?>> params, Mission mission);

    public abstract Type getType();

    public abstract int getCurrentCount(UUID uuid, Mission mission, String countedThing);

    public void postComplete(Player player, List<Map<?, ?>> params) {}
}
