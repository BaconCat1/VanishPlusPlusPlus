package org.bacon.vanishPlusPlusPlus.listener;

import org.bacon.vanishPlusPlusPlus.Settings;
import org.bacon.vanishPlusPlusPlus.VanishManager;
import org.bacon.vanishPlusPlusPlus.model.VanishRules;
import org.bukkit.Location;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.ThrowableProjectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.vehicle.VehicleEntityCollisionEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.util.Vector;

public final class EntityListener implements Listener {
    private final VanishManager vanishManager;
    private final Settings settings;

    public EntityListener(VanishManager vanishManager, Settings settings) {
        this.vanishManager = vanishManager;
        this.settings = settings;
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        Entity target = event.getEntity();
        if (target instanceof Player player && vanishManager.isVanished(player)) {
            event.setCancelled(true);
            return;
        }

        Entity damager = event.getDamager();
        Player sourcePlayer = null;
        if (damager instanceof Player player) {
            sourcePlayer = player;
        } else if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Player shooter) {
            sourcePlayer = shooter;
        }

        if (sourcePlayer != null && vanishManager.isVanished(sourcePlayer)) {
            VanishRules rules = vanishManager.getRules(sourcePlayer);
            if (!rules.canHitEntities()) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onProjectileHit(ProjectileHitEvent event) {
        if (event.isCancelled()) {
            return;
        }
        if (!(event.getHitEntity() instanceof Player player)) {
            return;
        }
        if (!vanishManager.isVanished(player)) {
            return;
        }
        if (!settings.ignoreProjectiles) {
            return;
        }
        event.setCancelled(true);
        respawnProjectile(event.getEntity());
    }

    @EventHandler(ignoreCancelled = true)
    public void onTarget(EntityTargetLivingEntityEvent event) {
        if (!(event.getTarget() instanceof Player player)) {
            return;
        }
        if (!vanishManager.isVanished(player)) {
            return;
        }
        if (!settings.disableMobTargeting) {
            return;
        }
        if (!vanishManager.getRules(player).canBeTargetedByMobs()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onVehicleCollision(VehicleEntityCollisionEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof Player player && vanishManager.isVanished(player)) {
            event.setCancelled(true);
        }
    }

    @SuppressWarnings("removal")
    private void respawnProjectile(Projectile projectile) {
        Location location = projectile.getLocation().clone();
        Vector velocity = projectile.getVelocity();
        if (velocity.lengthSquared() > 0.0001) {
            location.add(velocity.clone().normalize().multiply(0.15));
        }
        Projectile newProjectile = (Projectile) projectile.getWorld().spawnEntity(location, projectile.getType());
        newProjectile.setVelocity(velocity);
        newProjectile.setShooter(projectile.getShooter());
        if (projectile instanceof AbstractArrow arrow && newProjectile instanceof AbstractArrow newArrow) {
            newArrow.setCritical(arrow.isCritical());
            newArrow.setDamage(arrow.getDamage());
            newArrow.setKnockbackStrength(arrow.getKnockbackStrength());
            newArrow.setPierceLevel(arrow.getPierceLevel());
            newArrow.setPickupStatus(arrow.getPickupStatus());
            newArrow.setFireTicks(arrow.getFireTicks());
        }
        if (projectile instanceof Arrow arrow && newProjectile instanceof Arrow newArrow) {
            newArrow.setBasePotionData(arrow.getBasePotionData());
            newArrow.setBasePotionType(arrow.getBasePotionType());
            if (arrow.getColor() != null) {
                newArrow.setColor(arrow.getColor());
            }
            newArrow.clearCustomEffects();
            for (PotionEffect effect : arrow.getCustomEffects()) {
                newArrow.addCustomEffect(effect, true);
            }
        }
        if (projectile instanceof ThrowableProjectile throwable && newProjectile instanceof ThrowableProjectile newThrowable) {
            ItemStack item = throwable.getItem();
            if (item != null) {
                newThrowable.setItem(item);
            }
        }
        projectile.remove();
    }
}
