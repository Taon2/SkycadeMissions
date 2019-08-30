package net.skycade.skycademissions.missions;

import net.skycade.skycademissions.missions.types.Type;
import org.bukkit.ChatColor;
import org.bukkit.Material;

import java.util.List;
import java.util.Map;

public class Mission {

    private final boolean isDaily;
    private boolean isCurrent;
    private long generatedOn;
    private final Type type;
    private final String handle;
    private final String displayName;

    private final List<Map<?, ?>> params;

    private final MissionLevel level;

    private final Material icon;
    private final short durability;
    private final List<String> lore;

    private final int position;

    boolean isDaily() {
        return isDaily;
    }

    Mission(Boolean isDaily, Boolean isCurrent, long generatedOn, Type type, String handle, String displayName, List<Map<?, ?>> params,
            MissionLevel level, String icon, List<String> lore, int position) {
        this.isDaily = isDaily;
        this.isCurrent = isCurrent;
        this.generatedOn = generatedOn;
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

    public List<Map<?, ?>> getParams() {
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

    public boolean isCurrent() {
        return isCurrent;
    }

    public void setCurrent(boolean isCurrent){
        this.isCurrent = isCurrent;
    }

    public long getGeneratedOn() {
        return generatedOn;
    }

    public void setGeneratedOn(long generatedOn) {
        this.generatedOn = generatedOn;
    }
}
