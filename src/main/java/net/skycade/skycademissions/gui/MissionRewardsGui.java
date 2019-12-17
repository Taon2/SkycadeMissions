package net.skycade.skycademissions.gui;

import net.md_5.bungee.api.ChatColor;
import net.skycade.SkycadeCore.guis.dynamicnew.DynamicGui;
import net.skycade.SkycadeCore.utility.ItemBuilder;
import net.skycade.skycademissions.missions.MissionManager;
import net.skycade.skycademissions.missions.Reward;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class MissionRewardsGui extends DynamicGui {

    private static final ItemStack BACK = new ItemBuilder(Material.ARROW)
            .setDisplayName(ChatColor.YELLOW + "" + ChatColor.BOLD + "Go Back")
            .build();

    private int slot = 10;

    public MissionRewardsGui() {
        super(ChatColor.AQUA + "" + ChatColor.BOLD + "Rewards", 3);

        MissionManager.getRewards()
                .forEach((missionLevel, rewardsList) -> {
                    setItem(slot, p -> {
                        ItemBuilder b = new ItemBuilder(new ItemStack(Material.NETHER_STAR, 1));

                        b.setDisplayName(org.bukkit.ChatColor.RED + "" + org.bukkit.ChatColor.BOLD + missionLevel.name().toUpperCase() + " Rewards");

                        // Constructs lore of all rewards for this mission type
                        List<String> lore = new ArrayList<>();
                        for (Reward reward : rewardsList) {
                            List<String> rewardNames = reward.getNames();
                            lore.addAll(rewardNames);
                            lore.add(org.bukkit.ChatColor.GOLD + "-");
                        }

                        b.setLore(lore);

                        return b.build();
                    });

                    slot += 2;
                });

        setItemInteraction(18, new ItemBuilder(BACK).build(),
                (p, ev) -> {
                    new MissionGui().open(p);
                });
    }
}
