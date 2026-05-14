package org.bacon.vanishPlusPlusPlus;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bacon.vanishPlusPlusPlus.command.VanishChatCommand;
import org.bacon.vanishPlusPlusPlus.command.VanishCommand;
import org.bacon.vanishPlusPlusPlus.command.VanishIgnoreCommand;
import org.bacon.vanishPlusPlusPlus.command.VanishListCommand;
import org.bacon.vanishPlusPlusPlus.command.VanishPickupCommand;
import org.bacon.vanishPlusPlusPlus.command.VanishRulesCommand;
import org.bacon.vanishPlusPlusPlus.listener.ChatListener;
import org.bacon.vanishPlusPlusPlus.listener.EntityListener;
import org.bacon.vanishPlusPlusPlus.listener.GameEventListener;
import org.bacon.vanishPlusPlusPlus.listener.InventoryListener;
import org.bacon.vanishPlusPlusPlus.listener.InteractionListener;
import org.bacon.vanishPlusPlusPlus.listener.MessageCommandListener;
import org.bacon.vanishPlusPlusPlus.listener.PlayerListener;
import org.bacon.vanishPlusPlusPlus.listener.PotionListener;
import org.bacon.vanishPlusPlusPlus.listener.RaidListener;
import org.bacon.vanishPlusPlusPlus.listener.ServerListListener;
import org.bacon.vanishPlusPlusPlus.listener.SpectatorToggleListener;
import org.bacon.vanishPlusPlusPlus.listener.TabCompleteListener;
import org.bacon.vanishPlusPlusPlus.listener.AdvancementListener;
import org.bacon.vanishPlusPlusPlus.model.PlayerData;
import org.bacon.vanishPlusPlusPlus.storage.DataStore;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.UUID;

public final class VanishPlusPlusPlus extends JavaPlugin {
    private Settings settings;
    private VanishManager vanishManager;
    private DataStore dataStore;
    private Object protocolLibHook;
    private Object luckPermsHook;
    private org.bacon.vanishPlusPlusPlus.hook.TabHook tabHook;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        settings = Settings.from(getConfig());
        dataStore = new DataStore(this);
        Map<UUID, PlayerData> data = dataStore.load(settings.defaultRules);
        vanishManager = new VanishManager(this, settings, dataStore, data);

