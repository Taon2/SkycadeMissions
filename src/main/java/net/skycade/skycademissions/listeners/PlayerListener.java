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
    public void onPlayerLogin(PlayerJoinEvent e) {
        missionsUserManager.load(e.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerLogout(PlayerQuitEvent e) {
        MissionsUser user = missionsUserManager.get(e.getPlayer().getUniqueId());

        missionsUserManager.updateCompletedDatabase(user);
        missionsUserManager.updateCountsDatabase(user);

        missionsUserManager.remove(e.getPlayer().getUniqueId());
    }
}
