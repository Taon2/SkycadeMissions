package net.skycade.skycademissions.gui;

import net.md_5.bungee.api.ChatColor;
import net.skycade.SkycadeCore.guis.dynamicnew.DynamicGui;
import net.skycade.SkycadeCore.utility.ItemBuilder;
import net.skycade.skycademissions.MissionsUser;
import net.skycade.skycademissions.MissionsUserManager;
import net.skycade.skycademissions.SkycadeMissionsPlugin;
import net.skycade.skycademissions.missions.DailyMissionManager;
import net.skycade.skycademissions.missions.MissionVerifyAction;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MissionGui extends DynamicGui {

    private static final ItemStack REWARDS = new ItemBuilder(Material.NETHER_STAR)
            .setDisplayName(org.bukkit.ChatColor.GOLD + "" + org.bukkit.ChatColor.BOLD + "Rewards")
            .setLore("Displays the rewards you can win.")
            .build();

    private int slot = 11;

    public MissionGui() {
        super(ChatColor.AQUA + "" + ChatColor.BOLD + "Missions", 3);

        DailyMissionManager.getInstance().getCurrent()
                .forEach(mission ->  {
                    setItemInteraction(slot, p -> {
                                ItemBuilder item = new ItemBuilder(mission.getIcon(), mission.getDurability(), 1);

                                item.setDisplayName(ChatColor.AQUA + "" + ChatColor.BOLD + mission.getDisplayName());

                                // Constructs lore
                                List<String> lore = new ArrayList<>();
                                lore.add(org.bukkit.ChatColor.RED + "" + org.bukkit.ChatColor.BOLD + mission.getLevel().toString());
                                lore.add(ChatColor.GOLD + "-");
                                lore.addAll(mission.getLore());
                                lore.add(ChatColor.GOLD + "-");

                                // Constructs the current counts the player has
                                List<Map<?, ?>> params = mission.getParams();
                                ArrayList<String> countingLore = new ArrayList<>();

                                for (Map<?, ?> s : params) {
                                    Object type = s.getOrDefault("type", null);
                                    if (type == null) continue;
                                    String countedThing = type.toString();

                                    int amount = 1;
                                    Object obj = s.getOrDefault("amount", null);
                                    if (obj != null) amount = (Integer) obj;

                                    short durability = -1;
                                    obj = s.getOrDefault("durability", null);
                                    if (obj != null) durability = ((Short) obj);

                                    if (durability != -1) {
                                        countedThing = countedThing + ":" + durability;
                                    }

                                    String currentCount =
                                            org.bukkit.ChatColor.GREEN + countedThing + ": "
                                                    + org.bukkit.ChatColor.AQUA + SkycadeMissionsPlugin.getInstance().getMissionManager().getType(mission.getType()).getCurrentCount(p.getUniqueId(), mission, countedThing)
                                                    + org.bukkit.ChatColor.RED + "/"
                                                    + org.bukkit.ChatColor.AQUA + amount;

                                    countingLore.add(currentCount);
                                }

                                lore.addAll(countingLore);

                                item.setLore(lore);

                                // Adds enchantment glow if mission is completed
                                MissionsUser user = MissionsUserManager.getInstance().get(p.getUniqueId());
                                if (user.hasPlayerCompleted(mission))
                                    item.addEnchantment(Enchantment.DURABILITY, 10).setItemFlags(ItemFlag.values());
                                return item.build();
                            },
                            (p, ev) -> {
                                new MissionVerifyAction(mission).checkComplete(p);
                            });

                    slot += 2;
                });

        setItemInteraction(26, new ItemBuilder(REWARDS).build(),
                (p, ev) -> {
                    new MissionRewardsGui().open(p);
                });
    }
}