        registerCommands();
        registerListeners();
        registerHooks();
        for (Player player : Bukkit.getOnlinePlayers()) {
            vanishManager.handleJoin(player);
        }
        vanishManager.updateVisibilityForAll();
        startHeartbeat();
        startActionBar();
        warnMissingDependencies();
    }

    @Override
    public void onDisable() {
        if (vanishManager != null) {
            vanishManager.saveNow();
        }
        if (protocolLibHook != null) {
            disableHook(protocolLibHook);
        }
        if (luckPermsHook != null) {
            disableHook(luckPermsHook);
        }
    }

    private void disableHook(Object hook) {
        try {
            hook.getClass().getMethod("disable").invoke(hook);
        } catch (ReflectiveOperationException ignored) {
            // Best-effort only
        }
    }

    private void registerCommands() {
        register("vanish", new VanishCommand(vanishManager, settings));
        register("vanishrules", new VanishRulesCommand(vanishManager, settings));
        register("vanishchat", new VanishChatCommand(vanishManager, settings));
        register("vanishpickup", new VanishPickupCommand(vanishManager, settings));
        register("vanishignore", new VanishIgnoreCommand(this, settings));
        register("vlist", new VanishListCommand(vanishManager, settings));
    }

    private void register(String name, org.bukkit.command.CommandExecutor executor) {
        PluginCommand command = getCommand(name);
        if (command != null) {
            command.setExecutor(executor);
            if (executor instanceof org.bukkit.command.TabCompleter completer) {
                command.setTabCompleter(completer);
            }
        }
    }

    private void registerListeners() {
        PluginManager manager = getServer().getPluginManager();
        manager.registerEvents(new PlayerListener(this, vanishManager, settings), this);
        manager.registerEvents(new ChatListener(this, vanishManager, settings), this);
        manager.registerEvents(new InteractionListener(vanishManager, settings), this);
        manager.registerEvents(new InventoryListener(vanishManager), this);
        manager.registerEvents(new PotionListener(vanishManager, settings), this);
        manager.registerEvents(new EntityListener(vanishManager, settings), this);
        manager.registerEvents(new RaidListener(vanishManager, settings), this);
        manager.registerEvents(new GameEventListener(vanishManager, settings), this);
        manager.registerEvents(new TabCompleteListener(vanishManager, settings), this);
        manager.registerEvents(new ServerListListener(vanishManager, settings), this);
        manager.registerEvents(new MessageCommandListener(vanishManager, settings), this);
        manager.registerEvents(new SpectatorToggleListener(vanishManager), this);
        manager.registerEvents(new AdvancementListener(vanishManager), this);
    }

    private void registerHooks() {
        tabHook = new org.bacon.vanishPlusPlusPlus.hook.TabHook(this, settings);
        tabHook.enable();
        vanishManager.setTabHook(tabHook.isActive() ? tabHook : null);
        PluginManager manager = getServer().getPluginManager();

        if (manager.getPlugin("ProtocolLib") != null) {
            try {
                Class<?> hookClass = Class.forName("org.bacon.vanishPlusPlusPlus.hook.ProtocolLibHook");
                Object hook = hookClass.getConstructor(JavaPlugin.class, VanishManager.class, Settings.class)
                    .newInstance(this, vanishManager, settings);
                hookClass.getMethod("enable").invoke(hook);
                protocolLibHook = hook;
            } catch (Exception e) {
                getLogger().warning("Failed to enable ProtocolLib hook: " + e.getMessage());
            }
        }

        if (manager.getPlugin("LuckPerms") != null) {
            try {
                Class<?> hookClass = Class.forName("org.bacon.vanishPlusPlusPlus.hook.LuckPermsHook");
                Object hook = hookClass.getConstructor(JavaPlugin.class, VanishManager.class).newInstance(this, vanishManager);
                hookClass.getMethod("enable").invoke(hook);
                luckPermsHook = hook;
            } catch (Exception e) {
                getLogger().warning("Failed to enable LuckPerms hook: " + e.getMessage());
            }
        }

        if (manager.getPlugin("voicechat") != null) {
            try {
                Class<?> hookClass = Class.forName("org.bacon.vanishPlusPlusPlus.hook.VoiceChatHook");
                hookClass.getMethod("register", JavaPlugin.class, VanishManager.class, Settings.class)
                    .invoke(null, this, vanishManager, settings);
            } catch (Exception e) {
                getLogger().warning("Failed to enable Simple Voice Chat hook: " + e.getMessage());
            }
        }
    }

    private void startHeartbeat() {
        long interval = settings.heartbeatSeconds * 20L;
        getServer().getGlobalRegionScheduler().runAtFixedRate(this, task -> {
            vanishManager.updateVisibilityForAll();
        }, interval, interval);
    }

    private void startActionBar() {
        if (!settings.actionBarEnabled) {
            return;
        }
        Component actionBar = LegacyComponentSerializer.legacySection().deserialize(settings.actionBarText);
        long interval = 40L;
        getServer().getGlobalRegionScheduler().runAtFixedRate(this, task -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.getScheduler().run(this, scheduledTask -> {
                    if (vanishManager.isVanished(player)) {
                        player.sendActionBar(actionBar);
                    }
                }, () -> {});
            }
        }, interval, interval);
    }

    private void warnMissingDependencies() {
        if (!settings.warnProtocolLibMissing) {
            return;
        }
        if (getServer().getPluginManager().getPlugin("ProtocolLib") == null) {
            getLogger().warning("ProtocolLib not found. Packet-level hiding (tab-complete, server list, nametag packets) will be best-effort.");
        }
    }
}
