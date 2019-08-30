package net.skycade.skycademissions.missions.types;

import net.skycade.SkycadeCore.Localization;
import net.skycade.skycademissions.MissionsUser;
import net.skycade.skycademissions.missions.Mission;
import net.skycade.skycademissions.missions.Result;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class LevelType extends MissionType {

    private static final Localization.Message NOT_ENOUGH_LEVELS = new Localization.Message("not-enough-levels", "&cYou need to have %val% more levels!");

    public LevelType() {
        super();
        Localization.getInstance().registerMessages("skycade.factions.missions.levels",
                NOT_ENOUGH_LEVELS
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

            int current  = getCurrentCount(player.getUniqueId(), miss, type.toString());
            if (current < amount) {
                hasFailed = true;
                player.sendMessage(NOT_ENOUGH_LEVELS.getMessage(player)
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
    public void postComplete(Player player, List<Map<?, ?>> params) {
        super.postComplete(player, params);

        for (Map<?, ?> s : params) {
            Object type = s.getOrDefault("type", null);
            if (type == null) continue;

            int amount = 1;
            Object obj = s.getOrDefault("amount", null);
            if (obj != null) amount = (Integer) obj;

            if (type.toString().equalsIgnoreCase("LEVELS")) {
                player.setLevel(player.getLevel() - amount);
            }
        }
    }

    @Override
    public Type getType() {
        return Type.LEVEL;
    }

    @Override
    public int getCurrentCount(UUID uuid, Mission mission, String countedThing) {
        int currentAmount = 0;
        Player player = Bukkit.getPlayer(uuid);
        MissionsUser user = MissionsUser.get(uuid);

        List<Map<?, ?>> section = mission.getParams();

        for (Map<?, ?> s : section) {
            Object type = s.getOrDefault("type", null);
            if (type == null) continue;

            int amount = 1;
            Object obj = s.getOrDefault("amount", null);
            if (obj != null) amount = (Integer) obj;

            if (type.toString().equalsIgnoreCase("LEVELS")) {
                currentAmount = player.getLevel();

                if (user.hasPlayerCompleted(mission) || currentAmount > amount) {
                    currentAmount = amount;
                }
            }
        }

        return currentAmount;
    }
}
