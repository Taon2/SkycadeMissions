package net.skycade.skycademissions.missions.types;

import net.skycade.SkycadeCore.Localization;
import net.skycade.SkycadeEnchants.enchant.common.Enchantment;
import net.skycade.SkycadeEnchants.events.SkycadeCustomEnchantItemEvent;
import net.skycade.prisons.util.EnchantmentTypes;
import net.skycade.skycademissions.MissionsUser;
import net.skycade.skycademissions.MissionsUserManager;
import net.skycade.skycademissions.SkycadeMissionsPlugin;
import net.skycade.skycademissions.missions.Mission;
import net.skycade.skycademissions.missions.Result;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class EnchantType extends MissionType implements Listener {

    private static final Localization.Message NOT_ENOUGH_ENCHANTED = new Localization.Message("not-enough-enchanted", "&cYou need to enchant %val% more items!");

    private TypesManager typesManager;

    public EnchantType(TypesManager typesManager) {
        super();
        this.typesManager = typesManager;
        Localization.getInstance().registerMessages("skycade.missions.enchanted",
                NOT_ENOUGH_ENCHANTED
        );
    }

    //Listener for the EnchantType
    @EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL)
    public void onSkycadeCustomEnchantItem(SkycadeCustomEnchantItemEvent event) {
        //Loops through all missions for this type
        for (Mission mission : typesManager.getCurrentCountableMissions()) {
            if (mission.getType() == Type.ENCHANT) {
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

                            if (SkycadeMissionsPlugin.getInstance().getMissionManager().getType(mission.getType()).getCurrentCount(p.getUniqueId(), mission, type.toString()) < amount) {
                                count = SkycadeMissionsPlugin.getInstance().getMissionManager().getType(mission.getType()).getCurrentCount(p.getUniqueId(), mission, type.toString()) + 1;
                            }

                            user.addCounter(mission, type.toString(), count);
                        }
                    }
                    //Handles missions that count specific block types
                    else {
                        String handle = null;
                        for (Map.Entry<Enchantment, Integer> entry : event.getEnchantments().entrySet()) {
                            Enchantment enchantment = entry.getKey();
                            if (EnchantmentTypes.getCommon().contains(enchantment.getHandle())) {
                                handle = "COMMON";
                            } else if (EnchantmentTypes.getRare().contains(enchantment.getHandle())) {
                                handle = "RARE";
                            } else if (EnchantmentTypes.getEpic().contains(enchantment.getHandle())) {
                                handle = "EPIC";
                            } else if (EnchantmentTypes.getLegendary().contains(enchantment.getHandle())) {
                                handle = "LEGENDARY";
                            }
                        }

                        if (handle == null || !handle.equalsIgnoreCase(type.toString())) continue;

                        int amount = 1;
                        Object obj = s.getOrDefault("amount", null);
                        if (obj != null) amount = (Integer) obj;

                        if (event.getPlayer() != null && event.getEnchantments().size() > 0) {
                            Player p = event.getPlayer();
                            MissionsUser user = MissionsUserManager.getInstance().get(p.getUniqueId());

                            int count = amount;

                            if (SkycadeMissionsPlugin.getInstance().getMissionManager().getType(mission.getType()).getCurrentCount(p.getUniqueId(), mission, handle) < amount) {
                                count = SkycadeMissionsPlugin.getInstance().getMissionManager().getType(mission.getType()).getCurrentCount(p.getUniqueId(), mission, handle) + 1;
                            }

                            user.addCounter(mission, handle, count);
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
        MissionsUser user = MissionsUserManager.getInstance().get(uuid);

        return user.getCurrentCount(mission, countedThing);
    }
}
