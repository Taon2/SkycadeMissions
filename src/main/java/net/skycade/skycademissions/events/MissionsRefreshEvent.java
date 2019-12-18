package net.skycade.skycademissions.events;

import net.skycade.skycademissions.missions.Mission;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.List;

public class MissionsRefreshEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private List<Mission> current;

    public MissionsRefreshEvent(List<Mission> current) {
        this.current = current;
    }

    public List<Mission> getCurrent() {
        return current;
    }

    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
