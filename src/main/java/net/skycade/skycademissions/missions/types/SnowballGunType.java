package net.skycade.skycademissions.missions.types;

import net.skycade.SkycadeCore.Localization;
import net.skycade.SkycadeEnchants.events.SkycadeSnowballGunEvent;
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

public class SnowballGunType extends MissionType {

    private static final Localization.Message NOT_ENOUGH_HIT = new Localization.Message("not-enough-hit", "&cYou need to hit %val% more players!");

    private TypesManager typesManager;

    public SnowballGunType(TypesManager typesManager) {
        super();
        this.typesManager = typesManager;
        Localization.getInstance().registerMessages("skycade.missions.hit",
                NOT_ENOUGH_HIT
        );
    }

    //Listener for the SnowballGunType
    @EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL)
    public void onSkycadeSnowballHit(SkycadeSnowballGunEvent event) {
        //Loops through all missions for this type
        for (Mission mission : typesManager.getCurrentCountableMissions()) {
            if (mission.getType() == Type.SNOWBALLGUN) {
                List<Map<?, ?>> section = mission.getParams();

                for (Map<?, ?> s : section) {
                    Object type = s.getOrDefault("type", null);
                    if (type == null) continue;

                    //Handles missions that count the target type
                    if (type.toString().equalsIgnoreCase(event.getTarget().getType().toString())) {
                        int amount = 1;
                        Object obj = s.getOrDefault("amount", null);
                        if (obj != null) amount = (Integer) obj;

                        if (event.getShooter() != null && event.getTarget() != null) {
                            Player shooter = event.getShooter();
                            Player target = event.getTarget();
                            MissionsUser user = MissionsUserManager.getInstance().get(shooter.getUniqueId());

                            int count = amount;

                            if (SkycadeMissionsPlugin.getInstance().getMissionManager().getType(mission.getType()).getCurrentCount(shooter.getUniqueId(), mission, type.toString()) < amount) {
                                //If the player has already been hit, return
                                if (user.getHitWithSnowball().contains(target.getUniqueId())) return;
                                user.addHitWithSnowball(target.getUniqueId());

                                count = SkycadeMissionsPlugin.getInstance().getMissionManager().getType(mission.getType()).getCurrentCount(shooter.getUniqueId(), mission, type.toString()) + 1;
                            }

                            user.addCounter(mission, type.toString(), count);
                        }
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
                player.sendMessage(NOT_ENOUGH_HIT.getMessage(player)
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
        return Type.SNOWBALLGUN;
    }

    @Override
    public int getCurrentCount(UUID uuid, Mission mission, String countedThing) {
        MissionsUser user = MissionsUserManager.getInstance().get(uuid);

        return user.getCurrentCount(mission, countedThing);
    }
}
