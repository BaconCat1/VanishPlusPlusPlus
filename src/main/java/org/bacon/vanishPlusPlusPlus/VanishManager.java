package org.bacon.vanishPlusPlusPlus;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bacon.vanishPlusPlusPlus.model.PlayerData;
import org.bacon.vanishPlusPlusPlus.model.VanishRules;
import org.bacon.vanishPlusPlusPlus.storage.DataStore;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public final class VanishManager {
    private final JavaPlugin plugin;
    private final Settings settings;
    private final DataStore dataStore;
    private final Map<UUID, PlayerData> data;
    private final Set<String> vanishedNames;
    private final Set<String> vanishedTokens;
    private final Set<UUID> vanishedIds;
    private final Set<UUID> seePermissionCache;
    private final Set<UUID> chatPermissionCache;
    private final Set<UUID> vanishPermissionCache;
    private final Set<UUID> vanishOthersPermissionCache;
    private final Set<UUID> rulesPermissionCache;
    private final Set<UUID> rulesOthersPermissionCache;
    private final Set<UUID> pickupPermissionCache;
    private final Set<UUID> pickupOthersPermissionCache;
    private final Set<UUID> ignoreWarningPermissionCache;
    private final Set<UUID> tabCompleteMessageBypassPermissionCache;
    private final Set<UUID> listPermissionCache;
    private final Set<UUID> hiddenTabListPlayerIds;
    private final Set<String> hiddenTabListNames;
    private final Map<UUID, UUID> playerWorlds;
    private final Map<UUID, String> playerNames;
    private final AtomicBoolean saveQueued = new AtomicBoolean(false);
    private volatile org.bacon.vanishPlusPlusPlus.hook.TabHook tabHook;
    private volatile NametagHandler nametagHandler;
    private volatile PlayerListHandler playerListHandler;
    private final Map<BlockKey, SilentContainer> silentContainers;
    private final Map<UUID, BlockKey> silentByPlayer;
    private final Method listPlayerMethod;
    private final Method unlistPlayerMethod;

    public VanishManager(JavaPlugin plugin, Settings settings, DataStore dataStore, Map<UUID, PlayerData> initialData) {
        this.plugin = plugin;
        this.settings = settings;
        this.dataStore = dataStore;
        this.data = new ConcurrentHashMap<>(initialData);
        this.vanishedNames = ConcurrentHashMap.newKeySet();
        this.vanishedTokens = ConcurrentHashMap.newKeySet();
        this.vanishedIds = ConcurrentHashMap.newKeySet();
        this.seePermissionCache = ConcurrentHashMap.newKeySet();
        this.chatPermissionCache = ConcurrentHashMap.newKeySet();
        this.vanishPermissionCache = ConcurrentHashMap.newKeySet();
        this.vanishOthersPermissionCache = ConcurrentHashMap.newKeySet();
        this.rulesPermissionCache = ConcurrentHashMap.newKeySet();
        this.rulesOthersPermissionCache = ConcurrentHashMap.newKeySet();
        this.pickupPermissionCache = ConcurrentHashMap.newKeySet();
        this.pickupOthersPermissionCache = ConcurrentHashMap.newKeySet();
        this.ignoreWarningPermissionCache = ConcurrentHashMap.newKeySet();
        this.tabCompleteMessageBypassPermissionCache = ConcurrentHashMap.newKeySet();
        this.listPermissionCache = ConcurrentHashMap.newKeySet();
        this.hiddenTabListPlayerIds = ConcurrentHashMap.newKeySet();
        this.hiddenTabListNames = ConcurrentHashMap.newKeySet();
        this.playerWorlds = new ConcurrentHashMap<>();
        this.playerNames = new ConcurrentHashMap<>();
        this.silentContainers = new ConcurrentHashMap<>();
        this.silentByPlayer = new ConcurrentHashMap<>();
        Method listMethod = null;
        Method unlistMethod = null;
        try {
            listMethod = Player.class.getMethod("listPlayer", Player.class);
        } catch (NoSuchMethodException ignored) {
            // Older API, tab list handled elsewhere.
        }
        try {
            unlistMethod = Player.class.getMethod("unlistPlayer", Player.class);
        } catch (NoSuchMethodException ignored) {
            // Older API, tab list handled elsewhere.
        }
        this.listPlayerMethod = listMethod;
        this.unlistPlayerMethod = unlistMethod;
        for (Map.Entry<UUID, PlayerData> entry : data.entrySet()) {
            if (entry.getValue().isVanished()) {
                Player player = Bukkit.getPlayer(entry.getKey());
                if (player != null) {
                    vanishedNames.add(player.getName().toLowerCase());
                    vanishedIds.add(entry.getKey());
                    playerNames.put(entry.getKey(), player.getName());
                }
            }
        }
        rebuildVanishedVisibilityCaches(Bukkit.getOnlinePlayers().toArray(new Player[0]));
    }

    public boolean isVanished(Player player) {
        return getData(player).isVanished();
    }

    public boolean isVanished(UUID uuid) {
        return vanishedIds.contains(uuid);
    }

    public PlayerData getData(Player player) {
        return data.computeIfAbsent(player.getUniqueId(), ignored -> new PlayerData(settings.defaultRules.copy()));
    }

    public VanishRules getRules(Player player) {
        return getData(player).getRules();
    }

    public Set<String> getVanishedNames() {
        return vanishedNames;
    }

    public Set<String> getVanishedTokens() {
        return vanishedTokens;
    }

    public Set<UUID> getVanishedIds() {
        return vanishedIds;
    }

    public int getHiddenTabListCount() {
        return hiddenTabListPlayerIds.size();
    }

    public Set<UUID> getHiddenTabListPlayerIds() {
        return hiddenTabListPlayerIds;
    }

    public Set<String> getHiddenTabListNames() {
        return hiddenTabListNames;
    }

    public boolean canSee(UUID viewerId) {
        return seePermissionCache.contains(viewerId);
    }

    public boolean canSeeVanishedInTabComplete(UUID viewerId) {
        return seePermissionCache.contains(viewerId) || tabCompleteMessageBypassPermissionCache.contains(viewerId);
    }

    public boolean canChat(UUID playerId) {
        return chatPermissionCache.contains(playerId);
    }

    public boolean canUseVanish(UUID playerId) {
        return vanishPermissionCache.contains(playerId) || vanishOthersPermissionCache.contains(playerId);
    }

    public boolean canUseRules(UUID playerId) {
        return rulesPermissionCache.contains(playerId) || rulesOthersPermissionCache.contains(playerId);
    }

    public boolean canUsePickup(UUID playerId) {
        return pickupPermissionCache.contains(playerId) || pickupOthersPermissionCache.contains(playerId);
    }

    public boolean canIgnoreWarning(UUID playerId) {
        return ignoreWarningPermissionCache.contains(playerId);
    }

    public boolean canUseList(UUID playerId) {
        return listPermissionCache.contains(playerId);
    }

    public void setTabHook(org.bacon.vanishPlusPlusPlus.hook.TabHook tabHook) {
        this.tabHook = tabHook;
    }

    public void setNametagHandler(NametagHandler nametagHandler) {
        this.nametagHandler = nametagHandler;
    }

    public void setPlayerListHandler(PlayerListHandler playerListHandler) {
        this.playerListHandler = playerListHandler;
    }

    public void handleJoin(Player player) {
        updatePlayerWorld(player);
        playerNames.put(player.getUniqueId(), player.getName());
        refreshPermissionCache(player);
        PlayerData playerData = getData(player);
        if (playerData.isVanished()) {
            applyVanish(player, false);
        } else {
            clearVanishState(player);
        }
    }

    public void handleQuit(Player player) {
        queueSave();
        vanishedNames.remove(player.getName().toLowerCase());
        vanishedIds.remove(player.getUniqueId());
        seePermissionCache.remove(player.getUniqueId());
        chatPermissionCache.remove(player.getUniqueId());
        vanishPermissionCache.remove(player.getUniqueId());
        vanishOthersPermissionCache.remove(player.getUniqueId());
        rulesPermissionCache.remove(player.getUniqueId());
        rulesOthersPermissionCache.remove(player.getUniqueId());
        pickupPermissionCache.remove(player.getUniqueId());
        pickupOthersPermissionCache.remove(player.getUniqueId());
        ignoreWarningPermissionCache.remove(player.getUniqueId());
        tabCompleteMessageBypassPermissionCache.remove(player.getUniqueId());
        listPermissionCache.remove(player.getUniqueId());
        hiddenTabListPlayerIds.remove(player.getUniqueId());
        hiddenTabListNames.remove(player.getName().toLowerCase(java.util.Locale.ROOT));
        playerWorlds.remove(player.getUniqueId());
        playerNames.remove(player.getUniqueId());
        NametagHandler handler = nametagHandler;
        if (handler != null) {
            handler.forget(player.getUniqueId());
        }
        PlayerListHandler listHandler = playerListHandler;
        if (listHandler != null) {
            listHandler.forget(player.getUniqueId());
        }
        clearSilentContainer(player.getUniqueId());
        rebuildVanishedVisibilityCaches();
    }

    public void updatePlayerWorld(Player player) {
        playerWorlds.put(player.getUniqueId(), player.getWorld().getUID());
    }

    public UUID getPlayerWorld(UUID playerId) {
        return playerWorlds.get(playerId);
    }

    public void toggleVanish(Player target) {
        boolean next = !isVanished(target);
        if (next) {
            applyVanish(target, true);
        } else {
            removeVanish(target, true);
        }
    }

    public void applyVanish(Player target, boolean broadcast) {
        PlayerData playerData = getData(target);
        playerData.setVanished(true);
        playerData.clearPendingChat();
        vanishedNames.add(target.getName().toLowerCase());
        vanishedIds.add(target.getUniqueId());
        rebuildVanishedVisibilityCaches();

        runOnEntity(target, () -> {
            target.setInvisible(false);
            target.setCollidable(!settings.zeroCollision);
            target.setCanPickupItems(playerData.getRules().canPickupItems());
            applyListName(target, true);
            if (tabHook != null) {
                tabHook.apply(target, true);
            }
        });

        updateVisibilityForAll();
        if (broadcast && settings.broadcastFakeMessages && playerData.getRules().broadcastFakeMessages()) {
            broadcastFakeQuit(target);
        }
        queueSave();
    }

    public void removeVanish(Player target, boolean broadcast) {
        PlayerData playerData = getData(target);
        playerData.setVanished(false);
        playerData.clearPendingChat();
        vanishedNames.remove(target.getName().toLowerCase());
        vanishedIds.remove(target.getUniqueId());
        rebuildVanishedVisibilityCaches();

        runOnEntity(target, () -> {
            target.setInvisible(false);
            target.setCollidable(true);
            target.setCanPickupItems(true);
            if (playerData.getRules().forceSurvivalOnUnvanish() && target.getGameMode() == GameMode.SPECTATOR) {
                target.setGameMode(GameMode.SURVIVAL);
            }
            applyListName(target, false);
            if (tabHook != null) {
                tabHook.apply(target, false);
            }
        });

        updateVisibilityForAll();
        if (broadcast && settings.broadcastFakeMessages && playerData.getRules().broadcastFakeMessages()) {
            broadcastFakeJoin(target);
        }
        queueSave();
    }

    public void updateVisibilityForAll() {
        plugin.getServer().getGlobalRegionScheduler().run(plugin, task -> {
            Player[] snapshot = Bukkit.getOnlinePlayers().toArray(new Player[0]);
            Set<String> vanishedEntries = buildVanishedEntries();
            rebuildVanishedVisibilityCaches(snapshot);
            for (Player viewer : snapshot) {
                refreshViewer(viewer, snapshot, vanishedEntries);
            }
        });
    }

    public void refreshViewer(Player viewer, Player[] snapshot, Set<String> vanishedEntries) {
        runOnEntity(viewer, () -> {
            refreshPermissionCache(viewer);
            for (Player target : snapshot) {
                if (viewer.equals(target)) {
                    continue;
                }
                if (isVanished(target)) {
                    if (viewer.hasPermission(Permissions.SEE)) {
                        viewer.showPlayer(plugin, target);
                        applyPlayerListVisibility(viewer, target, true);
                    } else {
                        viewer.hidePlayer(plugin, target);
                        applyPlayerListVisibility(viewer, target, shouldShowInPublicTabList(target));
                    }
                } else {
                    viewer.showPlayer(plugin, target);
                    applyPlayerListVisibility(viewer, target, true);
                }
            }
            updateNametagTeam(viewer, vanishedEntries);
        });
    }

    public void applyPickupSetting(Player player) {
        boolean canPickup = !isVanished(player) || getRules(player).canPickupItems();
        runOnEntity(player, () -> player.setCanPickupItems(canPickup));
    }

    public void refreshListName(Player player) {
        runOnEntity(player, () -> applyListName(player, isVanished(player)));
    }

    public void toggleSpectatorWhileVanished(Player player) {
        if (!isVanished(player)) {
            return;
        }
        runOnEntity(player, () -> {
            if (!isVanished(player) || !getRules(player).doubleSneakSpectator()) {
                return;
            }
            GameMode nextMode = player.getGameMode() == GameMode.SPECTATOR ? GameMode.SURVIVAL : GameMode.SPECTATOR;
            player.setGameMode(nextMode);
        });
    }

    public void enforceVanishState(Player player) {
        runOnEntity(player, () -> {
            if (!isVanished(player)) {
                return;
            }
            VanishRules rules = getRules(player);
            if (!rules.doubleSneakSpectator() && player.getGameMode() == GameMode.SPECTATOR) {
                player.setGameMode(GameMode.SURVIVAL);
            }
        });
    }

    public void queueSave() {
        if (!saveQueued.compareAndSet(false, true)) {
            return;
        }
        plugin.getServer().getAsyncScheduler().runNow(plugin, task -> {
            saveQueued.set(false);
            dataStore.saveSync(data);
        });
    }

    public void saveNow() {
        dataStore.saveSync(data);
    }

    public void broadcastFakeQuit(Player target) {
        Component message = buildFakeMessage(settings.fakeQuitMessage, target, "multiplayer.player.left");
        UUID targetId = target.getUniqueId();
        plugin.getServer().getGlobalRegionScheduler().run(plugin, task -> {
            for (Player viewer : Bukkit.getOnlinePlayers()) {
                viewer.getScheduler().run(plugin, scheduled -> {
                    if (!viewer.getUniqueId().equals(targetId) && !viewer.hasPermission(Permissions.SEE)) {
                        viewer.sendMessage(message);
                    }
                }, () -> {});
            }
        });
    }

    public void broadcastFakeJoin(Player target) {
        Component message = buildFakeMessage(settings.fakeJoinMessage, target, "multiplayer.player.joined");
        UUID targetId = target.getUniqueId();
        plugin.getServer().getGlobalRegionScheduler().run(plugin, task -> {
            for (Player viewer : Bukkit.getOnlinePlayers()) {
                viewer.getScheduler().run(plugin, scheduled -> {
                    if (!viewer.getUniqueId().equals(targetId) && !viewer.hasPermission(Permissions.SEE)) {
                        viewer.sendMessage(message);
                    }
                }, () -> {});
            }
        });
    }

    private void applyListName(Player player, boolean vanished) {
        PlayerData playerData = getData(player);
        if (vanished) {
            if (playerData.getOriginalListName() == null) {
                Component current = player.playerListName();
                playerData.setOriginalListName(LegacyComponentSerializer.legacySection().serialize(current));
            }
            if (playerData.getRules().showInTabListAndPlayerCount()) {
                if (playerData.getOriginalListName() != null) {
                    Component restored = LegacyComponentSerializer.legacySection().deserialize(playerData.getOriginalListName());
                    player.playerListName(restored);
                } else {
                    player.playerListName(null);
                }
            } else {
                Component prefix = LegacyComponentSerializer.legacySection().deserialize(settings.tabPrefix);
                Component name = Component.text(player.getName(), NamedTextColor.GRAY, TextDecoration.ITALIC);
                player.playerListName(prefix.append(name));
            }
        } else {
            if (playerData.getOriginalListName() != null) {
                Component restored = LegacyComponentSerializer.legacySection().deserialize(playerData.getOriginalListName());
                player.playerListName(restored);
                playerData.setOriginalListName(null);
            } else {
                player.playerListName(null);
            }
        }
    }

    private void clearVanishState(Player player) {
        runOnEntity(player, () -> {
            player.setInvisible(false);
            player.setCollidable(true);
            applyListName(player, false);
        });
    }

    private Component buildFakeMessage(String template, Player target, String fallbackKey) {
        if (template == null) {
            return Component.translatable(fallbackKey, Component.text(target.getName()));
        }
        String trimmed = template.trim();
        if (trimmed.isEmpty() || trimmed.equalsIgnoreCase("native") || trimmed.equalsIgnoreCase("translatable")) {
            return Component.translatable(fallbackKey, Component.text(target.getName()));
        }
        String resolved = template.replace("%player%", target.getName());
        return LegacyComponentSerializer.legacySection().deserialize(resolved);
    }
    private void applyPlayerListVisibility(Player viewer, Player target, boolean shouldShow) {
        if (!settings.hideFromTabComplete) {
            return;
        }
        PlayerListHandler handler = playerListHandler;
        if (handler == null) {
            if (shouldShow) {
                invokeListPlayer(viewer, target);
            } else {
                invokeUnlistPlayer(viewer, target);
            }
            return;
        }
        if (shouldShow) {
            handler.show(viewer, target);
        } else {
            handler.hide(viewer, target);
        }
    }

    private void invokeListPlayer(Player viewer, Player target) {
        if (listPlayerMethod == null) {
            return;
        }
        try {
            listPlayerMethod.invoke(viewer, target);
        } catch (Exception ignored) {
            // Best-effort only
        }
    }

    private void invokeUnlistPlayer(Player viewer, Player target) {
        if (unlistPlayerMethod == null) {
            return;
        }
        try {
            unlistPlayerMethod.invoke(viewer, target);
        } catch (Exception ignored) {
            // Best-effort only
        }
    }

    private void updateNametagTeam(Player viewer, Set<String> vanishedEntries) {
        NametagHandler handler = nametagHandler;
        if (handler == null) {
            return;
        }
        if (vanishedEntries.isEmpty()) {
            handler.clear(viewer);
            return;
        }
        handler.update(viewer, vanishedEntries, settings.nametagPrefix);
    }

    private void runOnEntity(Player player, Runnable action) {
        player.getScheduler().run(plugin, task -> action.run(), () -> {});
    }

    private Set<String> buildVanishedEntries() {
        Set<String> vanishedEntries = ConcurrentHashMap.newKeySet();
        for (UUID id : vanishedIds) {
            String name = playerNames.get(id);
            if (name != null) {
                vanishedEntries.add(name);
            }
        }
        return vanishedEntries;
    }

    private void rebuildVanishedVisibilityCaches(Player[] snapshot) {
        vanishedTokens.clear();
        hiddenTabListPlayerIds.clear();
        hiddenTabListNames.clear();
        for (Player player : snapshot) {
            if (isVanished(player)) {
                VanishRules rules = getRules(player);
                if (!rules.showInTabComplete()) {
                    addNameTokens(vanishedTokens, player);
                }
                if (!rules.showInTabListAndPlayerCount()) {
                    hiddenTabListPlayerIds.add(player.getUniqueId());
                    hiddenTabListNames.add(player.getName().toLowerCase(java.util.Locale.ROOT));
                }
            }
        }
    }

    private void rebuildVanishedVisibilityCaches() {
        Player[] snapshot = Bukkit.getOnlinePlayers().toArray(new Player[0]);
        rebuildVanishedVisibilityCaches(snapshot);
    }

    private boolean shouldShowInPublicTabList(Player player) {
        if (!isVanished(player)) {
            return true;
        }
        return getRules(player).showInTabListAndPlayerCount();
    }

    private void addNameTokens(Set<String> tokens, Player player) {
        String name = player.getName();
        if (!name.isBlank()) {
            tokens.add(name.toLowerCase(java.util.Locale.ROOT));
        }
        String display = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
            .serialize(player.displayName());
        if (!display.isBlank()) {
            tokens.add(display.toLowerCase(java.util.Locale.ROOT));
        }
        String list = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
            .serialize(player.playerListName());
        if (!list.isBlank()) {
            tokens.add(list.toLowerCase(java.util.Locale.ROOT));
        }
    }

    private void refreshPermissionCache(Player player) {
        UUID id = player.getUniqueId();
        if (player.hasPermission(Permissions.SEE)) {
            seePermissionCache.add(id);
        } else {
            seePermissionCache.remove(id);
        }
        if (player.hasPermission(Permissions.CHAT)) {
            chatPermissionCache.add(id);
        } else {
            chatPermissionCache.remove(id);
        }
        if (player.hasPermission(Permissions.VANISH)) {
            vanishPermissionCache.add(id);
        } else {
            vanishPermissionCache.remove(id);
        }
        if (player.hasPermission(Permissions.VANISH_OTHERS)) {
            vanishOthersPermissionCache.add(id);
        } else {
            vanishOthersPermissionCache.remove(id);
        }
        if (player.hasPermission(Permissions.RULES)) {
            rulesPermissionCache.add(id);
        } else {
            rulesPermissionCache.remove(id);
        }
        if (player.hasPermission(Permissions.RULES_OTHERS)) {
            rulesOthersPermissionCache.add(id);
        } else {
            rulesOthersPermissionCache.remove(id);
        }
        if (player.hasPermission(Permissions.PICKUP)) {
            pickupPermissionCache.add(id);
        } else {
            pickupPermissionCache.remove(id);
        }
        if (player.hasPermission(Permissions.PICKUP_OTHERS)) {
            pickupOthersPermissionCache.add(id);
        } else {
            pickupOthersPermissionCache.remove(id);
        }
        if (player.hasPermission(Permissions.IGNORE_WARNING)) {
            ignoreWarningPermissionCache.add(id);
        } else {
            ignoreWarningPermissionCache.remove(id);
        }
        if (player.hasPermission(Permissions.TAB_COMPLETE_MESSAGE_BYPASS)) {
            tabCompleteMessageBypassPermissionCache.add(id);
        } else {
            tabCompleteMessageBypassPermissionCache.remove(id);
        }
        if (player.hasPermission(Permissions.LIST)) {
            listPermissionCache.add(id);
        } else {
            listPermissionCache.remove(id);
        }
    }

    public void recordSilentContainer(Player player, org.bukkit.block.Block block) {
        clearSilentContainer(player.getUniqueId());
        BlockKey key = BlockKey.from(block);
        silentContainers.computeIfAbsent(key, ignored -> new SilentContainer()).openers.add(player.getUniqueId());
        silentByPlayer.put(player.getUniqueId(), key);
    }

    public boolean isSilentContainer(UUID worldId, int x, int y, int z, UUID viewerId) {
        BlockKey key = new BlockKey(worldId, x, y, z);
        SilentContainer container = silentContainers.get(key);
        if (container == null) {
            return false;
        }
        return !container.openers.contains(viewerId);
    }

    public void clearSilentContainer(UUID playerId) {
        BlockKey key = silentByPlayer.remove(playerId);
        if (key != null) {
            SilentContainer container = silentContainers.get(key);
            if (container != null) {
                container.openers.remove(playerId);
                if (container.openers.isEmpty()) {
                    silentContainers.remove(key);
                }
            }
        }
    }

    private static final class SilentContainer {
        private final Set<UUID> openers = ConcurrentHashMap.newKeySet();
    }

    private record BlockKey(UUID worldId, int x, int y, int z) {
        static BlockKey from(org.bukkit.block.Block block) {
            return new BlockKey(block.getWorld().getUID(), block.getX(), block.getY(), block.getZ());
        }
    }

    public interface NametagHandler {
        void update(Player viewer, Set<String> vanishedEntries, String prefix);

        void clear(Player viewer);

        void forget(UUID viewerId);
    }

    public interface PlayerListHandler {
        void hide(Player viewer, Player target);

        void show(Player viewer, Player target);

        void forget(UUID viewerId);
    }
}
