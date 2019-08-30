package net.skycade.skycademissions.missions.types;

import net.skycade.SkycadeCore.Localization;
import net.skycade.skycademissions.MissionsUser;
import net.skycade.skycademissions.missions.Mission;
import net.skycade.skycademissions.missions.Result;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PlaytimeType extends MissionType {

    private static final Localization.Message NOT_ENOUGH_PLAYTIME = new Localization.Message("not-enough-playtime", "&cYou need to play for %val% more %time%!");

    public PlaytimeType() {
        super();
        Localization.getInstance().registerMessages("skycade.prisons.missions.playtime",
                NOT_ENOUGH_PLAYTIME
        );
    }

    @Override
    public Result validate(Player player, List<Map<?, ?>> params) {
        return new Result(Result.Type.FAILURE);
    }

    @Override
    public Result validate(Player player, List<Map<?, ?>> params, Mission miss) {

        boolean hasFailed = false;

        for (Map<?, ?> s : params) {

            Object type = s.getOrDefault("type", null);
            if (type == null) continue;

            int amount = 1;
            Object obj = s.getOrDefault("amount", null);
            if (obj != null) amount = (Integer) obj;

            long current  = getCurrentLongCount(player.getUniqueId(), miss, type.toString());
            if (current < amount*3600000) {
                hasFailed = true;
                player.sendMessage(NOT_ENOUGH_PLAYTIME.getMessage(player)
                        .replaceAll("%val%", (amount - current) + "")
                        .replaceAll("%time%", type.toString().toLowerCase() + "")
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
        return Type.PLAYTIME;
    }

    @Override
    public int getCurrentCount(UUID uuid, Mission mission, String countedThing) {
        return (int) (getCurrentLongCount(uuid, mission, countedThing)/3600000);
    }

    private long getCurrentLongCount(UUID uuid, Mission mission, String counted) {
        MissionsUser user = MissionsUser.get(uuid);

        return user.getCurrentLongCount(mission, counted);
    }
}
