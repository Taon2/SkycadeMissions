package net.skycade.skycademissions.missions;

import net.skycade.skycademissions.SkycadeMissionsPlugin;
import net.skycade.skycademissions.missions.types.TypesListener;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static net.skycade.skycademissions.util.Messages.NEWDAILYMISSIONS;

public class DailyMissionManager extends BukkitRunnable {

    private static List<String> current = new ArrayList<>();
    private static long lastGenerated = 0L;

    private static DailyMissionManager instance;

    DailyMissionManager() {
        if (instance == null) {
            runTaskTimer(SkycadeMissionsPlugin.getInstance(), 1L, 1200L);
            instance = this;
        }
        run();
    }

    @Override
    public void run() {
        File file = new File(SkycadeMissionsPlugin.getInstance().getDataFolder(), "daily.yml");

        if (lastGenerated == 0L) {
            if (file.exists()) {
                YamlConfiguration conf = YamlConfiguration.loadConfiguration(file);
                lastGenerated = conf.getLong("generatedOn", 0);
                current = conf.getStringList("current");
            }
        }

        Calendar cal = Calendar.getInstance();
//        cal.set(Calendar.HOUR, 0);
//        cal.set(Calendar.MINUTE, 0);
//        cal.set(Calendar.SECOND, 0);

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

            YamlConfiguration config = new YamlConfiguration();
            config.set("generatedOn", System.currentTimeMillis());
            lastGenerated = System.currentTimeMillis();
            config.set("current", current);

            try {
                config.save(file);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            TypesListener.loadCurrentCountableMissions();

            NEWDAILYMISSIONS.broadcast();
            Bukkit.getLogger().info(ChatColor.translateAlternateColorCodes('&', NEWDAILYMISSIONS.toString()));
        }
    }

    public static List<String> getCurrent() {
        return current;
    }
}
