package org.bacon.vanishPlusPlusPlus.hook;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.event.EventSubscription;
import net.luckperms.api.event.group.GroupDataRecalculateEvent;
import net.luckperms.api.event.user.UserDataRecalculateEvent;
import net.luckperms.api.LuckPermsProvider;
import org.bacon.vanishPlusPlusPlus.VanishManager;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public final class LuckPermsHook {
    private final JavaPlugin plugin;
    private final VanishManager vanishManager;
    private EventSubscription<UserDataRecalculateEvent> userSubscription;
    private EventSubscription<GroupDataRecalculateEvent> groupSubscription;

    public LuckPermsHook(JavaPlugin plugin, VanishManager vanishManager) {
        this.plugin = plugin;
        this.vanishManager = vanishManager;
    }

    public void enable() {
        Plugin luckPermsPlugin = plugin.getServer().getPluginManager().getPlugin("LuckPerms");
        if (luckPermsPlugin == null || !luckPermsPlugin.isEnabled()) {
            return;
        }
        try {
            LuckPerms luckPerms = LuckPermsProvider.get();
            userSubscription = luckPerms.getEventBus().subscribe(plugin, UserDataRecalculateEvent.class, event -> vanishManager.updateVisibilityForAll());
            groupSubscription = luckPerms.getEventBus().subscribe(plugin, GroupDataRecalculateEvent.class, event -> vanishManager.updateVisibilityForAll());
        } catch (IllegalStateException ignored) {
            // LuckPerms not ready
        }
    }

    @SuppressWarnings("unused")
    public void disable() {
        if (userSubscription != null) {
            userSubscription.close();
            userSubscription = null;
        }
        if (groupSubscription != null) {
            groupSubscription.close();
            groupSubscription = null;
        }
    }
}
