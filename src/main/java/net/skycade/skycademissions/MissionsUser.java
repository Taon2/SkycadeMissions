package net.skycade.skycademissions;

import com.google.common.collect.Maps;
import net.skycade.SkycadeCore.CoreSettings;
import net.skycade.SkycadeCore.utility.AsyncScheduler;
import net.skycade.skycademissions.missions.DailyMissionManager;
import net.skycade.skycademissions.missions.Mission;
import net.skycade.skycademissions.missions.MissionManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class MissionsUser {

    private static Map<UUID, MissionsUser> users = Maps.newHashMap();

    public static MissionsUser get(UUID uuid) {
        return users.getOrDefault(uuid, null);
    }

    public static void remove(UUID uuid) {
        users.remove(uuid);
    }

    public static Map<UUID, MissionsUser> getUsers() {
        return users;
    }

    public static void add(UUID uuid, MissionsUser user) {
        users.put(uuid, user);
    }

    private Player player;
    private Map<String, List<Count>> counts = new HashMap<>();
    private Map<String, Long> completed = new HashMap<>();

    private List<UUID> hitWithSnowball = new ArrayList<>();

    public MissionsUser(Player player) {
        this.player = player;

        load();
    }

    public Player getPlayer() {
        return player;
    }

    public Map<String, List<Count>> getCounts() {
        return counts;
    }

    public Map<String, Long> getCompleted() {
        return completed;
    }

    public List<UUID> getHitWithSnowball() {
        return hitWithSnowball;
    }

    public void addHitWithSnowball(UUID uuid) {
        hitWithSnowball.add(uuid);
    }

    private void load() {
        Bukkit.getScheduler().runTaskAsynchronously(SkycadeMissionsPlugin.getInstance(), () -> {
            try (Connection connection = CoreSettings.getInstance().getConnection()) {
                PreparedStatement statement = connection.prepareStatement("SELECT `mission`, `counted`, `timestamp`, `count` FROM skycade_missions_counters WHERE uuid = ? AND instance = ? AND (season IS NULL OR season = ?)");
                statement.setString(1, player.getUniqueId().toString());
                statement.setString(2, CoreSettings.getInstance().getThisInstance());
                statement.setString(3, CoreSettings.getInstance().getSeason());
                ResultSet set = statement.executeQuery();

                String missionHandle;
                while (set.next()) {
                    missionHandle = set.getString("mission");

                    //checks if the mission exists, and if the mission is current
                    if (missionHandle == null || MissionManager.getMissionFromName(missionHandle) == null || !DailyMissionManager.getCurrent().contains(missionHandle)) continue;
                    String counted = set.getString("counted");
                    long timestamp = set.getLong("timestamp");
                    int count = set.getInt("count");

                    //checks if the count is current
                    Calendar cal = Calendar.getInstance();
                    long today = cal.getTimeInMillis();

                    if (timestamp < today) {
                        count = 0;
                    }

                    Count countData = new Count(counted, count);

                    if (counts.containsKey(missionHandle)) {
                        counts.get(missionHandle).add(countData);
                    } else {
                        counts.put(missionHandle, new ArrayList<>(Collections.singleton(countData)));
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });

        Bukkit.getScheduler().runTaskAsynchronously(SkycadeMissionsPlugin.getInstance(), () -> {
            try (Connection connection = CoreSettings.getInstance().getConnection()) {
                PreparedStatement statement = connection.prepareStatement("SELECT `mission`, `timestamp` FROM skycade_missions_completed WHERE uuid = ? AND instance = ? AND (season IS NULL OR season = ?)");
                statement.setString(1, player.getUniqueId().toString());
                statement.setString(2, CoreSettings.getInstance().getThisInstance());
                statement.setString(3, CoreSettings.getInstance().getSeason());
                ResultSet set = statement.executeQuery();
                while (set.next()) {
                    String missionHandle = set.getString("mission");
                    long timestamp = set.getLong("timestamp");

                    completed.put(missionHandle, timestamp);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    public void addCounter(Mission mission, String counted, int count) {
        boolean doesCountExist = false;
        for (Count missionCount : counts.get(mission.getHandle())) {
            if (!missionCount.getCounted().equals(counted)) continue;
            doesCountExist = true;
        }

        //Checks to see if there is an active counter within the last 24 hours
        if (!doesCountExist && !hasPlayerCompleted(mission)) {
            //Starts a new counter if there is not an active counter and the mission hasn't been completed
            counts.get(mission.getHandle()).add(new Count(counted, count));
        } else if (!hasPlayerCompleted(mission)){
            //Increments the existing counter as long as the mission hasn't been completed
            for (Count missionCount : counts.get(mission.getHandle())) {
                if (!missionCount.getCounted().equals(counted)) continue;
                missionCount.setCount(count);
            }
        }
    }

    public void addLongCounter(Mission mission, String counted, long count) {
        boolean doesCountExist = false;
        for (Count missionCount : counts.get(mission.getHandle())) {
            if (!missionCount.getCounted().equals(counted)) continue;
            doesCountExist = true;
        }

        //Checks to see if there is an active counter within the last 24 hours
        if (!doesCountExist && !hasPlayerCompleted(mission)) {
            //Starts a new counter if there is not an active counter and the mission hasn't been completed
            counts.get(mission.getHandle()).add(new Count(counted, count));
        } else if (!hasPlayerCompleted(mission)){
            //Increments the existing counter as long as the mission hasn't been completed
            for (Count missionCount : counts.get(mission.getHandle())) {
                if (!missionCount.getCounted().equals(counted)) continue;
                missionCount.setLongCount(count);
            }
        }
    }

    public int getCurrentCount(Mission mission, String counted) {
        List<Map<?, ?>> section = mission.getParams();
        int currentCount = 0;

        for (Map<?, ?> s : section) {
            Object type = s.getOrDefault("type", null);
            if (type == null) continue;
            String countedThing = type.toString();

            if (!countedThing.equals(counted)) continue;

            boolean doesCountExist = false;
            if (counts.containsKey(mission.getHandle())) {
                for (Count missionCount : counts.get(mission.getHandle())) {
                    if (!missionCount.getCounted().equals(counted)) continue;
                    doesCountExist = true;
                }
            }

            //Checks to see if there is an active counter within the last 24 hours
            if (hasPlayerCompleted(mission)) {
                //Returns max value if already completed
                int amount = 1;
                Object obj = s.getOrDefault("amount", null);
                if (obj != null) amount = (Integer) obj;

                return amount;
            } else if (!doesCountExist && !hasPlayerCompleted(mission)) {
                //Starts a new counter if there is not an active counter and the mission hasn't been completed
                counts.computeIfAbsent(mission.getHandle(), k -> new ArrayList<>());
                counts.get(mission.getHandle()).add(new Count(counted, currentCount));
            } else {
                //Returns the existing counter
                for (Count missionCount : counts.get(mission.getHandle())) {
                    if (!missionCount.getCounted().equals(counted)) continue;
                    currentCount = missionCount.getCount();
                }
            }
        }

        return currentCount;
    }

    public long getCurrentLongCount(Mission mission, String counted) {
        List<Map<?, ?>> section = mission.getParams();
        long currentCount = 0;

        for (Map<?, ?> s : section) {
            Object type = s.getOrDefault("type", null);
            if (type == null) continue;
            String countedThing = type.toString();

            if (!countedThing.equals(counted)) continue;

            boolean doesCountExist = false;
            if (counts.containsKey(mission.getHandle())) {
                for (Count missionCount : counts.get(mission.getHandle())) {
                    if (!missionCount.getCounted().equals(counted)) continue;
                    doesCountExist = true;
                }
            }

            //Checks to see if there is an active counter within the last 24 hours
            if (hasPlayerCompleted(mission)) {
                //Returns max value if already completed
                int amount = 1;
                Object obj = s.getOrDefault("amount", null);
                if (obj != null) amount = (Integer) obj;

                return amount;
            } else if (!doesCountExist && !hasPlayerCompleted(mission)) {
                //Starts a new counter if there is not an active counter and the mission hasn't been completed
                counts.computeIfAbsent(mission.getHandle(), k -> new ArrayList<>());
                counts.get(mission.getHandle()).add(new Count(counted, currentCount));
            } else {
                //Returns the existing counter
                for (Count missionCount : counts.get(mission.getHandle())) {
                    if (!missionCount.getCounted().equals(counted)) continue;
                    currentCount = missionCount.getLongCount();
                }
            }
        }

        return currentCount;
    }

    public boolean hasPlayerCompleted(Mission mission) {
        if (!completed.containsKey(mission.getHandle())) return false;

        long timestamp = completed.get(mission.getHandle());

        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("Europe/London"));
        calendar.set(Calendar.HOUR, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);

        long timeInMillis = calendar.getTimeInMillis();
        return timestamp > timeInMillis;
    }

    public void completeMission(Mission mission) {
        completed.put(mission.getHandle(), System.currentTimeMillis());
    }

    public void updateCountsDatabase() {
        counts.forEach((missionHandle, missionCounts) -> missionCounts.forEach(count -> {
            AsyncScheduler.runTask(SkycadeMissionsPlugin.getInstance(), () -> {

                try (Connection connection = CoreSettings.getInstance().getConnection()) {
                    String sql = "INSERT INTO skycade_missions_counters (`uuid`, `mission`, `counted`, `timestamp`, `count`, `instance`, `season`) VALUES (?, ?, ?, ?, ?, ?, ?) " +
                            "ON DUPLICATE KEY UPDATE count = VALUES(count)";
                    PreparedStatement statement = connection.prepareStatement(sql);
                    try {
                        statement.setString(1, player.getUniqueId().toString());
                        statement.setString(2, missionHandle);
                        statement.setString(3, count.getCounted());
                        statement.setLong(4, count.getTimestamp());
                        statement.setInt(5, count.getCount());
                        statement.setString(6, CoreSettings.getInstance().getThisInstance());
                        statement.setString(7, CoreSettings.getInstance().getSeason());
                        statement.addBatch();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                    statement.executeBatch();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            });
        }));
    }

    public void updateCompletedDatabase() {
        completed.forEach((missionHandle, timestamp) -> {
            AsyncScheduler.runTask(SkycadeMissionsPlugin.getInstance(), () -> {

                try (Connection connection = CoreSettings.getInstance().getConnection()) {
                    String sql = "INSERT INTO skycade_missions_completed (`uuid`, `mission`, `timestamp`, `instance`, `season`) VALUES (?, ?, ?, ?, ?) " +
                            "ON DUPLICATE KEY UPDATE timestamp = VALUES(timestamp)";
                    PreparedStatement statement = connection.prepareStatement(sql);
                    try {
                        statement.setString(1, player.getUniqueId().toString());
                        statement.setString(2, missionHandle);
                        statement.setLong(3, timestamp);
                        statement.setString(4, CoreSettings.getInstance().getThisInstance());
                        statement.setString(5, CoreSettings.getInstance().getSeason());
                        statement.addBatch();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                    statement.executeBatch();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            });
        });
    }

    public static class Count {

        private String counted;
        private int count;
        private long timestamp;
        private long longCount;

        Count(String counted, int count) {
            this.counted = counted;
            this.timestamp = DailyMissionManager.getLastGenerated() + 86400000;
            this.count = count;
        }

        Count(String counted, long longCount) {
            this.counted = counted;
            this.longCount = longCount;
        }

        String getCounted() {
            return counted;
        }

        int getCount() {
            return count;
        }

        void setCount(int count) {
            this.count = count;
        }

        long getLongCount() {
            return longCount;
        }

        void setLongCount(long longCount) {
            this.longCount = longCount;
        }

        public long getTimestamp() {
            return timestamp;
        }
    }
}

