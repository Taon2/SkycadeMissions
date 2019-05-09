package net.skycade.skycademissions.missions.types;

import net.skycade.skycademissions.missions.DailyMissionManager;
import net.skycade.skycademissions.missions.Mission;
import net.skycade.skycademissions.missions.MissionManager;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class TypesListener implements Listener {

    private static List<Mission> currentCountableMissions = new ArrayList<>();

    private static List<Type> countableTypes;

    static {
        countableTypes = Arrays.asList(
                Type.DAMAGE,
                Type.KILLS,
                Type.MINING
        );
    }

    public TypesListener() {
        for (String handle : DailyMissionManager.getCurrent()) {
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
}
