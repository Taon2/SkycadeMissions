package net.skycade.skycademissions.missions.types;

import net.skycade.SkycadeCore.Localization;
import net.skycade.skycademissions.MissionsUser;
import net.skycade.skycademissions.MissionsUserManager;
import net.skycade.skycademissions.SkycadeMissionsPlugin;
import net.skycade.skycademissions.missions.Mission;
import net.skycade.skycademissions.missions.Result;
import org.bukkit.Bukkit;
import org.bukkit.entity.AnimalTamer;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityTameEvent;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class TamingType extends MissionType {

    private static final Localization.Message NOT_ENOUGH_TAMED = new Localization.Message("not-enough-tamed", "&cYou need to tame %val% more %type%!");

    private TypesManager typesManager;

    public TamingType(TypesManager typesManager) {
        super();
        this.typesManager = typesManager;
        Localization.getInstance().registerMessages("skycade.factions.missions.taming",
                NOT_ENOUGH_TAMED
        );
    }

    //Listener for the MiningType
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onBlockBreak(EntityTameEvent event) {
        if (event.isCancelled()) return;

        int randomId = ThreadLocalRandom.current().nextInt();
        if (SkycadeMissionsPlugin.getInstance().getMissionManager().isDebug) {
            Bukkit.getLogger().info(randomId + "Running animal tame for " + event.getOwner().getName());
        }

        //Loops through all missions for this type
        for (Mission mission : typesManager.getCurrentCountableMissions()) {

            if (SkycadeMissionsPlugin.getInstance().getMissionManager().isDebug) {
                Bukkit.getLogger().info(randomId + "Looping through currentCountableMissions " + mission.getHandle());
            }

            if (mission.getType() == Type.TAMING) {

                if (SkycadeMissionsPlugin.getInstance().getMissionManager().isDebug) {
                    Bukkit.getLogger().info(randomId + "Confirmed " + mission.getHandle() + " is type TAMING");
                }

                List<Map<?, ?>> section = mission.getParams();

                for (Map<?, ?> s : section) {
                    Object type = s.getOrDefault("type", null);
                    if (type == null) continue;

                    //Handles missions that count any block types
                    if (type.toString().equals("ANY")) {
                        int amount = 1;
                        Object obj = s.getOrDefault("amount", null);
                        if (obj != null) amount = (Integer) obj;

                        AnimalTamer p = event.getOwner();
                        MissionsUser user = MissionsUserManager.getInstance().get(p.getUniqueId());

                        int count = amount;

                        if (SkycadeMissionsPlugin.getInstance().getMissionManager().getType(mission.getType()).getCurrentCount(p.getUniqueId(), mission, type.toString()) < amount) {
                            count = SkycadeMissionsPlugin.getInstance().getMissionManager().getType(mission.getType()).getCurrentCount(p.getUniqueId(), mission, type.toString()) + 1;
                        }

                        user.addCounter(mission, type.toString(), count);
                    }
                    //Handles missions that count specific block types
                    else {
                        EntityType entityType = EntityType.valueOf(type.toString());

                        int amount = 1;
                        Object obj = s.getOrDefault("amount", null);
                        if (obj != null) amount = (Integer) obj;

                        if (event.getEntityType() == entityType) {
                            AnimalTamer p = event.getOwner();
                            MissionsUser user = MissionsUserManager.getInstance().get(p.getUniqueId());

                            int count = amount;

                            if (SkycadeMissionsPlugin.getInstance().getMissionManager().getType(mission.getType()).getCurrentCount(p.getUniqueId(), mission, entityType.toString()) < amount) {
                                count = SkycadeMissionsPlugin.getInstance().getMissionManager().getType(mission.getType()).getCurrentCount(p.getUniqueId(), mission, entityType.toString()) + 1;
                            }

                            user.addCounter(mission, entityType.toString(), count);
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
                player.sendMessage(NOT_ENOUGH_TAMED.getMessage(player)
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
        return Type.TAMING;
    }

    @Override
    public int getCurrentCount(UUID uuid, Mission mission, String countedThing) {
        MissionsUser user = MissionsUserManager.getInstance().get(uuid);

        return user.getCurrentCount(mission, countedThing);
    }
}
