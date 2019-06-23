package net.skycade.skycademissions.missions;

import net.skycade.skycademissions.missions.types.Type;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.List;

public class Mission {

    private final boolean isDaily;
    private final Type type;
    private final String handle;
    private final String displayName;

    private final ConfigurationSection params;

    private final MissionLevel level;

    private final Material icon;
    private final short durability;
    private final List<String> lore;

    private final int position;

    private final long expiry;

    boolean isDaily() {
        return isDaily;
    }

    Mission(Boolean isDaily, Type type, String handle, String displayName, ConfigurationSection params,
            MissionLevel level, String icon, List<String> lore, int position, long expiry) {
        this.isDaily = isDaily;
        this.type = type;
        this.handle = handle;
        this.displayName = displayName == null ? null : ChatColor.translateAlternateColorCodes('&', displayName);
        this.params = params;
        this.level = level;
        if (icon.contains(":")) {
            this.icon = Material.valueOf(icon.substring(0, icon.indexOf(":")));
            int colonLocation = icon.indexOf(":");

            this.durability = Short.parseShort(icon.substring((colonLocation+1)));
        }
        else{
            this.icon = Material.valueOf(icon.toUpperCase());
            this.durability = 0;
        }

        this.lore = lore;
        this.position = position;
        this.expiry = expiry;

    }

    short getDurability() {
        return durability;
    }

    public Type getType() {
        return type;
    }

    public String getHandle() {
        return handle;
    }

    String getDisplayName() {
        return displayName;
    }

    public ConfigurationSection getParams() {
        return params;
    }

    public MissionLevel getLevel() {
        return level;
    }

    Material getIcon() {

        return icon;
    }

    List<String> getLore() {
        return lore;
    }

    Integer getPosition() {
        return position;
    }

    public long getExpiry() {
        return expiry;
    }
}
