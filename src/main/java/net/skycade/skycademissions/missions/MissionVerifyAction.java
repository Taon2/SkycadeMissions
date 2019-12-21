package net.skycade.skycademissions.missions;

import net.skycade.skycademissions.MissionsUser;
import net.skycade.skycademissions.MissionsUserManager;
import net.skycade.skycademissions.SkycadeMissionsPlugin;
import net.skycade.skycademissions.missions.types.MissionType;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import static net.skycade.skycademissions.util.Messages.*;

public class MissionVerifyAction {

    private final Mission mission;

    public MissionVerifyAction(Mission mission) {
        this.mission = mission;
    }

    //Checks if the player should get the rewards or not
    public void checkComplete(Player p) {
        MissionType type = SkycadeMissionsPlugin.getInstance().getMissionManager().getType(mission.getType());
        MissionsUser user = MissionsUserManager.getInstance().get(p.getUniqueId());

        if (type == null) return;

        if (user.hasPlayerCompleted(mission)) {
            ALREADYCOMPLETED.msg(p, "%mission%", mission.getDisplayName());
            if (SkycadeMissionsPlugin.v18) {
                p.playSound(p.getLocation(), Sound.valueOf("ENDERMAN_TELEPORT"), 1f, 1f);
            } else {
                p.playSound(p.getLocation(), Sound.ENTITY_ENDERMEN_TELEPORT, 1f, 1f);
            }
            return;
        }

        Result result = type.validate(p, mission.getParams(), mission);
        if (result.asBoolean()) {
            if (SkycadeMissionsPlugin.v18) {
                p.playSound(p.getLocation(), Sound.valueOf("LEVEL_UP"), 1f, 1f);
            } else {
                p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
            }
            if (mission.isDaily()) {
                giveRewards(p);
            }

            type.postComplete(p, mission.getParams());
            user.completeMission(mission);
            checkAllThree(p);
            p.getOpenInventory().close();
        } else {
            if (result.getMessage() != null) p.sendMessage(result.getMessage());
            if (SkycadeMissionsPlugin.v18) {
                p.playSound(p.getLocation(), Sound.valueOf("ENDERMAN_TELEPORT"), 1f, 1f);
            } else {
                p.playSound(p.getLocation(), Sound.ENTITY_ENDERMEN_TELEPORT, 1f, 1f);
            }
            return;
        }

        MissionsUserManager.getInstance().updateCompletedDatabase(user);
        MissionsUserManager.getInstance().updateCountsDatabase(user);
    }

    //Grants rewards to the player
    private void giveRewards(Player p) {
        COMPLETEMISSION.msg(p, "%mission%", mission.getDisplayName());

        Map<MissionLevel, List<Reward>> rewards = SkycadeMissionsPlugin.getInstance().getMissionManager().getRewards();
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
        List<Mission> daily = DailyMissionManager.getInstance().getCurrent();
        MissionsUser user = MissionsUserManager.getInstance().get(p.getUniqueId());

        for (Mission mission : daily) {
            if (mission != null && !user.hasPlayerCompleted(mission)) {
                return;
            }
        }

        Map<MissionLevel, List<Reward>> rewards = SkycadeMissionsPlugin.getInstance().getMissionManager().getRewards();
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
