package net.skycade.skycademissions.missions.types;

import net.skycade.skycademissions.SkycadeMissionsPlugin;
import net.skycade.skycademissions.missions.DailyMissionManager;
import net.skycade.skycademissions.missions.Mission;
import org.bukkit.Bukkit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TypesManager {

    private final SkycadeMissionsPlugin plugin;

    private List<Mission> currentCountableMissions = new ArrayList<>();

    private static final List<Type> countableTypes;

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
                Type.SPECIALABILITY,
                Type.KITKILL,
                Type.KILLSTREAK,
                Type.COINREWARD,
                Type.ISLAND_LEVEL,
                Type.TAMING
        );
    }

    public TypesManager(SkycadeMissionsPlugin plugin) {
        this.plugin = plugin;
    }

    public void registerTypes() {
        Bukkit.getPluginManager().registerEvents(new DamageType(this), plugin);
        Bukkit.getPluginManager().registerEvents(new FishingType(this), plugin);
        Bukkit.getPluginManager().registerEvents(new InventoryType(this), plugin);
        Bukkit.getPluginManager().registerEvents(new KillType(this), plugin);
        Bukkit.getPluginManager().registerEvents(new LandType(this), plugin);
        Bukkit.getPluginManager().registerEvents(new LevelType(this), plugin);
        Bukkit.getPluginManager().registerEvents(new MiningType(this), plugin);
        Bukkit.getPluginManager().registerEvents(new TamingType(this), plugin);

        //Don't suicide if shop plugin isn't loaded
        if (Bukkit.getPluginManager().getPlugin("SkycadeShop") != null) {
            Bukkit.getPluginManager().registerEvents(new ShopType(this), plugin);
            Bukkit.getPluginManager().registerEvents(new SwindleType(this), plugin);
        }

        //Don't suicide if enchants plugin isn't loaded
        if (Bukkit.getPluginManager().getPlugin("SkycadeEnchants") != null) {
            Bukkit.getPluginManager().registerEvents(new EnchantType(this), plugin);
            Bukkit.getPluginManager().registerEvents(new GenerateType(this), plugin);
            Bukkit.getPluginManager().registerEvents(new SnowballGunType(this), plugin);
        }

        //Don't suicide if kitpvp plugin isn't loaded
        if (Bukkit.getPluginManager().getPlugin("KitPvP") != null) {
            Bukkit.getPluginManager().registerEvents(new SpecialAbilityType(this), plugin);
            Bukkit.getPluginManager().registerEvents(new KitKillType(this), plugin);
            Bukkit.getPluginManager().registerEvents(new KillstreakType(this), plugin);
            Bukkit.getPluginManager().registerEvents(new CoinRewardType(this), plugin);
        }

        //Don't suicide if skyblock plugin isn't loaded
        if (Bukkit.getPluginManager().getPlugin("SkycadeSkyblock") != null) {
            Bukkit.getPluginManager().registerEvents(new IslandLevelType(this), plugin);
        }
    }

    public void loadCurrentCountableMissions() {
        for (Mission mission : DailyMissionManager.getInstance().getCurrent()) {
            if (mission != null && countableTypes.contains(mission.getType())) {
                currentCountableMissions.add(mission);
            }
        }
    }

    public List<Mission> getCurrentCountableMissions() {
        return currentCountableMissions;
    }
}
