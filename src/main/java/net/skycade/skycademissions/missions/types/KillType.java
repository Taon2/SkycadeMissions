package net.skycade.skycademissions.missions.types;

import net.skycade.SkycadeCore.Localization;
import net.skycade.skycademissions.SkycadeMissionsPlugin;
import net.skycade.skycademissions.missions.Mission;
import net.skycade.skycademissions.missions.MissionManager;
import net.skycade.skycademissions.missions.Result;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.*;

public class KillType extends MissionType {

    private static final Localization.Message NOT_ENOUGH_KILLS = new Localization.Message("not-enough-kills", "&cYou are need to kill %val% more %type%!");

    public KillType() {
        super();
        Localization.getInstance().registerMessages("skycade.factions.missions.kills",
                NOT_ENOUGH_KILLS
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

            int amount = 1;
            Object obj = s.getOrDefault("amount", null);
            if (obj != null) amount = (Integer) obj;

            int current  = getCurrentCount(player.getUniqueId(), miss, type.toString());
            if (current < amount) {
                hasFailed = true;
                player.sendMessage(NOT_ENOUGH_KILLS.getMessage(player)
                        .replaceAll("%val%", (amount - current) + "")
                        .replaceAll("%type%", type.toString())
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
        return Type.KILLS;
    }

    @Override
    public int getCurrentCount(UUID uuid, Mission mission, String countedThing) {
        File file = new File(SkycadeMissionsPlugin.getInstance().getDataFolder(), "completed.yml");
        List<Map<?, ?>> section = mission.getParams().getMapList("items");
        int currentCount = 0;

        for (Map<?, ?> s : section) {
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

            //Checks to see if there is an active counter within the last 24 hours
            if (MissionManager.hasPlayerCompleted(uuid, mission)) {
                //Returns max value if already completed
                int amount = 1;
                Object obj = s.getOrDefault("amount", null);
                if (obj != null) amount = (Integer) obj;

                return amount;
            } else if ((!doesCountExist || !isTimeEnabled) && !MissionManager.hasPlayerCompleted(uuid, mission)) {
                //Starts a new counter if there is not an active counter and the mission hasn't been completed
                conf.set(uuid.toString() + ".counters." + mission.getHandle() + "." + countedThing, currentCount);
                conf.set(uuid.toString() + ".counters." + mission.getHandle() + ".activated", System.currentTimeMillis());
            } else {
                //Returns the existing counter
                currentCount = conf.getInt(uuid.toString() + ".counters." + mission.getHandle() + "." + countedThing);
            }
        }
        return currentCount;
    }
}
