package net.skycade.skycademissions.missions.types;

import net.skycade.SkycadeEnchants.enchant.common.Enchantment;
import net.skycade.SkycadeEnchants.events.SkycadeCustomEnchantItemEvent;
import net.skycade.SkycadeEnchants.events.SkycadeGenerateEnchantEvent;
import net.skycade.SkycadeEnchants.events.SkycadeSnowballGunEvent;
import net.skycade.SkycadeEnchants.events.SkycadeSwindlerEvent;
import net.skycade.prisons.util.EnchantmentTypes;
import net.skycade.skycademissions.MissionsUser;
import net.skycade.skycademissions.SkycadeMissionsPlugin;
import net.skycade.skycademissions.missions.DailyMissionManager;
import net.skycade.skycademissions.missions.Mission;
import net.skycade.skycademissions.missions.MissionManager;
import net.skycade.skycadeshop.impl.skycade.event.PostSellTransactionEvent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
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
        loadCurrentCountableMissions();

        Bukkit.getPluginManager().registerEvents(new SkycadeShopListener(), SkycadeMissionsPlugin.getInstance()); // don't suicide if shop plugin is not loaded
        Bukkit.getPluginManager().registerEvents(new SkycadeEnchantsListener(), SkycadeMissionsPlugin.getInstance()); // don't suicide if enchants plugin is not loaded
    }

    public static void loadCurrentCountableMissions() {
        for (String handle : DailyMissionManager.getInstance().getCurrent()) {
            Mission mission = MissionManager.getMissionFromName(handle);
            if (mission != null && countableTypes.contains(mission.getType())) {
                currentCountableMissions.add(mission);
            }
        }
    }

    //Listener for the DamageType
    @EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL)
    public void onEntityDamage(EntityDamageByEntityEvent e) {
        for (Mission mission : currentCountableMissions) {
            if (mission.getType() == Type.DAMAGE) {
                List<Map<?, ?>> section = mission.getParams();

                for (Map<?, ?> s : section) {
                    Object type = s.getOrDefault("type", null);
                    if (type == null) continue;
                    EntityType entityType = EntityType.valueOf(type.toString());

                    int amount = 1;
                    Object obj = s.getOrDefault("amount", null);
                    if (obj != null) amount = (Integer) obj;

                    if (e.getDamager() != null && e.getDamager().getType() == EntityType.PLAYER && e.getEntity().getType() == entityType) {
                        Player p = (Player) e.getDamager();
                        MissionsUser user = MissionsUser.get(p.getUniqueId());

                        int count = amount;

                        if (MissionManager.getType(mission.getType()).getCurrentCount(p.getUniqueId(), mission, type.toString()) < amount) {
                            count = MissionManager.getType(mission.getType()).getCurrentCount(p.getUniqueId(), mission, type.toString()) + (int) e.getDamage();
                        }

                        user.addCounter(mission, e.getEntity().getType().toString(), count);
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
                List<Map<?, ?>> section = mission.getParams();

                for (Map<?, ?> s : section) {
                    Object type = s.getOrDefault("type", null);
                    if (type == null) continue;

                    if (type.toString().equals("ANY")) {
                        int amount = 1;
                        Object obj = s.getOrDefault("amount", null);
                        if (obj != null) amount = (Integer) obj;

                        if (e.getEntity().getKiller() != null) {
                            Player p = e.getEntity().getKiller();
                            MissionsUser user = MissionsUser.get(p.getUniqueId());

                            int count = amount;

                            if (MissionManager.getType(mission.getType()).getCurrentCount(p.getUniqueId(), mission, type.toString()) < amount) {
                                count = MissionManager.getType(mission.getType()).getCurrentCount(p.getUniqueId(), mission, type.toString()) + 1;
                            }

                            user.addCounter(mission, type.toString(), count);
                        }
                    } else {
                        EntityType entityType = EntityType.valueOf(type.toString());

                        int amount = 1;
                        Object obj = s.getOrDefault("amount", null);
                        if (obj != null) amount = (Integer) obj;

                        if (e.getEntity().getKiller() != null && e.getEntity().getKiller().getType() == EntityType.PLAYER && e.getEntity().getType() == entityType) {
                            Player p = e.getEntity().getKiller();
                            MissionsUser user = MissionsUser.get(p.getUniqueId());

                            int count = amount;

                            if (MissionManager.getType(mission.getType()).getCurrentCount(p.getUniqueId(), mission, entityType.toString()) < amount) {
                                count = MissionManager.getType(mission.getType()).getCurrentCount(p.getUniqueId(), mission, entityType.toString()) + 1;
                            }

                            user.addCounter(mission, entityType.toString(), count);
                        }
                    }
                }
            }
        }
    }

    //Listener for the MiningType
    @EventHandler(/*ignoreCancelled = true, */priority = EventPriority.NORMAL)
    public void onBlockBreak(BlockBreakEvent e) {
        for (Mission mission : currentCountableMissions) {
            if (mission.getType() == Type.MINING) {

                List<Map<?, ?>> section = mission.getParams();

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
                            MissionsUser user = MissionsUser.get(p.getUniqueId());

                            int count = amount;

                            if (MissionManager.getType(mission.getType()).getCurrentCount(p.getUniqueId(), mission, type.toString()) < amount) {
                                count = MissionManager.getType(mission.getType()).getCurrentCount(p.getUniqueId(), mission, type.toString()) + 1;
                            }

                            user.addCounter(mission, type.toString(), count);
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
                            MissionsUser user = MissionsUser.get(p.getUniqueId());

                            int count = amount;

                            if (MissionManager.getType(mission.getType()).getCurrentCount(p.getUniqueId(), mission, materialType.toString()) < amount) {
                                count = MissionManager.getType(mission.getType()).getCurrentCount(p.getUniqueId(), mission, materialType.toString()) + 1;
                            }

                            user.addCounter(mission, materialType.toString(), count);
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

                List<Map<?, ?>> section = mission.getParams();

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
                            MissionsUser user = MissionsUser.get(p.getUniqueId());

                            int count = amount;

                            if (MissionManager.getType(mission.getType()).getCurrentCount(p.getUniqueId(), mission, type.toString()) < amount) {
                                count = MissionManager.getType(mission.getType()).getCurrentCount(p.getUniqueId(), mission, type.toString()) + 1;
                            }

                            user.addCounter(mission, type.toString(), count);
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
                        if (obj != null && ((Integer) obj).shortValue() != -1) durability = ((Integer) obj).shortValue();

                        if (e.getPlayer() != null && e.getCaught() != null && e.getCaught() instanceof Item && ((Item) e.getCaught()).getItemStack().getType() == materialType && ((Item) e.getCaught()).getItemStack().getDurability() == durability) {
                            Player p = e.getPlayer();
                            MissionsUser user = MissionsUser.get(p.getUniqueId());

                            int count = amount;

                            if (((Item) e.getCaught()).getItemStack().getDurability() == durability) {
                                if (MissionManager.getType(mission.getType()).getCurrentCount(p.getUniqueId(), mission, materialType.toString()) < amount) {
                                    count = MissionManager.getType(mission.getType()).getCurrentCount(p.getUniqueId(), mission, materialType.toString()) + 1;
                                }
                            }

                            user.addCounter(mission, materialType.toString() + ":" + durability, count);
                        }
                    }
                }
            }
        }
    }

    private Map<UUID, Long> onlineMap = new HashMap<>();

    //Listener for the PlaytimeType
    @EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL)
    public void onPlayerLogin(PlayerLoginEvent event) {
        for (Mission mission : currentCountableMissions) {
            if (mission.getType() == Type.PLAYTIME) {
                Player p = event.getPlayer();
                MissionsUser user = MissionsUser.get(p.getUniqueId());

                List<Map<?, ?>> section = mission.getParams();

                for (Map<?, ?> s : section) {
                    Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("Europe/London"));
                    calendar.set(Calendar.HOUR, 0);
                    calendar.set(Calendar.MINUTE, 0);
                    calendar.set(Calendar.SECOND, 0);

                    long timeInMillis = calendar.getTimeInMillis();

                    Object type = s.getOrDefault("type", null);
                    if (type == null) continue;

                    if (type.toString().equals("HOURS")) {
                        boolean doesCountExist = user.getCompleted().containsKey(mission.getHandle());
                        boolean isTimeEnabled = user.getCompleted().get(mission.getHandle()) > timeInMillis;

                        if (!doesCountExist || !isTimeEnabled)
                            user.addLongCounter(mission, type.toString(), 0);

                        onlineMap.put(event.getPlayer().getUniqueId(), System.currentTimeMillis());
                    }
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL)
    public void onPlayerLogout(PlayerQuitEvent event) {
        for (Mission mission : currentCountableMissions) {
            if (mission.getType() == Type.PLAYTIME) {

                List<Map<?, ?>> section = mission.getParams();

                for (Map<?, ?> s : section) {
                    Object type = s.getOrDefault("type", null);
                    if (type == null) continue;

                    if (type.toString().equals("HOURS")) {
                        int amount = 1;
                        Object obj = s.getOrDefault("amount", null);
                        if (obj != null) amount = (Integer) obj;

                        if (event.getPlayer() != null) {
                            Player p = event.getPlayer();
                            MissionsUser user = MissionsUser.get(p.getUniqueId());

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

                            user.addLongCounter(mission, type.toString(), count);
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
                    List<Map<?, ?>> section = mission.getParams();

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
                                MissionsUser user = MissionsUser.get(p.getUniqueId());

                                int count = amount;

                                if (MissionManager.getType(mission.getType()).getCurrentCount(p.getUniqueId(), mission, type.toString()) < amount) {
                                    count = MissionManager.getType(mission.getType()).getCurrentCount(p.getUniqueId(), mission, type.toString()) + (int) event.getMoneyGained();
                                }

                                user.addCounter(mission, type.toString(), count);
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
                    List<Map<?, ?>> section = mission.getParams();

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
                                MissionsUser user = MissionsUser.get(p.getUniqueId());

                                int count = amount;

                                if (MissionManager.getType(mission.getType()).getCurrentCount(p.getUniqueId(), mission, type.toString()) < amount) {
                                    count = MissionManager.getType(mission.getType()).getCurrentCount(p.getUniqueId(), mission, type.toString()) + 1;
                                }

                                user.addCounter(mission, type.toString(), count);
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
                                MissionsUser user = MissionsUser.get(p.getUniqueId());

                                int count = amount;

                                if (MissionManager.getType(mission.getType()).getCurrentCount(p.getUniqueId(), mission, handle) < amount) {
                                    count = MissionManager.getType(mission.getType()).getCurrentCount(p.getUniqueId(), mission, handle) + 1;
                                }

                                user.addCounter(mission, handle, count);
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
                    List<Map<?, ?>> section = mission.getParams();

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
                                MissionsUser user = MissionsUser.get(p.getUniqueId());

                                int count = amount;

                                if (MissionManager.getType(mission.getType()).getCurrentCount(p.getUniqueId(), mission, type.toString()) < amount) {
                                    count = MissionManager.getType(mission.getType()).getCurrentCount(p.getUniqueId(), mission, type.toString()) + 1;
                                }

                                user.addCounter(mission, type.toString(), count);
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
                    List<Map<?, ?>> section = mission.getParams();

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
                                MissionsUser user = MissionsUser.get(p.getUniqueId());

                                int count = amount;

                                if (MissionManager.getType(mission.getType()).getCurrentCount(p.getUniqueId(), mission, type.toString()) < amount) {
                                    count = MissionManager.getType(mission.getType()).getCurrentCount(p.getUniqueId(), mission, type.toString()) + (int) event.getAmount();
                                }

                                user.addCounter(mission, type.toString(), count);
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
                    List<Map<?, ?>> section = mission.getParams();

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
                                MissionsUser user = MissionsUser.get(shooter.getUniqueId());

                                int count = amount;

                                if (MissionManager.getType(mission.getType()).getCurrentCount(shooter.getUniqueId(), mission, type.toString()) < amount) {

                                    Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("Europe/London"));
                                    calendar.set(Calendar.HOUR, 0);
                                    calendar.set(Calendar.MINUTE, 0);
                                    calendar.set(Calendar.SECOND, 0);

                                    long timeInMillis = calendar.getTimeInMillis();

                                    //If the player has already been hit, return
                                    if (user.getHitWithSnowball().contains(target.getUniqueId())) return;
                                    user.addHitWithSnowball(target.getUniqueId());

                                    count = MissionManager.getType(mission.getType()).getCurrentCount(shooter.getUniqueId(), mission, type.toString()) + 1;
                                }

                                user.addCounter(mission, type.toString(), count);
                            }
                        }
                    }
                }
            }
        }
    }
}
