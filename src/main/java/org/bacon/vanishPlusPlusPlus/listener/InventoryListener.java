package org.bacon.vanishPlusPlusPlus.listener;

import org.bacon.vanishPlusPlusPlus.VanishManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;

public final class InventoryListener implements Listener {
    private final VanishManager vanishManager;

    public InventoryListener(VanishManager vanishManager) {
        this.vanishManager = vanishManager;
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        vanishManager.clearSilentContainer(player.getUniqueId());
    }
}
