package net.skycade.skycademissions.missions.types;

import net.skycade.SkycadeCore.Localization;
import net.skycade.kitpvp.bukkitevents.KitPvPKillPlayerEvent;
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

public class KitKillType extends MissionType {

    private static final Localization.Message NOT_ENOUGH_KIT_KILLS = new Localization.Message("not-enough-special-ability", "&cYou need to kill players with %val% more kits!");

    private TypesManager typesManager;

    public KitKillType(TypesManager typesManager) {
        super();
        this.typesManager = typesManager;
        Localization.getInstance().registerMessages("skycade.missions.kit-kills",
                NOT_ENOUGH_KIT_KILLS
        );
    }

    //Listener for the SpecialAbilityType
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onKitPvPKillPlayer(KitPvPKillPlayerEvent event) {
        //Loops through all missions for this type
        for (Mission mission : typesManager.getCurrentCountableMissions()) {
            if (mission.getType() == Type.KITKILL) {
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
                        //If the player has already used this special ability, return
                        if (user.getKitKillKitsUsed().contains(event.getKitType().getKit().getName())) return;
                        user.addKitKillKitUsed(event.getKitType().getKit().getName());

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
                player.sendMessage(NOT_ENOUGH_KIT_KILLS.getMessage(player)
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
        return Type.KITKILL;
    }

    @Override
    public int getCurrentCount(UUID uuid, Mission mission, String countedThing) {
        MissionsUser user = MissionsUserManager.getInstance().get(uuid);

        return user.getCurrentCount(mission, countedThing);
    }
}
