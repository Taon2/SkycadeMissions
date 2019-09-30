package net.skycade.skycademissions.missions;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.skycade.SkycadeCore.CoreSettings;
import net.skycade.SkycadeCore.guis.dynamicnew.DynamicGui;
import net.skycade.SkycadeCore.utility.AsyncScheduler;
import net.skycade.SkycadeCore.utility.ItemBuilder;
import net.skycade.skycademissions.MissionsUser;
import net.skycade.skycademissions.SkycadeMissionsPlugin;
import net.skycade.skycademissions.missions.types.*;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public class MissionManager implements Listener {

    private static SkycadeMissionsPlugin plugin = SkycadeMissionsPlugin.getInstance();

    private static Map<Type, MissionType> types = new HashMap<>();

    private static TreeSet<Mission> missions = new TreeSet<>(Comparator.comparingInt(Mission::getPosition));

    private static Map<MissionLevel, List<Reward>> rewards = new HashMap<>();

    public static void addMissions(MissionType... types) {
        for (MissionType type : types) {
            MissionManager.types.put(type.getType(), type);
        }
    }

    public static void loadMissions() {
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
                    if (lore != null) lore = ChatColor.translateAlternateColorCodes('&', lore);

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
            Bukkit.getPluginManager().registerEvents(new TypesListener(), plugin);
        });

        loadRewards();

        new LandType();
        new InventoryType();
        new DamageType();
        new KillType();
        new MiningType();
        new ShopType();
        new EnchantType();
        new GenerateType();
        new SwindleType();
        new SnowballGunType();
        new FishingType();
        new LevelType();
        new PlaytimeType();
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
                    if (name != null) name = ChatColor.translateAlternateColorCodes('&', name);

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

                    Reward rewardData = new Reward(level, rewardDisplayName, rewardCommands);

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

    public static void openMissionGui(Player player) {
        openMissionGui(player, 1);
    }

    private static void openMissionGui(Player player, int page) {
        DynamicGui missionsGui = new DynamicGui(ChatColor.translateAlternateColorCodes('&', "&c&lMissions"), 3);
        MissionsUser user = MissionsUser.get(player.getUniqueId());

        int x = 0;

        if (DailyMissionManager.getInstance().getCurrent().size() <= 0) return;

        List<String> daily = DailyMissionManager.getInstance().getCurrent();

        int guiSlot = 11;
        for (Mission mission : missions) {

            if (mission.isDaily() && !daily.contains(mission.getHandle())) continue;

            ++x;
            if (x <= 18 * (page - 1)) continue;
            if (x > 18 * page) break;
            ItemStack item;

            List<Map<?, ?>> params = mission.getParams();
            String currentCount;
            ArrayList<String> countingLore = new ArrayList<>();

            for (Map<?, ?> s : params) {
                Object type = s.getOrDefault("type", null);
                if (type == null) continue;
                String countedThing = type.toString();

                int amount = 1;
                Object obj = s.getOrDefault("amount", null);
                if (obj != null) amount = (Integer) obj;

                short durability = -1;
                obj = s.getOrDefault("durability", null);
                if (obj != null) durability = ((Short) obj);

                if (durability != -1) {
                    countedThing = countedThing + ":" + durability;
                }

                currentCount = ChatColor.GREEN + countedThing + ": " + ChatColor.AQUA + MissionManager.getType(mission.getType()).getCurrentCount(player.getUniqueId(), mission, countedThing) + (ChatColor.RED + "/") + ChatColor.AQUA + amount;
                countingLore.add(currentCount);
            }

            List <String> lore = new ArrayList<>();

            lore.add(ChatColor.RED + "" + ChatColor.BOLD + mission.getLevel().toString());
            lore.addAll(mission.getLore());
            lore.addAll(countingLore);

            if (mission.getIcon() == Material.INK_SACK || mission.getIcon() == Material.WOOL || mission.getIcon() == Material.LOG || mission.getIcon() == Material.RAW_FISH) {
                ItemBuilder b = new ItemBuilder(new ItemStack(mission.getIcon(), 1, mission.getDurability()))
                        .setDisplayName(ChatColor.AQUA + "" + ChatColor.BOLD + mission.getDisplayName())
                        .setLore(lore);

                if (user.hasPlayerCompleted(mission))
                    b = b.addEnchantment(Enchantment.DURABILITY, 10)
                            .setItemFlags(ItemFlag.values());

                item = b.build();
            } else {
                ItemBuilder b = new ItemBuilder(new ItemStack(mission.getIcon(), 1))
                        .setDisplayName(ChatColor.AQUA + "" + ChatColor.BOLD + mission.getDisplayName())
                        .setLore(lore);

                if (user.hasPlayerCompleted(mission))
                    b = b.addEnchantment(Enchantment.DURABILITY, 10)
                            .setItemFlags(ItemFlag.values());

                item = b.build();
            }

            missionsGui.setItemInteraction(guiSlot, item, new MissionVerifyAction(mission));
            guiSlot += 2;
        }

        ItemBuilder rewardsItem = new ItemBuilder(new ItemStack(Material.NETHER_STAR, 1))
                .setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + "Rewards")
                .setLore("Displays the rewards you can win.");
        missionsGui.setItem(26, rewardsItem.build());

        missionsGui.open(player);
    }

    private static void openRewardsGUI(Player player) {
        DynamicGui rewardsGui = new DynamicGui(ChatColor.translateAlternateColorCodes('&', "&c&lRewards"), 3);

        Map<MissionLevel, List<Reward>> rewards = MissionManager.getRewards();

        if (rewards.size() <= 0) return;

        int guiSlot = 10;
        for (MissionLevel levelKey : rewards.keySet()) {
            ItemStack item;
            ArrayList <String> lore = new ArrayList<>();

            for (Reward reward : rewards.get(levelKey)) {
                List<String> rewardNames = reward.getNames();
                lore.addAll(rewardNames);
                lore.add(ChatColor.GOLD + "-");
            }

            ItemBuilder b = new ItemBuilder(new ItemStack(Material.NETHER_STAR, 1))
                    .setDisplayName(ChatColor.RED + "" + ChatColor.BOLD + levelKey.name().toUpperCase() + " Rewards")
                    .setLore(lore);

            item = b.build();

            rewardsGui.setItem(guiSlot, item);
            guiSlot += 2;
        }

        ItemBuilder backItem = new ItemBuilder(new ItemStack(Material.ARROW, 1))
                .setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + "Go Back")
                .setLore("Return to the Missions GUI.");
        rewardsGui.setItem(26, backItem.build());

        rewardsGui.open(player);
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onInventoryClick(InventoryClickEvent e) {
        if (e.getClickedInventory() == null) return;

        if (e.getClickedInventory().getName().equals("§c§lMissions") && e.getSlot() == 26) {
            openRewardsGUI((Player) e.getWhoClicked());
        } else if (e.getClickedInventory().getName().equals("§c§lRewards") && e.getSlot() == 26) {
            openMissionGui((Player) e.getWhoClicked());
        } else if (e.getClickedInventory().getName().equals("§c§lRewards")){
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerLogin(PlayerJoinEvent e) {
        MissionsUser.add(e.getPlayer().getUniqueId(), new MissionsUser(e.getPlayer()));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerLogout(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        MissionsUser user = MissionsUser.get(p.getUniqueId());

        user.updateCountsDatabase();
        user.updateCompletedDatabase();
        MissionsUser.remove(e.getPlayer().getUniqueId());
    }

    public static MissionType getType(Type type) {
        return types.getOrDefault(type, null);
    }

    static Map<MissionLevel, List<Reward>> getRewards() {
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

    static void updateMissionsDatabase() {
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
