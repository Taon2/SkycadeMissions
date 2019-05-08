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
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class MissionManager {

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
                        // todo log to console
                        continue;
                    }

                    String requiredLevelString = mission.getString("requiredlevel", "EASY").toUpperCase();
                    MissionLevel level; // todo
                    try {
                        level = MissionLevel.valueOf(requiredLevelString);
                    } catch (IllegalArgumentException e) {
                        // todo
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

        Bukkit.getPluginManager().registerEvents(new TypesListener(), plugin);
    }

    private static void save() {
        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Couldn't save missions yaml file.", e);
        }
    }

    public static void openGui(Player player) {
        openGui(player, 1);
    }

    private static void openGui(Player player, int page) {
        DynamicGui gui = new DynamicGui(ChatColor.translateAlternateColorCodes('&', "&c&lMissions"), 3);

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
            String currentCount = "";
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

            gui.setItemInteraction(guiSlot, item, new MissionVerifyAction(mission));
            guiSlot += 2;
        }

        gui.open(player);
    }

    public static MissionType getType(Type type) {
        return types.getOrDefault(type, null);
    }

    static ConfigurationSection getRewards() {
        return rewards;
    }

    public static void addCounter(UUID uuid, Mission mission, int count) {
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
            conf.set(uuid.toString() + ".counters." + mission.getHandle() + ".count", count);
            conf.set(uuid.toString() + ".counters." + mission.getHandle() + ".activated", System.currentTimeMillis());
        } else if (!hasPlayerCompleted(uuid, mission)){
            //Increments the existing counter as long as the mission hasn't been completed
            conf.set(uuid.toString() + ".counters." + mission.getHandle() + ".count", count);
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
        conf.set(uuid.toString() + "." + "counters", null);
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
