package net.skycade.skycademissions.missions.types;

import net.skycade.SkycadeCore.Localization;
import net.skycade.skycademissions.missions.Mission;
import net.skycade.skycademissions.missions.MissionManager;
import net.skycade.skycademissions.missions.Result;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class GenerateType extends MissionType {

    private static final Localization.Message NOT_ENOUGH_GENERATED = new Localization.Message("not-enough-generated", "&cYou need to generate %val% more enchantments!");

    public GenerateType() {
        super();
        Localization.getInstance().registerMessages("skycade.prisons.missions.generated",
                NOT_ENOUGH_GENERATED
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
                player.sendMessage(NOT_ENOUGH_GENERATED.getMessage(player)
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
        return Type.GENERATE;
    }

    @Override
    public int getCurrentCount(UUID uuid, Mission mission, String countedThing) {
        return MissionManager.getCurrentCount(uuid, mission, countedThing);
    }
}