package net.skycade.skycademissions.missions.types;

import net.skycade.SkycadeCore.Localization;
import net.skycade.SkycadeEnchants.events.SkycadeGenerateEnchantEvent;
import net.skycade.skycademissions.MissionsUser;
import net.skycade.skycademissions.MissionsUserManager;
import net.skycade.skycademissions.missions.Mission;
import net.skycade.skycademissions.missions.MissionManager;
import net.skycade.skycademissions.missions.Result;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class GenerateType extends MissionType {

    private static final Localization.Message NOT_ENOUGH_GENERATED = new Localization.Message("not-enough-generated", "&cYou need to generate %val% more enchantments!");

    private TypesManager typesManager;

    public GenerateType(TypesManager typesManager) {
        super();
        this.typesManager = typesManager;
        Localization.getInstance().registerMessages("skycade.prisons.missions.generated",
                NOT_ENOUGH_GENERATED
        );
    }

    //Listener for the GenerateType
    @EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL)
    public void onSkycadeEnchantGenerate(SkycadeGenerateEnchantEvent event) {
        //Loops through all missions for this type
        for (Mission mission : typesManager.getCurrentCountableMissions()) {
            if (mission.getType() == Type.GENERATE) {
                List<Map<?, ?>> section = mission.getParams();

                for (Map<?, ?> s : section) {
                    Object type = s.getOrDefault("type", null);
                    if (type == null) continue;

                    //Handles missions that count any enchantments
                    if (type.toString().equals("ANY")) {
                        int amount = 1;
                        Object obj = s.getOrDefault("amount", null);
                        if (obj != null) amount = (Integer) obj;

                        if (event.getPlayer() != null) {
                            Player p = event.getPlayer();
                            MissionsUser user = MissionsUserManager.getInstance().get(p.getUniqueId());

                            int count = amount;

                            if (MissionManager.getType(mission.getType()).getCurrentCount(p.getUniqueId(), mission, type.toString()) < amount) {
                                count = MissionManager.getType(mission.getType()).getCurrentCount(p.getUniqueId(), mission, type.toString()) + 1;
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
        MissionsUser user = MissionsUserManager.getInstance().get(uuid);

        return user.getCurrentCount(mission, countedThing);
    }
}
