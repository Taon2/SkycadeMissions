package net.skycade.skycademissions.missions.types;

import net.skycade.SkycadeCore.Localization;
import net.skycade.skycademissions.SkycadeMissionsPlugin;
import net.skycade.skycademissions.missions.Mission;
import net.skycade.skycademissions.missions.MissionManager;
import net.skycade.skycademissions.missions.Result;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.*;

public class DamageType extends MissionType {

    private static final Localization.Message NOT_ENOUGH_DAMAGE = new Localization.Message("not-enough-damage", "&cYou are need to deal %val% more damage to %type%!");

    public DamageType() {
        super();
        Localization.getInstance().registerMessages("skycade.factions.missions.inventory",
                NOT_ENOUGH_DAMAGE
        );
    }

    @Override
    public Result validate(Player player, ConfigurationSection params) {
        return new Result(Result.Type.FAILURE);
    }

    @Override
    public Result validate(Player player, ConfigurationSection params, Mission miss) {

        boolean hasFailed = false;

        List<Map<?, ?>> section = params.getMapList("items");

        for (Map<?, ?> s : section) {

            Object type = s.getOrDefault("type", null);
            if (type == null) continue;
            EntityType entityType = EntityType.valueOf(type.toString());

            int amount = 1;
            Object obj = s.getOrDefault("amount", null);
            if (obj != null) amount = (Integer) obj;

            int current  = getCurrentCount(player.getUniqueId(), miss, type.toString());
            if (current < amount) {
                hasFailed = true;
                player.sendMessage(NOT_ENOUGH_DAMAGE.getMessage(player)
                        .replaceAll("%val%", (amount - current) + "")
                        .replaceAll("%type%", entityType.name().toLowerCase().replaceAll("_", " "))
                );
            }
        }
        if (hasFailed){
            return new Result(Result.Type.FAILURE);
        }

        return new Result(Result.Type.SUCCESS);
    }

    @Override
    public Type getType() {
        return Type.DAMAGE;
    }

    @Override
    public int getCurrentCount(UUID uuid, Mission mission, String countedThing) {
        File file = new File(SkycadeMissionsPlugin.getInstance().getDataFolder(), "completed.yml");

        YamlConfiguration conf;

        if (!file.exists()) {
            conf = new YamlConfiguration();
        } else {
            conf = YamlConfiguration.loadConfiguration(file);
        }

        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("Europe/London"));
        calendar.set(Calendar.HOUR, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);

        long timeInMillis = calendar.getTimeInMillis();

        boolean doesCountExist = conf.contains(uuid.toString() + ".counters." + mission.getHandle());
        boolean isTimeEnabled = conf.getLong(uuid.toString() + ".counters." + mission.getHandle() + ".activated") > timeInMillis;
        int currentCount = 0;

        //Checks to see if there is an active counter within the last 24 hours
        if ((!doesCountExist || !isTimeEnabled) && !MissionManager.hasPlayerCompleted(uuid, mission)) {
            //Starts a new counter if there is not an active counter and the mission hasn't been completed
            conf.set(uuid.toString() + ".counters." + mission.getHandle() + ".count", currentCount);
            conf.set(uuid.toString() + ".counters." + mission.getHandle() + ".activated", System.currentTimeMillis());
        }

        //Returns the existing counter
        currentCount = conf.getInt(uuid.toString() + ".counters." + mission.getHandle() + ".count");

        return currentCount;
    }
}
