package net.skycade.skycademissions.missions;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.skycade.SkycadeCore.CoreSettings;
import net.skycade.SkycadeCore.utility.AsyncScheduler;
import net.skycade.skycademissions.SkycadeMissionsPlugin;
import net.skycade.skycademissions.missions.types.*;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.event.Listener;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public class MissionManager implements Listener {

    private static Map<Type, MissionType> types = new HashMap<>();

    private static TreeSet<Mission> missions = new TreeSet<>(Comparator.comparingInt(Mission::getPosition));

    private static TreeMap<MissionLevel, List<Reward>> rewards = new TreeMap<>();

    public MissionManager() {
        loadMissions();
        loadRewards();
    }

    public static void addMissions(MissionType... types) {
        for (MissionType type : types) {
            MissionManager.types.put(type.getType(), type);
        }
    }

    public static MissionType getType(Type type) {
        return types.getOrDefault(type, null);
    }

    public static Map<MissionLevel, List<Reward>> getRewards() {
        return rewards;
    }

    static List<Mission> getAllDaily() {
        return missions.stream().filter(Mission::isDaily).collect(Collectors.toList());
    }

    public static Mission getMissionFromName(String handle) {
        for (Mission mission : missions) {
            if (mission.getHandle().equals(handle)) {
                return mission;
            }
        }
        return null;
    }

    private static void loadMissions() {
        JsonParser jsonParser = new JsonParser();

        Bukkit.getScheduler().runTaskAsynchronously(SkycadeMissionsPlugin.getInstance(), () -> {
            try (Connection connection = CoreSettings.getInstance().getConnection()) {
                PreparedStatement statement = connection.prepareStatement("SELECT `mission`, `current`, `generatedon`, `dailymission`, `icon`, `displayname`, `description`, `params`, `level`, `type`, `position` FROM skycade_missions WHERE instance = ?");
                statement.setString(1, CoreSettings.getInstance().getThisInstance());
                ResultSet set = statement.executeQuery();

                while (set.next()) {
                    String handle = set.getString("mission");
                    boolean isCurrent = set.getBoolean("current");
                    long generatedOn = set.getLong("generatedon");
                    boolean isDaily = set.getBoolean("dailymission");
                    String icon = set.getString("icon");
                    String displayName = set.getString("displayname");
                    String lore = set.getString("description");
                    if (lore != null) lore = ChatColor.GRAY + lore;

                    List<String> missionLore;

                    if (lore != null) {
                        missionLore = new ArrayList<>(Arrays.asList(lore.split("\n")));
                    } else {
                        missionLore = new ArrayList<>();
                    }

                    String paramsJson = set.getString("params");
                    List<Map<?, ?>> params = new ArrayList<>();
                    for (Map.Entry<String, JsonElement> entry : jsonParser.parse(paramsJson).getAsJsonObject().entrySet()) {
                        JsonObject jsonParams = entry.getValue().getAsJsonObject();

                        String type;
                        try {
                            type = jsonParams.get("type").getAsString();
                        } catch (Exception e) { continue; }

                        int amount;
                        try {
                            amount = jsonParams.get("amount").getAsInt();
                        } catch (Exception e) { continue; }

                        short durability = -1;
                        try {
                            durability = jsonParams.get("durability").getAsShort();
                        } catch (Exception ignored) { }

                        Map<Object, Object> map = new HashMap<>();
                        map.put("type", type);
                        map.put("amount", amount);
                        if (durability != -1)
                            map.put("durability", durability);
                        params.add(map);
                    }

                    MissionLevel level = MissionLevel.valueOf(set.getString("level"));
                    Type type = Type.valueOf(set.getString("type"));
                    int position = set.getInt("position");

                    missions.add(new Mission(isDaily, isCurrent, generatedOn, type, handle, displayName, params, level, icon, missionLore, position));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }

            new DailyMissionManager();

            TypesManager.getInstance().loadCurrentCountableMissions();
        });
    }

    private static void loadRewards() {
        Bukkit.getScheduler().runTaskAsynchronously(SkycadeMissionsPlugin.getInstance(), () -> {
            try (Connection connection = CoreSettings.getInstance().getConnection()) {
                PreparedStatement statement = connection.prepareStatement("SELECT `level`, `name`, `commands` FROM skycade_missions_rewards WHERE instance = ?");
                statement.setString(1, CoreSettings.getInstance().getThisInstance());
                ResultSet set = statement.executeQuery();

                while (set.next()) {
                    MissionLevel level = MissionLevel.valueOf(set.getString("level"));
                    String name = set.getString("name");
                    String commands = set.getString("commands");
                    if (name != null) name = ChatColor.GRAY + name;

                    List<String> rewardDisplayName;

                    if (name != null) {
                        rewardDisplayName = new ArrayList<>(Arrays.asList(name.split("\n")));
                    } else {
                        rewardDisplayName = new ArrayList<>();
                    }

                    List<String> rewardCommands;

                    if (name != null) {
                        rewardCommands = new ArrayList<>(Arrays.asList(commands.split("\n")));
                    } else {
                        rewardCommands = new ArrayList<>();
                    }

                    Reward rewardData = new Reward(rewardDisplayName, rewardCommands);

                    if (rewards.containsKey(level)) {
                        rewards.get(level).add(rewardData);
                    } else {
                        rewards.put(level, new ArrayList<>(Collections.singleton(rewardData)));
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    public static void updateMissionsDatabase() {
        AsyncScheduler.runTask(SkycadeMissionsPlugin.getInstance(), () -> {

            try (Connection connection = CoreSettings.getInstance().getConnection()) {
                String sql = "INSERT INTO skycade_missions (`instance`, `mission`, `current`, `generatedon`, `dailymission`, `icon`, `displayname`, `description`, `params`, `level`, `type`, `position`) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                        "ON DUPLICATE KEY UPDATE current = VALUES(current), generatedon = VALUES(generatedon)";
                PreparedStatement statement = connection.prepareStatement(sql);
                missions.forEach((mission -> {
                    try {
                        statement.setString(1, CoreSettings.getInstance().getThisInstance());
                        statement.setString(2, mission.getHandle());
                        statement.setBoolean(3, mission.isCurrent());
                        statement.setLong(4, mission.getGeneratedOn());
                        statement.setBoolean(5, mission.isDaily());
                        statement.setString(6, mission.getIcon().name());
                        statement.setString(7, mission.getDisplayName());
                        StringBuilder loreString = new StringBuilder();
                        for (String line : mission.getLore()) {
                            if (line != null) {
                                loreString.append(line);
                            }
                        }
                        statement.setString(8, loreString.toString());
                        statement.setString(9, new Gson().toJson(mission.getParams()));
                        statement.setString(10, mission.getLevel().name());
                        statement.setString(11, mission.getType().name());
                        statement.setInt(12, mission.getPosition());

                        statement.addBatch();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }));
                statement.executeBatch();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }
}
