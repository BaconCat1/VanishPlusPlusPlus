package org.bacon.vanishPlusPlusPlus.listener;

import org.bacon.vanishPlusPlusPlus.Settings;
import org.bacon.vanishPlusPlusPlus.VanishManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.raid.RaidTriggerEvent;

public final class RaidListener implements Listener {
    private final VanishManager vanishManager;
    private final Settings settings;

    public RaidListener(VanishManager vanishManager, Settings settings) {
        this.vanishManager = vanishManager;
        this.settings = settings;
    }

    @EventHandler(ignoreCancelled = true)
    public void onRaidTrigger(RaidTriggerEvent event) {
        if (!settings.preventRaidTrigger) {
            return;
        }
        Player player = event.getPlayer();
        if (vanishManager.isVanished(player)) {
            event.setCancelled(true);
        }
    }
}
