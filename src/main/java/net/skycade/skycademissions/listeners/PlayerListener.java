package net.skycade.skycademissions.listeners;

import net.skycade.skycademissions.MissionsUser;
import net.skycade.skycademissions.MissionsUserManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {

    private MissionsUserManager missionsUserManager;

    public PlayerListener(MissionsUserManager missionsUserManager) {
        this.missionsUserManager = missionsUserManager;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerLogin(PlayerJoinEvent event) {
        missionsUserManager.load(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerLogout(PlayerQuitEvent event) {
        MissionsUser user = missionsUserManager.get(event.getPlayer().getUniqueId());

        missionsUserManager.updateCountsDatabase(user);
        missionsUserManager.updateCompletedDatabase(user);

        missionsUserManager.remove(event.getPlayer().getUniqueId());
    }
}
