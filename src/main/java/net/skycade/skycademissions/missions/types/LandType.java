package net.skycade.skycademissions.missions.types;

import net.skycade.SkycadeCore.Localization;
import net.skycade.skycademissions.MissionsUser;
import net.skycade.skycademissions.missions.Mission;
import net.skycade.skycademissions.missions.Result;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class LandType extends MissionType {

    private static final Localization.Message MISSING_ITEM = new Localization.Message("missing-item", "&cYou are missing %val% %item%!");

    public LandType() {
        super();
        Localization.getInstance().registerMessages("skycade.factions.missions.land",
                MISSING_ITEM
        );
    }

    @Override
    public Result validate(Player player, List<Map<?, ?>> params) {

        final HashMap<Material, Integer> neededItem = new HashMap<Material, Integer>();
        final HashMap<EntityType, Integer> neededEntities = new HashMap<EntityType, Integer>();

        for (Map<?, ?> s : params) {
            int amount = 1;
            Object obj = s.getOrDefault("amount", null);
            if (obj != null) amount = (Integer) obj;
            Object type = s.getOrDefault("type", null);

            // Find out if the needed item is a Material or an Entity
            boolean isEntity = false;
            for (EntityType entityType : EntityType.values()) {
                if (entityType.toString().equalsIgnoreCase(((String) type))) {
                    isEntity = true;
                    break;
                }
            }
            if (isEntity) {
                EntityType entityType = EntityType.valueOf(((String) type).toUpperCase());
                neededEntities.put(entityType, amount);
            } else {
                Material item;
                if (StringUtils.isNumeric(((String) type))) {
                    item = Material.valueOf((String) type);
                } else {
                    item = Material.getMaterial(((String) type).toUpperCase());
                }
                neededItem.put(item, amount);
            }
        }

        // We now have two sets of required items or entities
        // Check the items first
        final Location l = player.getLocation();
        final int px = l.getBlockX();
        final int py = l.getBlockY();
        final int pz = l.getBlockZ();

        // Get search radius - min is 10, max is 50
        int searchRadius = 10;
        for (int x = -searchRadius; x <= searchRadius; x++) {
            for (int y = -searchRadius; y <= searchRadius; y++) {
                for (int z = -searchRadius; z <= searchRadius; z++) {
                    final Material b = new Location(l.getWorld(), px + x, py + y, pz + z).getBlock().getType();
                    if (neededItem.containsKey(b)) {
                        if (neededItem.get(b) == 1) {
                            neededItem.remove(b);
                        } else {
                            // Reduce the require amount by 1
                            neededItem.put(b, neededItem.get(b) - 1);
                        }
                    }
                }
            }
        }
        // Check if all the needed items have been amassed
        if (!neededItem.isEmpty()) {
            for (Material missing : neededItem.keySet()) {
                player.sendMessage(ChatColor.RED +  "You are missing: " + neededItem.get(missing) + " x " + missing.toString() + "!");
                player.sendMessage(MISSING_ITEM.getMessage(player)
                        .replaceAll("%val%", (neededItem.get(missing)) + "")
                        .replaceAll("%item%", missing.toString())
                );
            }
            return new Result(Result.Type.FAILURE);
        } else {
            for (Entity entity : player.getNearbyEntities(searchRadius, searchRadius, searchRadius)) {
                if (neededEntities.containsKey(entity.getType())) {
                    if (neededEntities.get(entity.getType()) == 1) {
                        neededEntities.remove(entity.getType());
                    } else {
                        neededEntities.put(entity.getType(), neededEntities.get(entity.getType()) - 1);
                    }
                }
            }
            if (neededEntities.isEmpty()) {
                return new Result(Result.Type.SUCCESS);
            } else {
                for (EntityType missing : neededEntities.keySet()) {
                    player.sendMessage(MISSING_ITEM.getMessage(player)
                            .replaceAll("%val%", (neededEntities.get(missing)) + "")
                            .replaceAll("%item%", missing.toString())
                    );
                }
                return new Result(Result.Type.FAILURE);
            }
        }
    }

    @Override
    public Result validate(Player player, List<Map<?, ?>> params, Mission mission) {
        return validate(player, params);
    }

    @Override
    public Type getType() {
        return Type.LAND;
    }

    @Override
    public int getCurrentCount(UUID uuid, Mission mission, String countedThing) {
        MissionsUser user = MissionsUser.get(uuid);

        int currentAmount = 0;
        int amount = 1;
        List<Map<?, ?>> section = mission.getParams();
        final HashMap<Material, Integer> neededItem = new HashMap<Material, Integer>();
        final HashMap<EntityType, Integer> neededEntities = new HashMap<EntityType, Integer>();

        for (Map<?, ?> s : section) {
            Object type = s.getOrDefault("type", null);

            if (type.toString().equals(countedThing)) {
                Object obj = s.getOrDefault("amount", null);
                if (obj != null) amount = (Integer) obj;
                // Find out if the needed item is a Material or an Entity
                boolean isEntity = false;
                for (EntityType entityType : EntityType.values()) {
                    if (entityType.toString().equalsIgnoreCase(((String) type))) {
                        isEntity = true;
                        break;
                    }
                }
                if (isEntity) {
                    EntityType entityType = EntityType.valueOf(((String) type).toUpperCase());
                    neededEntities.put(entityType, amount);
                } else {
                    Material item;
                    if (StringUtils.isNumeric(((String) type))) {
                        item = Material.valueOf((String) type);
                    } else {
                        item = Material.getMaterial(((String) type).toUpperCase());
                    }
                    neededItem.put(item, amount);
                }
            }
        }

        // We now have two sets of required items or entities
        // Check the items first
        final Location l = Bukkit.getPlayer(uuid).getLocation();
        final int px = l.getBlockX();
        final int py = l.getBlockY();
        final int pz = l.getBlockZ();

        // Get search radius - min is 10, max is 50
        int searchRadius = 10;
        for (int x = -searchRadius; x <= searchRadius; x++) {
            for (int y = -searchRadius; y <= searchRadius; y++) {
                for (int z = -searchRadius; z <= searchRadius; z++) {
                    final Material b = new Location(l.getWorld(), px + x, py + y, pz + z).getBlock().getType();
                    if (neededItem.containsKey(b)) {
                        if (neededItem.get(b) == 1) {
                            neededItem.remove(b);
                        } else {
                            // Reduce the require amount by 1
                            neededItem.put(b, neededItem.get(b) - 1);
                        }
                    }
                }
            }
        }

        //Check the entities
        for (Entity entity : Bukkit.getPlayer(uuid).getNearbyEntities(searchRadius, searchRadius, searchRadius)) {
            if (neededEntities.containsKey(entity.getType())) {
                if (neededEntities.get(entity.getType()) == 1) {
                    neededEntities.remove(entity.getType());
                } else {
                    neededEntities.put(entity.getType(), neededEntities.get(entity.getType()) - 1);
                }
            }
        }

        if (user.hasPlayerCompleted(mission)) {
            currentAmount = amount;
        } else if (!neededItem.isEmpty()) {
            for (Material missing : neededItem.keySet()) {
                currentAmount = amount - neededItem.get(missing);
            }
        } else if (!neededEntities.isEmpty()) {
            for (EntityType missing : neededEntities.keySet()) {
                currentAmount = amount - neededEntities.get(missing);
            }
        } else {
            currentAmount = amount;
        }

        return currentAmount;
    }
}
