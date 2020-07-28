package net.skycade.skycademissions;

import com.google.common.collect.Maps;
import net.skycade.SkycadeCore.CoreSettings;
import net.skycade.SkycadeCore.utility.AsyncScheduler;
import net.skycade.skycademissions.missions.DailyMissionManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class MissionsUserManager {

    private static MissionsUserManager instance;

    private Map<UUID, MissionsUser> users = Maps.newHashMap();

    public MissionsUser get(UUID uuid) {
        return users.getOrDefault(uuid, null);
    }

    public void remove(UUID uuid) {
        users.remove(uuid);
    }

    public Map<UUID, MissionsUser> getUsers() {
        return users;
    }

    private void add(UUID uuid, MissionsUser user) {
        users.put(uuid, user);
    }

    public MissionsUserManager() {
        instance = this;
    }

    public void updateCountsDatabase(MissionsUser user) {
        if(user == null || user.getCounts() == null || user.getCounts().size() < 1)
            return; // no counted missions / null, so no need to insert into the DB. It saves time, processing power, and is easier on the server
        user.getCounts().forEach((missionHandle, missionCounts) -> missionCounts.forEach(count -> {
            AsyncScheduler.runTask(SkycadeMissionsPlugin.getInstance(), () -> {

                try (Connection connection = CoreSettings.getInstance().getConnection()) {
                    String sql = "INSERT INTO skycade_missions_counters (`uuid`, `mission`, `counted`, `timestamp`, `count`, `instance`, `season`) VALUES (?, ?, ?, ?, ?, ?, ?) " +
                            "ON DUPLICATE KEY UPDATE timestamp = VALUES(timestamp), count = VALUES(count)";
                    PreparedStatement statement = connection.prepareStatement(sql);
                    try {
                        statement.setString(1, user.getPlayer().getUniqueId().toString());
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

    public void updateCompletedDatabase(MissionsUser user) {
        if(user == null || user.getCompleted() == null || user.getCompleted().size() < 1)
            return; // no completed missions / null, so no need to insert into the DB. It saves time, processing power, and is easier on the server
        user.getCompleted().forEach((missionHandle, timestamp) -> {
            AsyncScheduler.runTask(SkycadeMissionsPlugin.getInstance(), () -> {

                try (Connection connection = CoreSettings.getInstance().getConnection()) {
                    String sql = "INSERT INTO skycade_missions_completed (`uuid`, `mission`, `timestamp`, `instance`, `season`) VALUES (?, ?, ?, ?, ?) " +
                            "ON DUPLICATE KEY UPDATE timestamp = VALUES(timestamp)";
                    PreparedStatement statement = connection.prepareStatement(sql);
                    try {
                        statement.setString(1, user.getPlayer().getUniqueId().toString());
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

    public void load(Player player) {
        Bukkit.getScheduler().runTaskAsynchronously(SkycadeMissionsPlugin.getInstance(), () -> {
            try (Connection connection = CoreSettings.getInstance().getConnection()) {
                PreparedStatement statement = connection.prepareStatement("SELECT `mission`, `counted`, `timestamp`, `count` FROM skycade_missions_counters WHERE uuid = ? AND instance = ? AND (season IS NULL OR season = ?)");
                statement.setString(1, player.getUniqueId().toString());
                statement.setString(2, CoreSettings.getInstance().getThisInstance());
                statement.setString(3, CoreSettings.getInstance().getSeason());
                ResultSet set = statement.executeQuery();

                Map<String, List<Count>> counts = new HashMap<>();

                while (set.next()) {
                    String missionHandle = set.getString("mission");

                    //checks if the mission exists, and if the mission is current
                    if (missionHandle == null || SkycadeMissionsPlugin.getInstance().getMissionManager().getMissionFromName(missionHandle) == null
                            || !DailyMissionManager.getInstance().getCurrent().contains(SkycadeMissionsPlugin.getInstance().getMissionManager().getMissionFromName(missionHandle))) continue;
                    String counted = set.getString("counted");
                    long timestamp = set.getLong("timestamp");
                    int count = set.getInt("count");

                    //checks if the count is current
                    Calendar cal = Calendar.getInstance();
                    long today = cal.getTimeInMillis();

                    if (timestamp < today) {
                        count = 0;
                    }

                    //loads counts
                    Count countData = new Count(counted, count);
                    counts.put(missionHandle, new ArrayList<>(Collections.singleton(countData)));
                }

                MissionsUser user = new MissionsUser(player, counts);
                add(player.getUniqueId(), user);
            } catch (SQLException e) {
                e.printStackTrace();
            }

            try (Connection connection = CoreSettings.getInstance().getConnection()) {
                PreparedStatement statement = connection.prepareStatement("SELECT `mission`, `timestamp` FROM skycade_missions_completed WHERE uuid = ? AND instance = ? AND (season IS NULL OR season = ?)");
                statement.setString(1, player.getUniqueId().toString());
                statement.setString(2, CoreSettings.getInstance().getThisInstance());
                statement.setString(3, CoreSettings.getInstance().getSeason());
                ResultSet set = statement.executeQuery();

                Map<String, Long> completed = new HashMap<>();

                while (set.next()) {
                    String missionHandle = set.getString("mission");
                    long timestamp = set.getLong("timestamp");

                    //loads completed missions
                    completed.put(missionHandle, timestamp);
                }

                MissionsUser missionsUser = get(player.getUniqueId());
                if (missionsUser != null) {
                    missionsUser.setCompleted(completed);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    public static MissionsUserManager getInstance() {
        if (instance == null)
            instance = new MissionsUserManager();
        return instance;
    }
}
