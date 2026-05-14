package org.bacon.vanishPlusPlusPlus.listener;

import org.bacon.vanishPlusPlusPlus.Settings;
import org.bacon.vanishPlusPlusPlus.VanishManager;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.AreaEffectCloudApplyEvent;
import org.bukkit.event.entity.PotionSplashEvent;

import java.util.Iterator;

public final class PotionListener implements Listener {
    private final VanishManager vanishManager;
    private final Settings settings;

    public PotionListener(VanishManager vanishManager, Settings settings) {
        this.vanishManager = vanishManager;
        this.settings = settings;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPotionSplash(PotionSplashEvent event) {
        if (!settings.preventPotionEffects) {
            return;
        }
        for (LivingEntity entity : event.getAffectedEntities()) {
            if (entity instanceof Player player && vanishManager.isVanished(player)) {
                if (!vanishManager.getRules(player).canReceivePotionEffects()) {
                    event.setIntensity(entity, 0.0);
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onAreaEffectCloud(AreaEffectCloudApplyEvent event) {
        if (!settings.preventPotionEffects) {
            return;
        }
        Iterator<LivingEntity> iterator = event.getAffectedEntities().iterator();
        while (iterator.hasNext()) {
            LivingEntity entity = iterator.next();
            if (entity instanceof Player player && vanishManager.isVanished(player)) {
                if (!vanishManager.getRules(player).canReceivePotionEffects()) {
                    iterator.remove();
                }
            }
        }
    }
}
