package net.skycade.skycademissions.missions.types;

import net.skycade.SkycadeEnchants.enchant.common.Enchantment;
import net.skycade.SkycadeEnchants.events.SkycadeCustomEnchantItemEvent;
import net.skycade.SkycadeEnchants.events.SkycadeGenerateEnchantEvent;
import net.skycade.SkycadeEnchants.events.SkycadeSnowballGunEvent;
import net.skycade.SkycadeEnchants.events.SkycadeSwindlerEvent;
import net.skycade.prisons.util.EnchantmentTypes;
import net.skycade.skycademissions.SkycadeMissionsPlugin;
import net.skycade.skycademissions.missions.DailyMissionManager;
import net.skycade.skycademissions.missions.Mission;
import net.skycade.skycademissions.missions.MissionManager;
import net.skycade.skycadeshop.impl.skycade.event.PostSellTransactionEvent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.*;

public class TypesListener implements Listener {

    private static List<Mission> currentCountableMissions = new ArrayList<>();

    private static List<Type> countableTypes;

    static {
        countableTypes = Arrays.asList(
                Type.DAMAGE,
                Type.KILLS,
                Type.MINING,
                Type.SHOP,
                Type.ENCHANT,
                Type.GENERATE,
                Type.SWINDLE,
                Type.SNOWBALLGUN,
                Type.FISHING,
                Type.PLAYTIME
        );
    }

    public TypesListener() {
        for (String handle : DailyMissionManager.getCurrent()) {
            Mission mission = MissionManager.getMissionFromName(handle);
            if (mission != null && countableTypes.contains(mission.getType())) {
                currentCountableMissions.add(mission);
            }
        }

        Bukkit.getPluginManager().registerEvents(new SkycadeShopListener(), SkycadeMissionsPlugin.getInstance()); // don't suicide if shop plugin is not loaded
        Bukkit.getPluginManager().registerEvents(new SkycadeEnchantsListener(), SkycadeMissionsPlugin.getInstance()); // don't suicide if enchants plugin is not loaded
    }

