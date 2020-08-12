package net.skycade.skycademissions.missions.types;

import net.skycade.SkycadeCore.Localization;
import net.skycade.skycademissions.MissionsUser;
import net.skycade.skycademissions.MissionsUserManager;
import net.skycade.skycademissions.SkycadeMissionsPlugin;
import net.skycade.skycademissions.missions.Mission;
import net.skycade.skycademissions.missions.Result;
import net.skycade.skycadeskyblock.server.events.IslandLevelChangeEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static net.skycade.skycadeskyblock.server.SkycadeSkyblockServerPlugin.ISLAND_LEVEL_DIVIDER;

public class IslandLevelType extends MissionType {

    private static final Localization.Message NOT_ENOUGH_CHANGE = new Localization.Message("not-enough-change", "&cYou still need to increase your island level by %val%!");

    private TypesManager typesManager;

    public IslandLevelType(TypesManager typesManager) {
        super();
        this.typesManager = typesManager;
        Localization.getInstance().registerMessages("skycade.missions.islevel",
                NOT_ENOUGH_CHANGE
        );
    }

    //Listener for the IslandLevelType
    @EventHandler
    public void onIslandLevelChange(IslandLevelChangeEvent event) {
        //Loops through all missions for this type
        for (Mission mission : typesManager.getCurrentCountableMissions()) {
            if (mission.getType() == Type.ISLAND_LEVEL) {
                List<Map<?, ?>> section = mission.getParams();

                for (Map<?, ?> s : section) {
                    Object type = s.getOrDefault("type", null);
                    if (type == null) continue;

                    int amount = 1;
                    Object obj = s.getOrDefault("amount", null);
                    if (obj != null) amount = (Integer) obj;

                    if (event.getUuid() != null) {
                        MissionsUser user = MissionsUserManager.getInstance().get(event.getUuid());

                        int count = amount;
                        int change = (int) ((event.getNewLevel() / ISLAND_LEVEL_DIVIDER.getValue())
                                - (event.getOldLevel() / ISLAND_LEVEL_DIVIDER.getValue()));

                        if (SkycadeMissionsPlugin.getInstance().getMissionManager().getType(mission.getType()).getCurrentCount(event.getUuid(), mission, type.toString()) < amount) {
                            count = SkycadeMissionsPlugin.getInstance().getMissionManager().getType(mission.getType()).getCurrentCount(event.getUuid(), mission, type.toString()) + change;
                        }

                        user.addCounter(mission, type.toString(), count);
                    }
                }
            }
        }
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
                player.sendMessage(NOT_ENOUGH_CHANGE.getMessage(player)
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
        return Type.ISLAND_LEVEL;
    }

    @Override
    public int getCurrentCount(UUID uuid, Mission mission, String countedThing) {
        MissionsUser user = MissionsUserManager.getInstance().get(uuid);

        return user.getCurrentCount(mission, countedThing);
    }
}
