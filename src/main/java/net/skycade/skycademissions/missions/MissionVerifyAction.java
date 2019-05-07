package net.skycade.skycademissions.missions;

import net.skycade.SkycadeCore.utility.command.InventoryUtil;
import net.skycade.skycademissions.missions.types.MissionType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiConsumer;

public class MissionVerifyAction implements BiConsumer<Player, InventoryClickEvent> {

    private final Mission mission;

    MissionVerifyAction(Mission mission) {
        this.mission = mission;
    }

    @Override
    public void accept(Player player, InventoryClickEvent event) {
        MissionType type = MissionManager.getType(mission.getType());

        if (type == null) return;

        if (MissionManager.hasPlayerCompleted(player.getUniqueId(), mission)) {
            player.sendMessage(ChatColor.RED + "You have already completed " + mission.getDisplayName() + "!");
            return;
        }

        Result result = type.validate(player, mission.getParams(), mission);
        if (result.asBoolean()) {
            if (mission.isDaily()) {
                giveRewards(player);
            } else {
                ConfigurationSection rewards = MissionManager.getRewards();

                List<?> items = rewards.getList("items");
                if (items != null && !items.isEmpty()) {
                    for (Object item : items) {
                        ItemStack reward = (ItemStack) item;
                        InventoryUtil.giveItems(player, reward);
                    }
                }

                int xp = rewards.getInt("xp", 0);
                if (xp > 0) player.giveExpLevels(xp);

                List<String> list = rewards.getStringList("consoleCommands");

                for (String s : list) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), s.replaceAll("%player%", player.getName())
                            .replaceAll("%uuid%", player.getUniqueId().toString()));
                }
            }

            type.postComplete(player, mission.getParams());
            MissionManager.completeMission(player.getUniqueId(), mission);
            player.getOpenInventory().close();
        } else {
            if (result.getMessage() != null) player.sendMessage(result.getMessage());
        }
    }

    private void giveRewards(Player p) {
        ConfigurationSection rewards = MissionManager.getRewards();

        if (mission.getLevel() == MissionLevel.EASY){
            ConfigurationSection easyRewards = rewards.getConfigurationSection("easy");
            List<String> keys = new ArrayList<>(easyRewards.getKeys(false));
            int num = keys.size();
            String key = keys.get(ThreadLocalRandom.current().nextInt(num));

            List<String> commands = easyRewards.getStringList(key);
            commands.forEach(e ->
                    Bukkit.getServer().dispatchCommand(
                            Bukkit.getServer().getConsoleSender(),
                            e.replace("%player%", p.getName())
                    )
            );
        }
        else if (mission.getLevel() == MissionLevel.MEDIUM){
            ConfigurationSection mediumRewards = rewards.getConfigurationSection("medium");
            List<String> keys = new ArrayList<>(mediumRewards.getKeys(false));
            int num = keys.size();
            String key = keys.get(ThreadLocalRandom.current().nextInt(num));

            List<String> commands = mediumRewards.getStringList(key);
            commands.forEach(e ->
                    Bukkit.getServer().dispatchCommand(
                            Bukkit.getServer().getConsoleSender(),
                            e.replace("%player%", p.getName())
                    )
            );
        }
        else if (mission.getLevel() == MissionLevel.HARD){
            ConfigurationSection hardRewards = rewards.getConfigurationSection("hard");
            List<String> keys = new ArrayList<>(hardRewards.getKeys(false));
            int num = keys.size();
            String key = keys.get(ThreadLocalRandom.current().nextInt(num));

            List<String> commands = hardRewards.getStringList(key);
            commands.forEach(e ->
                    Bukkit.getServer().dispatchCommand(
                            Bukkit.getServer().getConsoleSender(),
                            e.replace("%player%", p.getName())
                    )
            );
        }
        p.sendMessage(ChatColor.GOLD + "You have completed " + mission.getDisplayName() + "!");
    }
}
