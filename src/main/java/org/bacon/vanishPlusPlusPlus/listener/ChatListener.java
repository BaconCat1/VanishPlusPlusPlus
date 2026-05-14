package org.bacon.vanishPlusPlusPlus.listener;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bacon.vanishPlusPlusPlus.Settings;
import org.bacon.vanishPlusPlusPlus.VanishManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bacon.vanishPlusPlusPlus.model.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ChatListener implements Listener {
    private final VanishManager vanishManager;
    private final Settings settings;
    private final JavaPlugin plugin;
    private final Map<UUID, RecentChat> recentlyAllowed = new ConcurrentHashMap<>();
    private final Map<UUID, RecentChat> recentlyBlocked = new ConcurrentHashMap<>();

    public ChatListener(JavaPlugin plugin, VanishManager vanishManager, Settings settings) {
        this.plugin = plugin;
        this.vanishManager = vanishManager;
        this.settings = settings;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        String message = PlainTextComponentSerializer.plainText().serialize(event.message());
        if (shouldCancelChat(player, message)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    @SuppressWarnings("deprecation")
    public void onLegacyChat(AsyncPlayerChatEvent event) {
        if (shouldCancelChat(event.getPlayer(), event.getMessage())) {
            event.setCancelled(true);
        }
    }

    private boolean shouldCancelChat(Player player, String message) {
        UUID playerId = player.getUniqueId();
        if (!vanishManager.isVanished(player)) {
            return false;
        }
        if (matchesRecent(recentlyAllowed.get(playerId), message)) {
            recentlyAllowed.remove(playerId);
            return false;
        }
        if (matchesRecent(recentlyBlocked.get(playerId), message)) {
            recentlyBlocked.remove(playerId);
            return true;
        }
        PlayerData data = vanishManager.getData(player);
        if (data.isAllowNextChat()) {
            data.setAllowNextChat(false);
            data.setPendingChatMessage(null);
            recentlyAllowed.put(playerId, new RecentChat(message, System.currentTimeMillis()));
            return false;
        }
        boolean hasPermission = vanishManager.canChat(player.getUniqueId());
        boolean ruleAllows = data.getRules().canChat();
        if (!hasPermission) {
            player.getScheduler().run(plugin, task -> player.sendMessage(settings.msgNoPermission), () -> {});
            recentlyBlocked.put(playerId, new RecentChat(message, System.currentTimeMillis()));
            return true;
        }
        if (ruleAllows) {
            return false;
        }
        data.setPendingChatMessage(message);
        player.getScheduler().run(plugin, task -> player.sendMessage(settings.msgChatLocked), () -> {});
        recentlyBlocked.put(playerId, new RecentChat(message, System.currentTimeMillis()));
        return true;
    }

    private boolean matchesRecent(RecentChat recent, String message) {
        if (recent == null) {
            return false;
        }
        return System.currentTimeMillis() - recent.createdAtMillis <= 1000L && recent.message.equals(message);
    }

    private record RecentChat(String message, long createdAtMillis) {
    }
}
