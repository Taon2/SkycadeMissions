package net.skycade.skycademissions.missions.types;

import net.skycade.SkycadeCore.Localization;
import net.skycade.skycademissions.MissionsUser;
import net.skycade.skycademissions.missions.Mission;
import net.skycade.skycademissions.missions.Result;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class EnchantType extends MissionType {

    private static final Localization.Message NOT_ENOUGH_ENCHANTED = new Localization.Message("not-enough-enchanted", "&cYou need to enchant %val% more items!");

    public EnchantType() {
        super();
        Localization.getInstance().registerMessages("skycade.prisons.missions.enchanted",
                NOT_ENOUGH_ENCHANTED
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
                player.sendMessage(NOT_ENOUGH_ENCHANTED.getMessage(player)
                        .replaceAll("%val%", (amount - current) + "")
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
        return Type.ENCHANT;
    }

    @Override
    public int getCurrentCount(UUID uuid, Mission mission, String countedThing) {
        MissionsUser user = MissionsUser.get(uuid);

        return user.getCurrentCount(mission, countedThing);
    }
}
