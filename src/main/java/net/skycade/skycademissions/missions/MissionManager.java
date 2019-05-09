package net.skycade.skycademissions.missions;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import net.skycade.SkycadeCore.guis.dynamicnew.DynamicGui;
import net.skycade.SkycadeCore.utility.ItemBuilder;
import net.skycade.skycademissions.SkycadeMissionsPlugin;
import net.skycade.skycademissions.missions.types.*;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class MissionManager implements Listener {

    private static YamlConfiguration yaml;
    private static File file;

    private static SkycadeMissionsPlugin plugin = SkycadeMissionsPlugin.getInstance();

    private static Map<Type, MissionType> types = new HashMap<>();

    private static TreeSet<Mission> missions = new TreeSet<>(Comparator.comparingInt(Mission::getPosition));

    private static ConfigurationSection rewards;

    public static void addMissions(MissionType... types) {
        for (MissionType type : types) {
            MissionManager.types.put(type.getType(), type);
        }
    }

    private static final LoadingCache<UUID, Map<String, Long>> completedMissions = CacheBuilder.newBuilder()
            .build(new CacheLoader<UUID, Map<String, Long>>() {
                @Override
                public Map<String, Long> load(UUID uuid) {
                    Map<String, Long> map = new HashMap<>();

                    File file = new File(plugin.getDataFolder(), "completed.yml");
                    if (!file.exists()) return map;

                    YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                    ConfigurationSection section = config.getConfigurationSection(uuid.toString());
                    if (section != null)
                        for (String s : section.getKeys(false)) {
                            map.put(s, section.getLong(s));
                        }

                    return map;
                }
            });

    public static void load() {
        file = new File(SkycadeMissionsPlugin.getInstance().getDataFolder(), "missions.yml");

        if (!file.exists()) {
            yaml = new YamlConfiguration();
            save();
        } else {
            yaml = YamlConfiguration.loadConfiguration(file);
            save();
        }

        ConfigurationSection main = yaml.getConfigurationSection("missions");
        rewards = yaml.getConfigurationSection("rewards");

        if (main != null) {
            for (String handle : main.getKeys(false)) {
                try {
                    ConfigurationSection mission = main.getConfigurationSection(handle);

                    String typeString = mission.getString("type").toUpperCase();
                    Type type;
                    try {
                        type = Type.valueOf(typeString);
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().log(Level.WARNING, "Unknown MissionType for '" + handle + "'", e);
                        continue;
                    }

                    String missionLevel = mission.getString("level", "EASY").toUpperCase();
                    MissionLevel level;
                    try {
                        level = MissionLevel.valueOf(missionLevel);
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().log(Level.WARNING, "Unknown MissionLevel for '" + handle + "'", e);
                        continue;
                    }

                    ConfigurationSection params = mission.getConfigurationSection("params");

                    String displayName = mission.getString("displayname");
                    String icon = mission.getString("icon");
                    boolean isDaily = mission.getBoolean("dailymission");

                    List<String> lore = mission.getStringList("description").stream()
                            .map(e -> ChatColor.translateAlternateColorCodes('&', e))
                            .collect(Collectors.toList());

                    int position = mission.getInt("position");
                    long expiry = mission.getLong("expiry");

                    missions.add(new Mission(isDaily, type, handle, displayName, params, level, icon, lore, position, expiry));
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "Couldn't load mission '" + handle + "'", e);
                }
            }
        }

        new DailyMissionManager();

        new LandType();
        new InventoryType();
        new DamageType();
        new KillType();
        new MiningType();

        Bukkit.getPluginManager().registerEvents(new TypesListener(), plugin);
    }

    private static void save() {
        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Couldn't save missions yaml file.", e);
        }
    }

    public static void openMissionGui(Player player) {
        openMissionGui(player, 1);
    }

    private static void openMissionGui(Player player, int page) {
        DynamicGui missionsGui = new DynamicGui(ChatColor.translateAlternateColorCodes('&', "&c&lMissions"), 3);

        int x = 0;

        if (DailyMissionManager.getCurrent().size() <= 0) return;

        List<String> daily = DailyMissionManager.getCurrent();

        int guiSlot = 11;
        for (Mission mission : missions) {
            if (mission.isDaily() && !daily.contains(mission.getHandle())) continue;

            ++x;
            if (x <= 18 * (page - 1)) continue;
            if (x > 18 * page) break;
            ItemStack item;

            List<Map<?, ?>> section = mission.getParams().getMapList("items");
            String currentCount;
            ArrayList<String> countingLore = new ArrayList<>();

            for (Map<?, ?> s : section) {

                Object type = s.getOrDefault("type", null);
                if (type == null) continue;
                String countedThing = type.toString();

                int amount = 1;
                Object obj = s.getOrDefault("amount", null);
                if (obj != null) amount = (Integer) obj;

                currentCount = ChatColor.GREEN + countedThing + ": " + ChatColor.AQUA + MissionManager.getType(mission.getType()).getCurrentCount(player.getUniqueId(), mission, countedThing) + (ChatColor.RED + "/") + ChatColor.AQUA + amount;
                countingLore.add(currentCount);
            }

            ArrayList <String> lore = new ArrayList<>();

            lore.add(ChatColor.RED + "" + ChatColor.BOLD + mission.getLevel().toString());
            lore.addAll(mission.getLore());
            lore.addAll(countingLore);

            if (mission.getIcon() == Material.INK_SACK || mission.getIcon() == Material.WOOL || mission.getIcon() == Material.LOG) {
                ItemBuilder b = new ItemBuilder(new ItemStack(mission.getIcon(), 1, mission.getDurability()))
                        .setDisplayName(ChatColor.AQUA + "" + ChatColor.BOLD + mission.getDisplayName())
                        .setLore(lore);

                if (hasPlayerCompleted(player.getUniqueId(), mission))
                    b = b.addEnchantment(Enchantment.DURABILITY, 10)
                            .setItemFlags(ItemFlag.values());

                item = b.build();
            } else {
                ItemBuilder b = new ItemBuilder(new ItemStack(mission.getIcon(), 1))
                        .setDisplayName(ChatColor.AQUA + "" + ChatColor.BOLD + mission.getDisplayName())
                        .setLore(lore);

                if (hasPlayerCompleted(player.getUniqueId(), mission))
                    b = b.addEnchantment(Enchantment.DURABILITY, 10)
                            .setItemFlags(ItemFlag.values());

                item = b.build();
            }

            missionsGui.setItemInteraction(guiSlot, item, new MissionVerifyAction(mission));
            guiSlot += 2;
        }

        ItemBuilder rewardsItem = new ItemBuilder(new ItemStack(Material.NETHER_STAR, 1))
                .setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + "Rewards")
                .setLore("Displays the rewards possible to win.");
        missionsGui.setItem(26, rewardsItem.build());

        missionsGui.open(player);
    }

    private static void openRewardsGUI(Player player) {
        DynamicGui rewardsGui = new DynamicGui(ChatColor.translateAlternateColorCodes('&', "&c&lRewards"), 3);

        ConfigurationSection rewards = MissionManager.getRewards();

        if (rewards.getKeys(false).size() <= 0) return;

        int guiSlot = 10;
        for (String typeKey : rewards.getKeys(false)) {
            ItemStack item;
            ArrayList <String> lore = new ArrayList<>();

            for (String rewardKey : rewards.getConfigurationSection(typeKey).getKeys(false)) {
                List<String> rewardNames = rewards.getConfigurationSection(typeKey).getStringList(rewardKey + ".names").stream()
                        .map(e -> ChatColor.translateAlternateColorCodes('&', e))
                        .collect(Collectors.toList());
                lore.addAll(rewardNames);
                lore.add(ChatColor.GOLD + "-");
            }

            //Sets the name for allthreecompleted section to a nicer looking name
            if (typeKey.equals("allthreecompleted")){
                typeKey = "ALL THREE";
            }

            ItemBuilder b = new ItemBuilder(new ItemStack(Material.NETHER_STAR, 1))
                    .setDisplayName(ChatColor.RED + "" + ChatColor.BOLD + typeKey.toUpperCase() + " Rewards")
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

    public static void addCounter(UUID uuid, Mission mission, String path, int count) {
        File file = new File(plugin.getDataFolder(), "completed.yml");

        YamlConfiguration conf;

        if (!file.exists()) {
            conf = new YamlConfiguration();
        } else {
            conf = YamlConfiguration.loadConfiguration(file);
        }

        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("Europe/London"));
        calendar.set(Calendar.HOUR, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);

        long timeInMillis = calendar.getTimeInMillis();

        boolean doesCountExist = conf.contains(uuid.toString() + ".counters." + mission.getHandle());
        boolean isTimeEnabled = conf.getLong(uuid.toString() + ".counters." + mission.getHandle() + ".activated") > timeInMillis;

        //Checks to see if there is an active counter within the last 24 hours
        if ((!doesCountExist || !isTimeEnabled) && !hasPlayerCompleted(uuid, mission)) {
            //Starts a new counter if there is not an active counter and the mission hasn't been completed
            conf.set(uuid.toString() + ".counters." + mission.getHandle() + "." + path, count);
            conf.set(uuid.toString() + ".counters." + mission.getHandle() + ".activated", System.currentTimeMillis());
        } else if (!hasPlayerCompleted(uuid, mission)){
            //Increments the existing counter as long as the mission hasn't been completed
            conf.set(uuid.toString() + ".counters." + mission.getHandle() + "." + path, count);
        }

        try {
            conf.save(file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        completedMissions.invalidate(uuid);
    }

    static void completeMission(UUID uuid, Mission mission) {
        File file = new File(plugin.getDataFolder(), "completed.yml");

        YamlConfiguration conf;

        if (!file.exists()) {
            conf = new YamlConfiguration();
        } else {
            conf = YamlConfiguration.loadConfiguration(file);
        }
        conf.set(uuid.toString() + "." + "counters" + mission.getHandle(), null);
        conf.set(uuid.toString() + "." + mission.getHandle(), System.currentTimeMillis());

        try {
            conf.save(file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        completedMissions.invalidate(uuid);
    }

    public static boolean hasPlayerCompleted(UUID uuid, Mission mission) {
        Map<String, Long> map = completedMissions.getUnchecked(uuid);
        if (!map.containsKey(mission.getHandle())) return false;

        if (mission.isDaily()) {
            Long timestamp = map.get(mission.getHandle());

            Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("Europe/London"));
            calendar.set(Calendar.HOUR, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);

            long timeInMillis = calendar.getTimeInMillis();
            return timestamp > timeInMillis;
        } else {
            return true;
        }
    }

    public static MissionType getType(Type type) {
        return types.getOrDefault(type, null);
    }

    static ConfigurationSection getRewards() {
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
}
