package net.skycade.skycademissions.missions;

import net.skycade.skycademissions.SkycadeMissionsPlugin;
import net.skycade.skycademissions.events.MissionsRefreshEvent;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

import static net.skycade.skycademissions.util.Messages.NEWDAILYMISSIONS;

public class DailyMissionManager extends BukkitRunnable {

    private static List<String> current = new ArrayList<>();
    private static long lastGenerated = 0L;

    private static DailyMissionManager instance;

    DailyMissionManager() {
        if (instance == null) instance = this;

        //Only refresh on servers that have that to true (the spawn server rather than island01 for example)
        if (SkycadeMissionsPlugin.getInstance().getConfig() != null && !SkycadeMissionsPlugin.getInstance().getConfig().getBoolean("refresh-missions")) return;

        run();
        runTaskTimer(SkycadeMissionsPlugin.getInstance(), 1L, 1200L);
    }

    @Override
    public void run() {
        if (lastGenerated == 0L) {
            MissionManager.getAllDaily().forEach(mission -> {
                if (mission.getGeneratedOn() != 0 && mission.isCurrent()) {
                    lastGenerated = mission.getGeneratedOn();
                    current.add(mission.getHandle());
                }
            });
        }

        Calendar cal = Calendar.getInstance();
        long today = cal.getTimeInMillis();

        if (lastGenerated + 86400000 < today) {
            // generate, persist
            List<Mission> daily = MissionManager.getAllDaily();
            List<String> oldMissions = new ArrayList<>(current);

            current.clear();

            for (int i = 0; i < 3 && i < daily.size(); ++i) {
                Mission mission;
                do {
                    int num = ThreadLocalRandom.current().nextInt(daily.size());
                    mission = daily.get(num);
                } while (current.contains(mission.getHandle()) || oldMissions.contains(mission.getHandle()));

                current.add(mission.getHandle());
            }

            current.stream().map(MissionManager::getMissionFromName).filter(Objects::nonNull).forEach(mission -> {
                mission.setGeneratedOn(System.currentTimeMillis());
                mission.setCurrent(true);
            });

            oldMissions.stream().map(MissionManager::getMissionFromName).filter(Objects::nonNull).forEach(mission -> {
                mission.setGeneratedOn(0);
                mission.setCurrent(false);
            });

            lastGenerated = System.currentTimeMillis();
            MissionManager.updateMissionsDatabase();

            NEWDAILYMISSIONS.broadcast();
        }

        MissionsRefreshEvent missionsRefreshEvent = new MissionsRefreshEvent(current);
        Bukkit.getServer().getPluginManager().callEvent(missionsRefreshEvent);
    }

    public static List<String> getCurrent() {
        return current;
    }

    public static DailyMissionManager getInstance() {
        return instance;
    }

    public void setCurrent(List<String> newCurrent) {
        current = newCurrent;
    }

    public static long getLastGenerated() { return lastGenerated;}
}
