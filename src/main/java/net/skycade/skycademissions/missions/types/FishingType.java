package net.skycade.skycademissions.missions.types;

import net.skycade.SkycadeCore.Localization;
import net.skycade.skycademissions.MissionsUser;
import net.skycade.skycademissions.missions.Mission;
import net.skycade.skycademissions.missions.Result;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class FishingType extends MissionType {

    private static final Localization.Message NOT_ENOUGH_FISH = new Localization.Message("not-enough-fish", "&cYou need to fish %val% more %type%!");

    public FishingType() {
        super();
        Localization.getInstance().registerMessages("skycade.factions.missions.fishing",
                NOT_ENOUGH_FISH
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

            short durability = -1;
            obj = s.getOrDefault("durability", null);
            if (obj != null && ((Integer) obj).shortValue() != -1) durability = ((Integer) obj).shortValue();

            String countedThing = type.toString();

            if (durability != -1) {
                countedThing = countedThing + ":" + durability;
            }

            int current  = getCurrentCount(player.getUniqueId(), miss, countedThing);
            if (current < amount) {
                hasFailed = true;
                player.sendMessage(NOT_ENOUGH_FISH.getMessage(player)
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
        return Type.FISHING;
    }

    @Override
    public int getCurrentCount(UUID uuid, Mission mission, String countedThing) {
        MissionsUser user = MissionsUser.get(uuid);

        return user.getCurrentCount(mission, countedThing);
    }
}
