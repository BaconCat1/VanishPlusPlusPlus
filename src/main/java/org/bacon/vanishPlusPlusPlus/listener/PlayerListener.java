package org.bacon.vanishPlusPlusPlus.listener;

import org.bacon.vanishPlusPlusPlus.Settings;
import org.bacon.vanishPlusPlusPlus.Permissions;
import org.bacon.vanishPlusPlusPlus.VanishManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class PlayerListener implements Listener {
    private final JavaPlugin plugin;
    private final VanishManager vanishManager;
    private final Settings settings;

    public PlayerListener(JavaPlugin plugin, VanishManager vanishManager, Settings settings) {
        this.plugin = plugin;
        this.vanishManager = vanishManager;
        this.settings = settings;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        vanishManager.handleJoin(event.getPlayer());
        vanishManager.updateVisibilityForAll();
        if (settings.hideRealJoinMessages && vanishManager.isVanished(event.getPlayer())) {
            event.joinMessage(null);
        }
        if (plugin.getConfig().getBoolean("warnings.protocollib-missing", true)
            && event.getPlayer().hasPermission(Permissions.IGNORE_WARNING)) {
            PluginManager pluginManager = event.getPlayer().getServer().getPluginManager();
            if (pluginManager.getPlugin("ProtocolLib") == null) {
                event.getPlayer().sendMessage("§eProtocolLib not found. Packet-level hiding will be best-effort. Use /vignore to silence this warning.");
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if (settings.hideRealQuitMessages && vanishManager.isVanished(event.getPlayer())) {
            event.quitMessage(null);
        }
        vanishManager.handleQuit(event.getPlayer());
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        vanishManager.updatePlayerWorld(event.getPlayer());
    }
}
