package org.bacon.vanishPlusPlusPlus.listener;

import org.bacon.vanishPlusPlusPlus.VanishManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SpectatorToggleListener implements Listener {
    private static final long DOUBLE_SNEAK_WINDOW_MILLIS = 500L;

    private final VanishManager vanishManager;
    private final Map<UUID, Long> lastSneakTimes = new ConcurrentHashMap<>();

    public SpectatorToggleListener(VanishManager vanishManager) {
        this.vanishManager = vanishManager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onSneak(PlayerToggleSneakEvent event) {
        if (!event.isSneaking()) {
            return;
        }

        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        if (!vanishManager.isVanished(player)) {
            lastSneakTimes.remove(playerId);
            return;
        }
        if (!vanishManager.getRules(player).doubleSneakSpectator()) {
            lastSneakTimes.remove(playerId);
            return;
        }

        long now = System.currentTimeMillis();
        Long lastSneak = lastSneakTimes.put(playerId, now);
        if (lastSneak != null && now - lastSneak <= DOUBLE_SNEAK_WINDOW_MILLIS) {
            lastSneakTimes.remove(playerId);
            vanishManager.toggleSpectatorWhileVanished(player);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        lastSneakTimes.remove(event.getPlayer().getUniqueId());
    }
}
