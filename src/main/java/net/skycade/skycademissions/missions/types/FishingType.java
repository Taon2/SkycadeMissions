package net.skycade.skycademissions.missions.types;

import net.skycade.SkycadeCore.Localization;
import net.skycade.skycademissions.MissionsUser;
import net.skycade.skycademissions.MissionsUserManager;
import net.skycade.skycademissions.SkycadeMissionsPlugin;
import net.skycade.skycademissions.missions.Mission;
import net.skycade.skycademissions.missions.Result;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerFishEvent;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class FishingType extends MissionType {

    private static final Localization.Message NOT_ENOUGH_FISH = new Localization.Message("not-enough-fish", "&cYou need to fish %val% more %type%!");

    private TypesManager typesManager;

    public FishingType(TypesManager typesManager) {
        super();
        this.typesManager = typesManager;
        Localization.getInstance().registerMessages("skycade.factions.missions.fishing",
                NOT_ENOUGH_FISH
        );
    }

    //Listener for the FishingType
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onFishCatch(PlayerFishEvent event) {
        if (event.isCancelled()) return;

        //Loops through all missions for this type
        for (Mission mission : typesManager.getCurrentCountableMissions()) {
            if (mission.getType() == Type.FISHING) {

                List<Map<?, ?>> section = mission.getParams();

                for (Map<?, ?> s : section) {
                    Object type = s.getOrDefault("type", null);
                    if (type == null) continue;

                    //Handles missions that count any item types
                    if (type.toString().equals("ANY")) {
                        int amount = 1;
                        Object obj = s.getOrDefault("amount", null);
                        if (obj != null) amount = (Integer) obj;

                        if (event.getPlayer() != null && event.getCaught() != null && event.getCaught() instanceof Item) {
                            Player p = event.getPlayer();
                            MissionsUser user = MissionsUserManager.getInstance().get(p.getUniqueId());

                            int count = amount;

                            if (SkycadeMissionsPlugin.getInstance().getMissionManager().getType(mission.getType()).getCurrentCount(p.getUniqueId(), mission, type.toString()) < amount) {
                                count = SkycadeMissionsPlugin.getInstance().getMissionManager().getType(mission.getType()).getCurrentCount(p.getUniqueId(), mission, type.toString()) + 1;
                            }

                            user.addCounter(mission, type.toString(), count);
                        }
                    }
                    //Handles missions that count specific item types
                    else {
                        Material materialType = Material.valueOf(type.toString());

                        int amount = 1;
                        Object obj = s.getOrDefault("amount", null);
                        if (obj != null) amount = (Integer) obj;

                        short durability = 0;
                        obj = s.getOrDefault("durability", null);
                        if (obj != null && (short) obj != -1) durability = (short) obj;

                        if (event.getPlayer() != null && event.getCaught() != null && event.getCaught() instanceof Item && ((Item) event.getCaught()).getItemStack().getType() == materialType && ((Item) event.getCaught()).getItemStack().getDurability() == durability) {
                            Player p = event.getPlayer();
                            MissionsUser user = MissionsUserManager.getInstance().get(p.getUniqueId());

                            int count = amount;

                            if (((Item) event.getCaught()).getItemStack().getDurability() == durability) {
                                if (SkycadeMissionsPlugin.getInstance().getMissionManager().getType(mission.getType()).getCurrentCount(p.getUniqueId(), mission, materialType.toString()) < amount) {
                                    count = SkycadeMissionsPlugin.getInstance().getMissionManager().getType(mission.getType()).getCurrentCount(p.getUniqueId(), mission, materialType.toString()) + 1;
                                }
                            }

                            user.addCounter(mission, materialType.toString() + ":" + durability, count);
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
        MissionsUser user = MissionsUserManager.getInstance().get(uuid);

        return user.getCurrentCount(mission, countedThing);
    }
}
