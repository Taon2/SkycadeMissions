package net.skycade.skycademissions.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.List;

public class MissionsRefreshEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private List<String> current;

    public MissionsRefreshEvent(List<String> current) {
        this.current = current;
    }

    public List<String> getCurrent() {
        return current;
    }

    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
