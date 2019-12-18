package net.skycade.skycademissions.missions.types;

import net.skycade.SkycadeCore.Localization;
import net.skycade.kitpvp.bukkitevents.KitPvPSpecialAbilityEvent;
import net.skycade.skycademissions.MissionsUser;
import net.skycade.skycademissions.MissionsUserManager;
import net.skycade.skycademissions.SkycadeMissionsPlugin;
import net.skycade.skycademissions.missions.Mission;
import net.skycade.skycademissions.missions.Result;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SpecialAbilityType extends MissionType {

    private static final Localization.Message NOT_ENOUGH_SPECIAL_ABILITY = new Localization.Message("not-enough-special-ability", "&cYou need to use your special ability %val% more times!");

    private TypesManager typesManager;

    public SpecialAbilityType(TypesManager typesManager) {
        super();
        this.typesManager = typesManager;
        Localization.getInstance().registerMessages("skycade.missions.special-ability",
                NOT_ENOUGH_SPECIAL_ABILITY
        );
    }

    //Listener for the SpecialAbilityType
    @EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL)
    public void onKitPvPSpecialAbility(KitPvPSpecialAbilityEvent event) {
        //Loops through all missions for this type
        for (Mission mission : typesManager.getCurrentCountableMissions()) {
            if (mission.getType() == Type.SPECIALABILITY) {
                List<Map<?, ?>> section = mission.getParams();

                for (Map<?, ?> s : section) {
                    Object type = s.getOrDefault("type", null);
                    if (type == null) continue;

                    //Handles missions that count all special abilities used
                    int amount = 1;
                    Object obj = s.getOrDefault("amount", null);
                    if (obj != null) amount = (Integer) obj;

                    MissionsUser user = MissionsUserManager.getInstance().get(event.getPlayer().getUniqueId());

                    int count = amount;

                    if (SkycadeMissionsPlugin.getInstance().getMissionManager().getType(mission.getType()).getCurrentCount(event.getPlayer().getUniqueId(), mission, type.toString()) < amount) {
                        count = SkycadeMissionsPlugin.getInstance().getMissionManager().getType(mission.getType()).getCurrentCount(event.getPlayer().getUniqueId(), mission, type.toString()) + 1;
                    }

                    user.addCounter(mission, type.toString(), count);
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
                player.sendMessage(NOT_ENOUGH_SPECIAL_ABILITY.getMessage(player)
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
        return Type.SPECIALABILITY;
    }

    @Override
    public int getCurrentCount(UUID uuid, Mission mission, String countedThing) {
        MissionsUser user = MissionsUserManager.getInstance().get(uuid);

        return user.getCurrentCount(mission, countedThing);
    }
}
