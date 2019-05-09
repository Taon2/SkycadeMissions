package net.skycade.skycademissions.missions;

import net.skycade.SkycadeCore.utility.command.InventoryUtil;
import net.skycade.skycademissions.missions.types.MissionType;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiConsumer;

import static net.skycade.skycademissions.util.Messages.*;

public class MissionVerifyAction implements BiConsumer<Player, InventoryClickEvent> {

    private final Mission mission;

    MissionVerifyAction(Mission mission) {
        this.mission = mission;
    }

    @Override
    public void accept(Player p, InventoryClickEvent event) {
        MissionType type = MissionManager.getType(mission.getType());

        if (type == null) return;

        if (MissionManager.hasPlayerCompleted(p.getUniqueId(), mission)) {
            ALREADYCOMPLETED.msg(p, "%mission%", mission.getDisplayName());
            return;
        }

        Result result = type.validate(p, mission.getParams(), mission);
        if (result.asBoolean()) {
            if (mission.isDaily()) {
                giveRewards(p);
            } else {

                ConfigurationSection rewards = MissionManager.getRewards();

                List<?> items = rewards.getList("items");
                if (items != null && !items.isEmpty()) {
                    for (Object item : items) {
                        ItemStack reward = (ItemStack) item;
                        InventoryUtil.giveItems(p, reward);
                    }
                }

                int xp = rewards.getInt("xp", 0);
                if (xp > 0) p.giveExpLevels(xp);

                List<String> list = rewards.getStringList("consoleCommands");

                for (String s : list) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), s.replaceAll("%player%", p.getName())
                            .replaceAll("%uuid%", p.getUniqueId().toString()));
                }
            }

            type.postComplete(p, mission.getParams());
            MissionManager.completeMission(p.getUniqueId(), mission);
            checkAllThree(p);
            p.getOpenInventory().close();
        } else {
            if (result.getMessage() != null) p.sendMessage(result.getMessage());
        }
    }

    private void giveRewards(Player p) {
        ConfigurationSection rewards = MissionManager.getRewards();

        if (mission.getLevel() == MissionLevel.EASY){
            ConfigurationSection easyRewards = rewards.getConfigurationSection("easy");
            List<String> keys = new ArrayList<>(easyRewards.getKeys(false));
            int num = keys.size();
            String key = keys.get(ThreadLocalRandom.current().nextInt(num));

            List<String> commands = easyRewards.getConfigurationSection(key).getStringList("commands");
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

            List<String> commands = mediumRewards.getConfigurationSection(key).getStringList("commands");
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

            List<String> commands = hardRewards.getConfigurationSection(key).getStringList("commands");
            commands.forEach(e ->
                    Bukkit.getServer().dispatchCommand(
                            Bukkit.getServer().getConsoleSender(),
                            e.replace("%player%", p.getName())
                    )
            );
        }
        COMPLETEMISSION.msg(p, "%mission%", mission.getDisplayName());
    }

    //If the player has completed all 3 missions for that day, then give them an additional bigger reward
    private void checkAllThree(Player p) {
        List<String> daily = DailyMissionManager.getCurrent();

        for (String name : daily) {
            Mission mission = MissionManager.getMissionFromName(name);
            if (mission != null && !MissionManager.hasPlayerCompleted(p.getUniqueId(), mission)) {
                return;
            }
        }

        ConfigurationSection rewards = MissionManager.getRewards().getConfigurationSection("allthreecompleted");

        if (rewards == null) {
            return;
        }

        List<String> keys = new ArrayList<>(rewards.getKeys(false));
        int num = keys.size();
        String key = keys.get(ThreadLocalRandom.current().nextInt(num));

        List<String> commands = rewards.getConfigurationSection(key).getStringList("commands");
        commands.forEach(e ->
                Bukkit.getServer().dispatchCommand(
                        Bukkit.getServer().getConsoleSender(),
                        e.replace("%player%", p.getName())
                )
        );

        ALLTHREECOMPLETED.msg(p);
    }
}
