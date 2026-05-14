package org.bacon.vanishPlusPlusPlus.listener;

import com.destroystokyo.paper.event.player.PlayerAdvancementCriterionGrantEvent;
import org.bacon.vanishPlusPlusPlus.VanishManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public final class AdvancementListener implements Listener {
    private final VanishManager vanishManager;

    public AdvancementListener(VanishManager vanishManager) {
        this.vanishManager = vanishManager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onAdvancementCriterionGrant(PlayerAdvancementCriterionGrantEvent event) {
        if (vanishManager.isVanished(event.getPlayer()) && vanishManager.getRules(event.getPlayer()).blockAdvancements()) {
            event.setCancelled(true);
        }
    }
}
