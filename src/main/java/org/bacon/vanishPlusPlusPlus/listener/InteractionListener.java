package org.bacon.vanishPlusPlusPlus.listener;

import org.bacon.vanishPlusPlusPlus.Settings;
import org.bacon.vanishPlusPlusPlus.VanishManager;
import org.bacon.vanishPlusPlusPlus.model.VanishRules;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.EnderChest;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityInteractEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

public final class InteractionListener implements Listener {
    private final VanishManager vanishManager;
    private final Settings settings;

    public InteractionListener(VanishManager vanishManager, Settings settings) {
        this.vanishManager = vanishManager;
        this.settings = settings;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (!vanishManager.isVanished(player)) {
            return;
        }
        VanishRules rules = vanishManager.getRules(player);
        if (!rules.canBreakBlocks()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (!vanishManager.isVanished(player)) {
            return;
        }
        VanishRules rules = vanishManager.getRules(player);
        if (!rules.canPlaceBlocks()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (!vanishManager.isVanished(player)) {
            return;
        }
        VanishRules rules = vanishManager.getRules(player);
        if (!rules.canDropItems()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof Player player)) {
            return;
        }
        if (!vanishManager.isVanished(player)) {
            return;
        }
        VanishRules rules = vanishManager.getRules(player);
        if (!rules.canPickupItems()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!vanishManager.isVanished(player)) {
            return;
        }
        VanishRules rules = vanishManager.getRules(player);
        Action action = event.getAction();

        if (action == Action.PHYSICAL && settings.disableBlockTriggering) {
            if (!rules.canTriggerWorldEvents()) {
                event.setCancelled(true);
            }
            return;
        }

        if (action == Action.RIGHT_CLICK_BLOCK || action == Action.RIGHT_CLICK_AIR) {
            if (action == Action.RIGHT_CLICK_BLOCK && settings.silentChests) {
                Block block = event.getClickedBlock();
                if (block != null && isSilentContainer(block.getType())) {
                    if (rules.canOpenContainersSilently()) {
                        event.setCancelled(true);
                        openSilentInventory(player, block);
                        return;
                    }
                    if (!rules.canInteract()) {
                        event.setCancelled(true);
                    }
                    return;
                }
            }
            ItemStack item = event.getItem();
            if (!rules.canUseVisibleItems() && item != null && isVisibleUseItem(item.getType())) {
                event.setCancelled(true);
                return;
            }
            if (!rules.canInteract()) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        if (!vanishManager.isVanished(player)) {
            return;
        }
        VanishRules rules = vanishManager.getRules(player);
        if (!rules.canInteract()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (!vanishManager.isVanished(player)) {
            return;
        }
        if (!settings.disableBlockTriggering) {
            return;
        }
        VanishRules rules = vanishManager.getRules(player);
        if (!rules.canTriggerWorldEvents()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityInteract(EntityInteractEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (!vanishManager.isVanished(player)) {
            return;
        }
        if (!settings.disableBlockTriggering) {
            return;
        }
        VanishRules rules = vanishManager.getRules(player);
        if (!rules.canTriggerWorldEvents()) {
            event.setCancelled(true);
        }
    }

    private boolean isSilentContainer(Material type) {
        return type == Material.CHEST
            || type == Material.TRAPPED_CHEST
            || type == Material.BARREL
            || type == Material.ENDER_CHEST
            || type.name().endsWith("SHULKER_BOX");
    }

    private boolean isVisibleUseItem(Material type) {
        return type == Material.FIREWORK_ROCKET
            || type == Material.SPLASH_POTION
            || type == Material.LINGERING_POTION
            || type == Material.ENDER_PEARL
            || type == Material.SNOWBALL
            || type == Material.EGG
            || type == Material.EXPERIENCE_BOTTLE
            || type == Material.WIND_CHARGE
            || type == Material.BOW
            || type == Material.CROSSBOW
            || type == Material.TRIDENT;
    }

    private void openSilentInventory(Player player, Block block) {
        vanishManager.recordSilentContainer(player, block);
        BlockState state = block.getState();
        if (state instanceof EnderChest) {
            player.openInventory(player.getEnderChest());
            return;
        }
        if (state instanceof InventoryHolder holder) {
            Inventory inventory = holder.getInventory();
            player.openInventory(inventory);
        }
    }
}
