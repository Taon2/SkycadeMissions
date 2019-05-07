package net.skycade.skycademissions.missions.types;

import net.skycade.SkycadeCore.Localization;
import net.skycade.SkycadeCore.Localization.Message;
import net.skycade.skycademissions.missions.Mission;
import net.skycade.skycademissions.missions.MissionManager;
import net.skycade.skycademissions.missions.Result;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class InventoryType extends MissionType {

    private static final Message MISSING_ITEM = new Message("missing-item", "&cYou are missing %val% of %item%!");

    public InventoryType() {
        super();
        Localization.getInstance().registerMessages("skycade.factions.missions.inventory",
                MISSING_ITEM
        );
    }

    @Override
    public Result validate(Player player, ConfigurationSection params) {

        boolean hasFailed = false;
        List<Map<?, ?>> section = params.getMapList("items");

        PlayerInventory inventory = player.getInventory();
        for (Map<?, ?> s : section) {

            Object type = s.getOrDefault("type", null);
            if (type == null) continue;

            Material material;
            try {
                material = Material.valueOf(((String) type).toUpperCase());
            } catch (IllegalArgumentException e) {
                continue; // todo warn console
            }

            int amount = 1;
            Object obj = s.getOrDefault("amount", null);
            if (obj != null) amount = (Integer) obj;

            Short durability = null;
            obj = s.getOrDefault("durability", null);
            if (obj != null) durability = ((Integer) obj).shortValue();

            final Short finalDurability = durability;

            int sum = Arrays.stream(inventory.getContents())
                    .filter(e -> e != null && e.getType() == material &&
                            (finalDurability == null || finalDurability == e.getDurability()))
                    .mapToInt(ItemStack::getAmount).sum();

            if (sum < amount) {
                hasFailed = true;
                if (finalDurability == null){
                    player.sendMessage(MISSING_ITEM.getMessage(player)
                            .replaceAll("%val%", (amount - sum) + "")
                            .replaceAll("%item%", material.name().toLowerCase().replaceAll("_", " "))
                    );
                }
                else{
                    player.sendMessage(MISSING_ITEM.getMessage(player)
                            .replaceAll("%val%", (amount - sum) + "")
                            .replaceAll("%item%", material.name().toLowerCase().replaceAll("_", " ") + ":" + finalDurability)
                    );
                }
            }
        }
        if (hasFailed){
            return new Result(Result.Type.FAILURE);
        }

        return new Result(Result.Type.SUCCESS);
    }

    @Override
    public Result validate(Player player, ConfigurationSection params, Mission mission) {
        return validate(player, params);
    }

    @Override
    public void postComplete(Player player, ConfigurationSection params) {
        super.postComplete(player, params);

        List<Map<?, ?>> section = params.getMapList("items");

        for (Map<?, ?> s : section) {

            Object type = s.getOrDefault("type", null);
            if (type == null) continue;

            Material material;
            try {
                material = Material.valueOf(((String) type).toUpperCase());
            } catch (IllegalArgumentException e) {
                continue; // todo warn console
            }

            int amount = 1;
            Object obj = s.getOrDefault("amount", null);
            if (obj != null) amount = (Integer) obj;

            Short durability = null;
            obj = s.getOrDefault("durability", null);
            if (obj != null) durability = ((Integer) obj).shortValue();

            final Short finalDurability = durability;

            if (finalDurability != null) {
                ItemStack item = new ItemStack(material, amount);
                item.setDurability(finalDurability);
                player.getInventory().removeItem(item);
            }
            else{
                ItemStack item = new ItemStack(material, amount);
                player.getInventory().removeItem(item);
            }
            player.updateInventory();
        }
    }

    @Override
    public Type getType() {
        return Type.INVENTORY;
    }

    @Override
    public int getCurrentCount(UUID uuid, Mission mission, String countedThing) {
        int currentAmount = 0;
        Player player = Bukkit.getPlayer(uuid);
        List<Map<?, ?>> section = mission.getParams().getMapList("items");

        PlayerInventory inventory = player.getInventory();
        for (Map<?, ?> s : section) {
            Object type = s.getOrDefault("type", null);
            if (type == null) continue;

            if (type.toString().equals(countedThing)) {

                Material material;
                try {
                    material = Material.valueOf(type.toString().toUpperCase());
                } catch (IllegalArgumentException e) {
                    continue; // todo warn console
                }

                int amount = 1;
                Object obj = s.getOrDefault("amount", null);
                if (obj != null) amount = (Integer) obj;

                Short durability = null;
                obj = s.getOrDefault("durability", null);
                if (obj != null) durability = ((Integer) obj).shortValue();

                final Short finalDurability = durability;

                int sum = Arrays.stream(inventory.getContents())
                        .filter(e -> e != null && e.getType() == material &&
                                (finalDurability == null || finalDurability == e.getDurability()))
                        .mapToInt(ItemStack::getAmount).sum();

                if (MissionManager.hasPlayerCompleted(uuid, mission)) {
                    currentAmount = amount;
                } else if (sum < amount) {
                    currentAmount = sum;
                } else {
                    currentAmount = amount;
                }
            }
        }

        return currentAmount;
    }
}
