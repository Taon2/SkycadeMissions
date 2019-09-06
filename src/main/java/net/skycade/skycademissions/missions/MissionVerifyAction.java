package net.skycade.skycademissions.missions;

import net.skycade.skycademissions.MissionsUser;
import net.skycade.skycademissions.missions.types.MissionType;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;
import java.util.Map;
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
        MissionsUser user = MissionsUser.get(p.getUniqueId());

        if (type == null) return;

        if (user.hasPlayerCompleted(mission)) {
            ALREADYCOMPLETED.msg(p, "%mission%", mission.getDisplayName());
            return;
        }

        Result result = type.validate(p, mission.getParams(), mission);
        if (result.asBoolean()) {
            if (mission.isDaily()) {
                giveRewards(p);
            }

            type.postComplete(p, mission.getParams());
            user.completeMission(mission);
            checkAllThree(p);
            p.getOpenInventory().close();
        } else {
            if (result.getMessage() != null) p.sendMessage(result.getMessage());
        }

        user.updateCountsDatabase();
        user.updateCompletedDatabase();
    }

    private void giveRewards(Player p) {
        COMPLETEMISSION.msg(p, "%mission%", mission.getDisplayName());

        Map<MissionLevel, List<Reward>> rewards = MissionManager.getRewards();
        List<Reward> missionRewards = rewards.get(mission.getLevel());

        int num = missionRewards.size();
        Reward reward = missionRewards.get(ThreadLocalRandom.current().nextInt(num));

        List<String> commands = reward.getCommands();
        commands.forEach(e -> Bukkit.getServer().dispatchCommand(
                Bukkit.getServer().getConsoleSender(),
                e.replace("%player%", p.getName())
        ));
        List<String> names = reward.getNames();
        REWARDWON.msg(p, "%reward%", names.toString().replace("[","").replace("]",""));
    }

    //If the player has completed all 3 missions for that day, then give them an additional bigger reward
    private void checkAllThree(Player p) {
        List<String> daily = DailyMissionManager.getInstance().getCurrent();
        MissionsUser user = MissionsUser.get(p.getUniqueId());

        for (String name : daily) {
            Mission mission = MissionManager.getMissionFromName(name);
            if (mission != null && !user.hasPlayerCompleted(mission)) {
                return;
            }
        }

        Map<MissionLevel, List<Reward>> rewards = MissionManager.getRewards();
        List<Reward> missionRewards = rewards.get(MissionLevel.ALLTHREE);

        if (missionRewards == null) {
            return;
        }

        int num = missionRewards.size();
        Reward reward = missionRewards.get(ThreadLocalRandom.current().nextInt(num));

        List<String> commands = reward.getCommands();
        commands.forEach(e -> Bukkit.getServer().dispatchCommand(
                Bukkit.getServer().getConsoleSender(),
                e.replace("%player%", p.getName())
        ));
        List<String> names = reward.getNames();
        ALLTHREECOMPLETED.msg(p, "%reward%", names.toString().replace("[","").replace("]",""));
    }
}
