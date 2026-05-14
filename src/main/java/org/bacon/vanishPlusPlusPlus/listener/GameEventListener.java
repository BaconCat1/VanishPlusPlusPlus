package org.bacon.vanishPlusPlusPlus.listener;

import org.bacon.vanishPlusPlusPlus.Settings;
import org.bacon.vanishPlusPlusPlus.VanishManager;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.GenericGameEvent;

public final class GameEventListener implements Listener {
    private final VanishManager vanishManager;
    private final Settings settings;

    public GameEventListener(VanishManager vanishManager, Settings settings) {
        this.vanishManager = vanishManager;
        this.settings = settings;
    }

    @EventHandler(ignoreCancelled = true)
    public void onGameEvent(GenericGameEvent event) {
        if (!settings.preventSculkSensors) {
            return;
        }
        Entity entity = event.getEntity();
        if (!(entity instanceof Player player)) {
            return;
        }
        if (!vanishManager.isVanished(player)) {
            return;
        }
        if (!vanishManager.getRules(player).canTriggerWorldEvents()) {
            event.setCancelled(true);
        }
    }
}
