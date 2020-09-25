package net.skycade.skycademissions.missions;

import net.skycade.skycademissions.SkycadeMissionsPlugin;
import net.skycade.skycademissions.events.MissionsRefreshEvent;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static net.skycade.skycademissions.util.Messages.NEWDAILYMISSIONS;

public class DailyMissionManager extends BukkitRunnable {

    private static List<Mission> current = new ArrayList<>();
    private static long lastGenerated = 0L;

    private static DailyMissionManager instance;

    public DailyMissionManager() {
        if (instance == null) instance = this;

        //Only refresh on servers that have that to true (the spawn server rather than island01 for example), but load when initialized
        if (SkycadeMissionsPlugin.getInstance().getConfig() != null && !SkycadeMissionsPlugin.getInstance().getConfig().getBoolean("refresh-missions")) {
            if (lastGenerated == 0L) {
                if (SkycadeMissionsPlugin.getInstance().getMissionManager() == null ||
                        SkycadeMissionsPlugin.getInstance().getMissionManager().getAllDaily() == null ||
                        SkycadeMissionsPlugin.getInstance().getMissionManager().getAllDaily().size() <= 0) {
                    return;
                }

                SkycadeMissionsPlugin.getInstance().getMissionManager().getAllDaily().forEach(mission -> {
                    if (mission.getGeneratedOn() != 0 && mission.isCurrent()) {
                        lastGenerated = mission.getGeneratedOn();
                        current.add(mission);
                    }
                });
            }

            return;
        }

        run();
        runTaskTimer(SkycadeMissionsPlugin.getInstance(), 0L, 1200L);
    }

    @Override
    public void run() {
        if (lastGenerated == 0L) {
            if (SkycadeMissionsPlugin.getInstance().getMissionManager() == null ||
                    SkycadeMissionsPlugin.getInstance().getMissionManager().getAllDaily() == null ||
                    SkycadeMissionsPlugin.getInstance().getMissionManager().getAllDaily().size() <= 0) {
                return;
            }

            SkycadeMissionsPlugin.getInstance().getMissionManager().getAllDaily().forEach(mission -> {
                if (mission.getGeneratedOn() != 0 && mission.isCurrent()) {
                    lastGenerated = mission.getGeneratedOn();
                    current.add(mission);
                }
            });
        }

        Calendar cal = Calendar.getInstance();
        long today = cal.getTimeInMillis();

        if (lastGenerated + 86400000 < today) {
            // generate, persist
            List<Mission> daily = SkycadeMissionsPlugin.getInstance().getMissionManager().getAllDaily();
            List<Mission> oldMissions = new ArrayList<>(current);

            current.clear();

            for (int i = 0; i < 3 && i < daily.size(); ++i) {
                Mission mission;
                do {
                    int num = ThreadLocalRandom.current().nextInt(daily.size());
                    mission = daily.get(num);
                } while (current.contains(mission) || oldMissions.contains(mission));

                current.add(mission);
            }

            for (Mission mission : current) {
                if (mission != null) {
                    mission.setGeneratedOn(System.currentTimeMillis());
                    mission.setCurrent(true);
                }
            }

            for (Mission mission : oldMissions) {
                if (mission != null) {
                    mission.setGeneratedOn(0);
                    mission.setCurrent(false);
                }
            }

            lastGenerated = System.currentTimeMillis();
            SkycadeMissionsPlugin.getInstance().getMissionManager().updateMissionsDatabase();

            SkycadeMissionsPlugin.getInstance().getTypesManager().loadCurrentCountableMissions();

            NEWDAILYMISSIONS.broadcast();
        }

        MissionsRefreshEvent missionsRefreshEvent = new MissionsRefreshEvent(current);
        Bukkit.getServer().getPluginManager().callEvent(missionsRefreshEvent);
    }

    public List<Mission> getCurrent() {
        return current;
    }

    public static DailyMissionManager getInstance() {
        return instance;
    }

    public void setCurrent(List<Mission> newCurrent) {
        current = newCurrent;
    }

    public long getLastGenerated() { return lastGenerated;}
}