    //Listener for the DamageType
    @EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL)
    public void onEntityDamage(EntityDamageByEntityEvent e) {
        for (Mission mission : currentCountableMissions) {
            if (mission.getType() == Type.DAMAGE) {
                List<Map<?, ?>> section = mission.getParams().getMapList("items");

                for (Map<?, ?> s : section) {
                    Object type = s.getOrDefault("type", null);
                    if (type == null) continue;
                    EntityType entityType = EntityType.valueOf(type.toString());

                    int amount = 1;
                    Object obj = s.getOrDefault("amount", null);
                    if (obj != null) amount = (Integer) obj;

                    if (e.getDamager() != null && e.getDamager().getType() == EntityType.PLAYER && e.getEntity().getType() == entityType) {
                        Player p = (Player) e.getDamager();
                        int count = amount;

                        if (MissionManager.getType(mission.getType()).getCurrentCount(p.getUniqueId(), mission, type.toString()) < amount) {
                            count = MissionManager.getType(mission.getType()).getCurrentCount(p.getUniqueId(), mission, type.toString()) + (int) e.getDamage();
                        }

                        MissionManager.addCounter(p.getUniqueId(), mission, e.getEntity().getType().toString(), count);
                    }
                }
            }
        }
    }

    //Listener for the KillType
    @EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL)
    public void onEntityDeath(EntityDeathEvent e) {
        for (Mission mission : currentCountableMissions) {
            if (mission.getType() == Type.KILLS) {
                List<Map<?, ?>> section = mission.getParams().getMapList("items");

                for (Map<?, ?> s : section) {
                    Object type = s.getOrDefault("type", null);
                    if (type == null) continue;
                    EntityType entityType = EntityType.valueOf(type.toString());

                    int amount = 1;
                    Object obj = s.getOrDefault("amount", null);
                    if (obj != null) amount = (Integer) obj;

                    if (e.getEntity().getKiller() != null && e.getEntity().getKiller().getType() == EntityType.PLAYER && e.getEntity().getType() == entityType) {
                        Player p = e.getEntity().getKiller();
                        int count = amount;

                        if (MissionManager.getType(mission.getType()).getCurrentCount(p.getUniqueId(), mission, entityType.toString()) < amount) {
                            count = MissionManager.getType(mission.getType()).getCurrentCount(p.getUniqueId(), mission, entityType.toString()) + 1;
                        }

                        MissionManager.addCounter(p.getUniqueId(), mission, entityType.toString(), count);
                    }
                }
            }
        }
    }

    //Listener for the MiningType
    @EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL)
    public void onBlockBreak(BlockBreakEvent e) {
        for (Mission mission : currentCountableMissions) {
            if (mission.getType() == Type.MINING) {
                List<Map<?, ?>> section = mission.getParams().getMapList("items");

                for (Map<?, ?> s : section) {
                    Object type = s.getOrDefault("type", null);
                    if (type == null) continue;

                    //Handles missions that count any block types
                    if (type.toString().equals("ANY")) {
                        int amount = 1;
                        Object obj = s.getOrDefault("amount", null);
                        if (obj != null) amount = (Integer) obj;

                        if (e.getPlayer() != null) {
                            Player p = e.getPlayer();
                            int count = amount;

                            if (MissionManager.getType(mission.getType()).getCurrentCount(p.getUniqueId(), mission, type.toString()) < amount) {
                                count = MissionManager.getType(mission.getType()).getCurrentCount(p.getUniqueId(), mission, type.toString()) + 1;
                            }

                            MissionManager.addCounter(p.getUniqueId(), mission, type.toString(), count);
                        }
                    }
                    //Handles missions that count specific block types
                    else {
                        Material materialType = Material.valueOf(type.toString());

                        int amount = 1;
                        Object obj = s.getOrDefault("amount", null);
                        if (obj != null) amount = (Integer) obj;

                        if (e.getPlayer() != null && e.getBlock().getType() == materialType) {
                            Player p = e.getPlayer();
                            int count = amount;

                            if (MissionManager.getType(mission.getType()).getCurrentCount(p.getUniqueId(), mission, materialType.toString()) < amount) {
                                count = MissionManager.getType(mission.getType()).getCurrentCount(p.getUniqueId(), mission, materialType.toString()) + 1;
                            }

                            MissionManager.addCounter(p.getUniqueId(), mission, materialType.toString(), count);
                        }
                    }
                }
            }
        }
    }

    //Listener for the FishingType
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onFishCatch(PlayerFishEvent e) {
        for (Mission mission : currentCountableMissions) {
            if (mission.getType() == Type.FISHING) {

                List<Map<?, ?>> section = mission.getParams().getMapList("items");

                for (Map<?, ?> s : section) {
                    Object type = s.getOrDefault("type", null);
                    if (type == null) continue;

                    //Handles missions that count any item types
                    if (type.toString().equals("ANY")) {
                        int amount = 1;
                        Object obj = s.getOrDefault("amount", null);
                        if (obj != null) amount = (Integer) obj;

                        if (e.getPlayer() != null && e.getCaught() != null && e.getCaught() instanceof Item) {
                            Player p = e.getPlayer();
                            int count = amount;

                            if (MissionManager.getType(mission.getType()).getCurrentCount(p.getUniqueId(), mission, type.toString()) < amount) {
                                count = MissionManager.getType(mission.getType()).getCurrentCount(p.getUniqueId(), mission, type.toString()) + 1;
                            }

                            MissionManager.addCounter(p.getUniqueId(), mission, type.toString(), count);
                        }
                    }
                    //Handles missions that count specific item types
                    else {
                        Material materialType = Material.valueOf(type.toString());

                        int amount = 1;
                        Object obj = s.getOrDefault("amount", null);
                        if (obj != null) amount = (Integer) obj;

                        short durability = 0;
                        obj = s.getOrDefault("durability", null);
                        if (obj != null) durability = ((Integer) obj).shortValue();

                        if (e.getPlayer() != null && e.getCaught() != null && e.getCaught() instanceof Item && ((Item) e.getCaught()).getItemStack().getType() == materialType && ((Item) e.getCaught()).getItemStack().getDurability() == durability) {
                            Player p = e.getPlayer();
                            int count = amount;

                            if (((Item) e.getCaught()).getItemStack().getDurability() == durability) {
                                if (MissionManager.getType(mission.getType()).getCurrentCount(p.getUniqueId(), mission, materialType.toString()) < amount) {
                                    count = MissionManager.getType(mission.getType()).getCurrentCount(p.getUniqueId(), mission, materialType.toString()) + 1;
                                }
                            }

                            MissionManager.addCounter(p.getUniqueId(), mission, materialType.toString() + ":" + durability, count);
                        }
                    }
                }
            }
        }
    }

    Map<UUID, Long> onlineMap = new HashMap<>();

    //Listener for the PlaytimeType
    @EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL)
    public void onPlayerLogin(PlayerLoginEvent event) {
        for (Mission mission : currentCountableMissions) {
            if (mission.getType() == Type.PLAYTIME) {
                YamlConfiguration conf = MissionManager.getCompletedConfig();

                List<Map<?, ?>> section = mission.getParams().getMapList("items");

                for (Map<?, ?> s : section) {
                    Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("Europe/London"));
                    calendar.set(Calendar.HOUR, 0);
                    calendar.set(Calendar.MINUTE, 0);
                    calendar.set(Calendar.SECOND, 0);

                    long timeInMillis = calendar.getTimeInMillis();

                    Object type = s.getOrDefault("type", null);
                    if (type == null) continue;

                    if (type.toString().equals("HOURS")) {
                        boolean doesCountExist = conf.contains(event.getPlayer().getUniqueId().toString() + ".counters." + mission.getHandle());
                        boolean isTimeEnabled = conf.getLong(event.getPlayer().getUniqueId().toString() + ".counters." + mission.getHandle() + ".activated") > timeInMillis;

                        if (!doesCountExist || !isTimeEnabled)
                            MissionManager.addCounter(event.getPlayer().getUniqueId(), mission, type.toString(), 0);

                        onlineMap.put(event.getPlayer().getUniqueId(), System.currentTimeMillis());
                    }
                }
                MissionManager.setCompletedConfig(conf);
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL)
    public void onPlayerLogout(PlayerQuitEvent event) {
        for (Mission mission : currentCountableMissions) {
            if (mission.getType() == Type.PLAYTIME) {

                List<Map<?, ?>> section = mission.getParams().getMapList("items");

                for (Map<?, ?> s : section) {
                    Object type = s.getOrDefault("type", null);
                    if (type == null) continue;

                    if (type.toString().equals("HOURS")) {
                        int amount = 1;
                        Object obj = s.getOrDefault("amount", null);
                        if (obj != null) amount = (Integer) obj;

                        if (event.getPlayer() != null) {
                            Player p = event.getPlayer();
                            long count = amount*3600000;

                            Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("Europe/London"));
                            calendar.set(Calendar.HOUR, 0);
                            calendar.set(Calendar.MINUTE, 0);
                            calendar.set(Calendar.SECOND, 0);

                            //Return if the timer hasn't started counting
                            if (!onlineMap.containsKey(p.getUniqueId())) return;

                            long startTime = onlineMap.get(p.getUniqueId());
                            long addedOnlineTime = System.currentTimeMillis() - startTime;

                            if ((MissionManager.getType(mission.getType()).getCurrentCount(p.getUniqueId(), mission, type.toString())*3600000) < amount) {
                                count = (MissionManager.getType(mission.getType()).getCurrentCount(p.getUniqueId(), mission, type.toString())*3600000) + addedOnlineTime;
                            }

                            MissionManager.addCounter(p.getUniqueId(), mission, type.toString(), count);
                            onlineMap.remove(p.getUniqueId());
                        }
                    }
                }
            }
        }
    }

    public class SkycadeShopListener implements Listener {

        //Listener for ShopType
        @EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL)
        public void onSkycadeShopSell(PostSellTransactionEvent event) {
            for (Mission mission : currentCountableMissions) {
                if (mission.getType() == Type.SHOP) {
                    List<Map<?, ?>> section = mission.getParams().getMapList("items");

                    for (Map<?, ?> s : section) {
                        Object type = s.getOrDefault("type", null);
                        if (type == null) continue;

                        //Handles missions that count any enchantments
                        if (type.toString().equals("$")) {
                            int amount = 1;
                            Object obj = s.getOrDefault("amount", null);
                            if (obj != null) amount = (Integer) obj;

                            if (event.getPlayer() != null) {
                                Player p = event.getPlayer();
                                int count = amount;

                                if (MissionManager.getType(mission.getType()).getCurrentCount(p.getUniqueId(), mission, type.toString()) < amount) {
                                    count = MissionManager.getType(mission.getType()).getCurrentCount(p.getUniqueId(), mission, type.toString()) + (int) event.getMoneyGained();
                                }

                                MissionManager.addCounter(p.getUniqueId(), mission, type.toString(), count);
                            }
                        }
                    }
                }
            }
        }
    }

    public class SkycadeEnchantsListener implements Listener {

        //Listener for the EnchantType
        @EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL)
        public void onSkycadeCustomEnchantItem(SkycadeCustomEnchantItemEvent event) {
            for (Mission mission : currentCountableMissions) {
                if (mission.getType() == Type.ENCHANT) {
                    List<Map<?, ?>> section = mission.getParams().getMapList("items");

                    for (Map<?, ?> s : section) {
                        Object type = s.getOrDefault("type", null);
                        if (type == null) continue;

                        //Handles missions that count any enchantments
                        if (type.toString().equals("ANY")) {
                            int amount = 1;
                            Object obj = s.getOrDefault("amount", null);
                            if (obj != null) amount = (Integer) obj;

                            if (event.getPlayer() != null) {
                                Player p = event.getPlayer();
                                int count = amount;

                                if (MissionManager.getType(mission.getType()).getCurrentCount(p.getUniqueId(), mission, type.toString()) < amount) {
                                    count = MissionManager.getType(mission.getType()).getCurrentCount(p.getUniqueId(), mission, type.toString()) + 1;
                                }

                                MissionManager.addCounter(p.getUniqueId(), mission, type.toString(), count);
                            }
                        }
                        //Handles missions that count specific block types
                        else {
                            String handle = null;
                            for (Map.Entry<Enchantment, Integer> entry : event.getEnchantments().entrySet()) {
                                Enchantment enchantment = entry.getKey();
                                if (EnchantmentTypes.getCommon().contains(enchantment.getHandle())) {
                                    handle = "COMMON";
                                } else if (EnchantmentTypes.getRare().contains(enchantment.getHandle())) {
                                    handle = "RARE";
                                } else if (EnchantmentTypes.getEpic().contains(enchantment.getHandle())) {
                                    handle = "EPIC";
                                } else if (EnchantmentTypes.getLegendary().contains(enchantment.getHandle())) {
                                    handle = "LEGENDARY";
                                }
                            }

                            if (handle == null || !handle.equalsIgnoreCase(type.toString())) continue;

                            int amount = 1;
                            Object obj = s.getOrDefault("amount", null);
                            if (obj != null) amount = (Integer) obj;

                            if (event.getPlayer() != null && event.getEnchantments().size() > 0) {
                                Player p = event.getPlayer();
                                int count = amount;

                                if (MissionManager.getType(mission.getType()).getCurrentCount(p.getUniqueId(), mission, handle) < amount) {
                                    count = MissionManager.getType(mission.getType()).getCurrentCount(p.getUniqueId(), mission, handle) + 1;
                                }

                                MissionManager.addCounter(p.getUniqueId(), mission, handle, count);
                            }
                        }
                    }
                }
            }
        }

        //Listener for the GenerateType
        @EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL)
        public void onSkycadeEnchantGenerate(SkycadeGenerateEnchantEvent event) {
            for (Mission mission : currentCountableMissions) {
                if (mission.getType() == Type.GENERATE) {
                    List<Map<?, ?>> section = mission.getParams().getMapList("items");

                    for (Map<?, ?> s : section) {
                        Object type = s.getOrDefault("type", null);
                        if (type == null) continue;

                        //Handles missions that count any enchantments
                        if (type.toString().equals("ANY")) {
                            int amount = 1;
                            Object obj = s.getOrDefault("amount", null);
                            if (obj != null) amount = (Integer) obj;

                            if (event.getPlayer() != null) {
                                Player p = event.getPlayer();
                                int count = amount;

                                if (MissionManager.getType(mission.getType()).getCurrentCount(p.getUniqueId(), mission, type.toString()) < amount) {
                                    count = MissionManager.getType(mission.getType()).getCurrentCount(p.getUniqueId(), mission, type.toString()) + 1;
                                }

                                MissionManager.addCounter(p.getUniqueId(), mission, type.toString(), count);
                            }
                        }
                    }
                }
            }
        }

        //Listener for the SwindleType
        @EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL)
        public void onSkycadeSwindle(SkycadeSwindlerEvent event) {
            for (Mission mission : currentCountableMissions) {
                if (mission.getType() == Type.SWINDLE) {
                    List<Map<?, ?>> section = mission.getParams().getMapList("items");

                    for (Map<?, ?> s : section) {
                        Object type = s.getOrDefault("type", null);
                        if (type == null) continue;

                        //Handles missions that count any enchantments
                        if (type.toString().equals("$")) {
                            int amount = 1;
                            Object obj = s.getOrDefault("amount", null);
                            if (obj != null) amount = (Integer) obj;

                            if (event.getPlayer() != null) {
                                Player p = event.getPlayer();
                                int count = amount;

                                if (MissionManager.getType(mission.getType()).getCurrentCount(p.getUniqueId(), mission, type.toString()) < amount) {
                                    count = MissionManager.getType(mission.getType()).getCurrentCount(p.getUniqueId(), mission, type.toString()) + (int) event.getAmount();
                                }

                                MissionManager.addCounter(p.getUniqueId(), mission, type.toString(), count);
                            }
                        }
                    }
                }
            }
        }

        //Listener for the SnowballGunType
        @EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL)
        public void onSkycadeSnowballHit(SkycadeSnowballGunEvent event) {
            for (Mission mission : currentCountableMissions) {
                if (mission.getType() == Type.SNOWBALLGUN) {
                    List<Map<?, ?>> section = mission.getParams().getMapList("items");

                    for (Map<?, ?> s : section) {
                        Object type = s.getOrDefault("type", null);
                        if (type == null) continue;

                        //Handles missions that count any enchantments
                        if (type.toString().equalsIgnoreCase(event.getTarget().getType().toString())) {
                            int amount = 1;
                            Object obj = s.getOrDefault("amount", null);
                            if (obj != null) amount = (Integer) obj;

                            if (event.getShooter() != null && event.getTarget() != null) {
                                Player shooter = event.getShooter();
                                Player target = event.getTarget();
                                int count = amount;

                                if (MissionManager.getType(mission.getType()).getCurrentCount(shooter.getUniqueId(), mission, type.toString()) < amount) {
                                    YamlConfiguration conf = MissionManager.getCompletedConfig();

                                    Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("Europe/London"));
                                    calendar.set(Calendar.HOUR, 0);
                                    calendar.set(Calendar.MINUTE, 0);
                                    calendar.set(Calendar.SECOND, 0);

                                    long timeInMillis = calendar.getTimeInMillis();

                                    boolean doesHitPlayersExist = conf.contains(event.getShooter().getUniqueId().toString() + ".counters." + mission.getHandle() + ".hitPlayers");
                                    boolean isTimeEnabled = conf.getLong(shooter.getUniqueId().toString() + ".counters." + mission.getHandle() + ".activated") > timeInMillis;
                                    List<String> hitPlayers = new ArrayList<>();

                                    //Resets the hitPlayers list if the mission is not current
                                    if (doesHitPlayersExist && !isTimeEnabled) {
                                        conf.set(event.getShooter().getUniqueId().toString() + ".counters." + mission.getHandle() + ".hitPlayers", hitPlayers);
                                    }

                                    if (doesHitPlayersExist && isTimeEnabled)
                                        hitPlayers.addAll(conf.getStringList(event.getShooter().getUniqueId().toString() + ".counters." + mission.getHandle() + ".hitPlayers"));

                                    //If the player has already been hit, return
                                    if (hitPlayers.contains(target.getUniqueId().toString())) return;
                                    hitPlayers.add(target.getUniqueId().toString());
                                    conf.set(event.getShooter().getUniqueId().toString() + ".counters." + mission.getHandle() + ".hitPlayers", hitPlayers);

                                    MissionManager.setCompletedConfig(conf);

                                    count = MissionManager.getType(mission.getType()).getCurrentCount(shooter.getUniqueId(), mission, type.toString()) + 1;
                                }


                                MissionManager.addCounter(shooter.getUniqueId(), mission, type.toString(), count);
                            }
                        }
                    }
                }
            }
        }
    }
}
