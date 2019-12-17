package net.skycade.skycademissions.missions.types;

import net.skycade.SkycadeCore.Localization;
import net.skycade.skycademissions.MissionsUser;
import net.skycade.skycademissions.MissionsUserManager;
import net.skycade.skycademissions.missions.Mission;
import net.skycade.skycademissions.missions.MissionManager;
import net.skycade.skycademissions.missions.Result;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class KillType extends MissionType {

    private static final Localization.Message NOT_ENOUGH_KILLS = new Localization.Message("not-enough-kills", "&cYou need to kill %val% more %type%!");

    private TypesManager typesManager;

    public KillType(TypesManager typesManager) {
        super();
        this.typesManager = typesManager;
        Localization.getInstance().registerMessages("skycade.factions.missions.kills",
                NOT_ENOUGH_KILLS
        );
    }

    //Listener for the KillType
    @EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL)
    public void onEntityDeath(EntityDeathEvent e) {
        //Loops through all missions for this type
        for (Mission mission : typesManager.getCurrentCountableMissions()) {
            if (mission.getType() == Type.KILLS) {
                List<Map<?, ?>> section = mission.getParams();

                for (Map<?, ?> s : section) {
                    Object type = s.getOrDefault("type", null);
                    if (type == null) continue;

                    if (type.toString().equals("ANY")) {
                        int amount = 1;
                        Object obj = s.getOrDefault("amount", null);
                        if (obj != null) amount = (Integer) obj;

                        if (e.getEntity().getKiller() != null) {
                            Player p = e.getEntity().getKiller();
                            MissionsUser user = MissionsUserManager.getInstance().get(p.getUniqueId());

                            int count = amount;

                            if (MissionManager.getType(mission.getType()).getCurrentCount(p.getUniqueId(), mission, type.toString()) < amount) {
                                count = MissionManager.getType(mission.getType()).getCurrentCount(p.getUniqueId(), mission, type.toString()) + 1;
                            }

                            user.addCounter(mission, type.toString(), count);
                        }
                    } else {
                        EntityType entityType = EntityType.valueOf(type.toString());

                        int amount = 1;
                        Object obj = s.getOrDefault("amount", null);
                        if (obj != null) amount = (Integer) obj;

                        if (e.getEntity().getKiller() != null && e.getEntity().getKiller().getType() == EntityType.PLAYER && e.getEntity().getType() == entityType) {
                            Player p = e.getEntity().getKiller();
                            MissionsUser user = MissionsUserManager.getInstance().get(p.getUniqueId());

                            int count = amount;

                            if (MissionManager.getType(mission.getType()).getCurrentCount(p.getUniqueId(), mission, entityType.toString()) < amount) {
                                count = MissionManager.getType(mission.getType()).getCurrentCount(p.getUniqueId(), mission, entityType.toString()) + 1;
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
                player.sendMessage(NOT_ENOUGH_KILLS.getMessage(player)
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
        return Type.KILLS;
    }

    @Override
    public int getCurrentCount(UUID uuid, Mission mission, String countedThing) {
        MissionsUser user = MissionsUserManager.getInstance().get(uuid);

        return user.getCurrentCount(mission, countedThing);
    }
}
