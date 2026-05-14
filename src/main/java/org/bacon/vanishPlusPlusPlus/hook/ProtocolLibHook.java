package org.bacon.vanishPlusPlusPlus.hook;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import com.comphenix.protocol.wrappers.WrappedServerPing;
import com.comphenix.protocol.wrappers.WrappedTeamParameters;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import com.comphenix.protocol.wrappers.PlayerInfoData;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bacon.vanishPlusPlusPlus.Settings;
import org.bacon.vanishPlusPlusPlus.VanishManager;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.LinkedHashSet;

@SuppressWarnings("SpellCheckingInspection")
public final class ProtocolLibHook implements VanishManager.NametagHandler, VanishManager.PlayerListHandler {
    private static final String NAMETAG_TEAM = "vppp_nametag";
    private final JavaPlugin plugin;
    private final VanishManager vanishManager;
    private final Settings settings;
    private ProtocolManager protocolManager;
    private PacketAdapter packetAdapter;
    private final Map<UUID, Set<String>> viewerNametags = new ConcurrentHashMap<>();
    private final Map<UUID, Set<UUID>> viewerHiddenPlayers = new ConcurrentHashMap<>();
    private volatile boolean nametagDisabled;
    private final Set<PacketType> suggestionPacketTypes = ConcurrentHashMap.newKeySet();
    private final Set<PacketType> playerInfoPacketTypes = ConcurrentHashMap.newKeySet();

    public ProtocolLibHook(JavaPlugin plugin, VanishManager vanishManager, Settings settings) {
        this.plugin = plugin;
        this.vanishManager = vanishManager;
        this.settings = settings;
    }

    public void enable() {
        Plugin protocolLib = plugin.getServer().getPluginManager().getPlugin("ProtocolLib");
        if (protocolLib == null || !protocolLib.isEnabled()) {
            return;
        }
        protocolManager = ProtocolLibrary.getProtocolManager();
        if (WrappedTeamParameters.isSupported()) {
            vanishManager.setNametagHandler(this);
        } else {
            plugin.getLogger().warning("ProtocolLib nametag packets are not supported on this server version.");
        }
        vanishManager.setPlayerListHandler(this);
        Set<PacketType> packetTypes = new LinkedHashSet<>();
        packetTypes.add(PacketType.Play.Server.TAB_COMPLETE);
        packetTypes.add(PacketType.Play.Server.CUSTOM_CHAT_COMPLETIONS);
        packetTypes.add(PacketType.Play.Server.PLAYER_INFO);
        packetTypes.add(PacketType.Play.Server.PLAYER_INFO_REMOVE);
        packetTypes.add(PacketType.Play.Server.BLOCK_ACTION);
        packetTypes.add(PacketType.Status.Server.SERVER_INFO);
        addByName(packetTypes, "COMMAND_SUGGESTIONS");
        addByName(packetTypes, "SUGGESTIONS");
        addByName(packetTypes, "COMMAND_COMPLETIONS");
        addByName(packetTypes, "TAB_COMPLETE");
        discoverPacketTypes(packetTypes);

        packetAdapter = new PacketAdapter(
            plugin,
            ListenerPriority.NORMAL,
            packetTypes
        ) {
            @Override
            public void onPacketSending(PacketEvent event) {
                if (event.isCancelled()) {
                    return;
                }
                PacketType type = event.getPacketType();
                if (type == PacketType.Status.Server.SERVER_INFO) {
                    handleServerInfo(event);
                    return;
                }
                if (type == PacketType.Play.Server.BLOCK_ACTION) {
                    handleBlockAction(event);
                    return;
                }
                if (type == PacketType.Play.Server.PLAYER_INFO) {
                    handlePlayerInfo(event);
                    return;
                }
                if (type == PacketType.Play.Server.PLAYER_INFO_REMOVE) {
                    handlePlayerInfoRemove(event);
                    return;
                }
                if (type == PacketType.Play.Server.TAB_COMPLETE) {
                    handleTabComplete(event);
                    return;
                }
                if (type == PacketType.Play.Server.CUSTOM_CHAT_COMPLETIONS) {
                    handleCustomChatCompletions(event);
                    return;
                }
                if (suggestionPacketTypes.contains(type)) {
                    handleSuggestions(event);
                }
                if (playerInfoPacketTypes.contains(type)) {
                    handlePlayerInfoGeneric(event);
                }
            }
        };
        protocolManager.addPacketListener(packetAdapter);
    }

    public void disable() {
        if (protocolManager != null && packetAdapter != null) {
            protocolManager.removePacketListener(packetAdapter);
        }
        vanishManager.setNametagHandler(null);
        vanishManager.setPlayerListHandler(null);
        viewerNametags.clear();
        viewerHiddenPlayers.clear();
    }

    private void handleTabComplete(PacketEvent event) {
        if (!settings.hideFromTabComplete) {
            return;
        }
        Player viewer = event.getPlayer();
        if (viewer != null && vanishManager.canSeeVanishedInTabComplete(viewer.getUniqueId())) {
            return;
        }
        Set<String> vanished = vanishManager.getVanishedTokens();
        try {
            if (event.getPacket().getStringArrays().size() > 0) {
                String[] original = event.getPacket().getStringArrays().read(0);
                if (original == null) {
                    return;
                }
                List<String> filtered = new ArrayList<>();
                for (String entry : original) {
                    if (isVisibleSuggestion(entry, vanished)) {
                        filtered.add(entry);
                    }
                }
                event.getPacket().getStringArrays().write(0, filtered.toArray(new String[0]));
            }
        } catch (Exception ignored) {
            // Unsupported packet format
        }
    }

    private void handleCustomChatCompletions(PacketEvent event) {
        if (!settings.hideFromTabComplete) {
            return;
        }
        Player viewer = event.getPlayer();
        if (viewer != null && vanishManager.canSeeVanishedInTabComplete(viewer.getUniqueId())) {
            return;
        }
        Set<String> vanished = vanishManager.getVanishedTokens();
        if (vanished.isEmpty()) {
            return;
        }
        if (isCustomChatRemove(event.getPacket())) {
            return;
        }
        try {
            if (event.getPacket().getStringArrays().size() > 0) {
                String[] original = event.getPacket().getStringArrays().read(0);
                if (original == null) {
                    return;
                }
                List<String> filtered = new ArrayList<>();
                for (String entry : original) {
                    if (isVisibleSuggestion(entry, vanished)) {
                        filtered.add(entry);
                    }
                }
                event.getPacket().getStringArrays().write(0, filtered.toArray(new String[0]));
            }
        } catch (Exception ignored) {
            // Best-effort only
        }
        scrubStringLists(event, vanished);
    }

    private void handleSuggestions(PacketEvent event) {
        if (!settings.hideFromTabComplete) {
            return;
        }
        Player viewer = event.getPlayer();
        if (viewer != null && vanishManager.canSeeVanishedInTabComplete(viewer.getUniqueId())) {
            return;
        }
        Set<String> vanished = vanishManager.getVanishedTokens();
        try {
            StructureModifier<Suggestions> modifier = event.getPacket().getSpecificModifier(Suggestions.class);
            if (modifier.size() == 0) {
                scrubLists(event, vanished);
                return;
            }
            Suggestions suggestions = modifier.read(0);
            if (suggestions != null && !suggestions.isEmpty()) {
                List<Suggestion> filtered = new ArrayList<>();
                for (Suggestion suggestion : suggestions.getList()) {
                    if (isVisibleSuggestion(suggestion.getText(), vanished)) {
                        filtered.add(suggestion);
                    }
                }
                if (filtered.size() != suggestions.getList().size()) {
                    Suggestions updated = new Suggestions(suggestions.getRange(), filtered);
                    modifier.write(0, updated);
                }
            }
            scrubLists(event, vanished);
        } catch (Exception ignored) {
            // Best-effort only
        }
    }

    @SuppressWarnings("rawtypes")
    private void scrubLists(PacketEvent event, Set<String> vanished) {
        StructureModifier<List> lists = event.getPacket().getSpecificModifier(List.class);
        for (int i = 0; i < lists.size(); i++) {
            List list = lists.read(i);
            if (list == null || list.isEmpty()) {
                continue;
            }
            Object first = list.getFirst();
            if (first instanceof String) {
                List<String> filtered = new ArrayList<>();
                for (Object entry : list) {
                    String value = String.valueOf(entry);
                    if (isVisibleSuggestion(value, vanished)) {
                        filtered.add(value);
                    }
                }
                lists.write(i, filtered);
            } else if (first instanceof Suggestion) {
                List<Suggestion> filtered = new ArrayList<>();
                for (Object entry : list) {
                    Suggestion suggestion = (Suggestion) entry;
                    if (isVisibleSuggestion(suggestion.getText(), vanished)) {
                        filtered.add(suggestion);
                    }
                }
                lists.write(i, filtered);
            }
        }
    }

    @SuppressWarnings("rawtypes")
    private void scrubStringLists(PacketEvent event, Set<String> vanished) {
        StructureModifier<List> lists = event.getPacket().getSpecificModifier(List.class);
        for (int i = 0; i < lists.size(); i++) {
            List list = lists.read(i);
            if (list == null || list.isEmpty()) {
                continue;
            }
            Object first = list.getFirst();
            if (!(first instanceof String)) {
                continue;
            }
            List<String> filtered = new ArrayList<>();
            for (Object entry : list) {
                String value = String.valueOf(entry);
                if (isVisibleSuggestion(value, vanished)) {
                    filtered.add(value);
                }
            }
            lists.write(i, filtered);
        }
    }

    private boolean isCustomChatRemove(PacketContainer packet) {
        try {
            StructureModifier<Object> modifier = packet.getModifier();
            for (int i = 0; i < modifier.size(); i++) {
                Class<?> type = modifier.getField(i).getType();
                if (!type.isEnum()) {
                    continue;
                }
                String name = type.getSimpleName().toUpperCase(Locale.ROOT);
                if (!name.contains("COMPLETION") && !name.contains("CHAT")) {
                    continue;
                }
                Object value = modifier.read(i);
                if (value instanceof Enum<?> action) {
                    return action.name().equalsIgnoreCase("REMOVE");
                }
            }
        } catch (Exception ignored) {
            // Best-effort only
        }
        return false;
    }

    private void handlePlayerInfoGeneric(PacketEvent event) {
        Player viewer = event.getPlayer();
        if (viewer != null && vanishManager.canSee(viewer.getUniqueId())) {
            return;
        }
        String typeName = event.getPacketType().name().toUpperCase(Locale.ROOT);
        if (typeName.contains("REMOVE")) {
            return;
        }
        try {
            List<PlayerInfoData> dataList = event.getPacket().getPlayerInfoDataLists().read(0);
            if (dataList != null && !dataList.isEmpty()) {
                Set<UUID> vanished = vanishManager.getHiddenTabListPlayerIds();
                List<PlayerInfoData> filtered = new ArrayList<>();
                for (PlayerInfoData data : dataList) {
                    if (!vanished.contains(data.getProfileId())) {
                        filtered.add(data);
                    }
                }
                event.getPacket().getPlayerInfoDataLists().write(0, filtered);
                return;
            }
        } catch (Exception ignored) {
            // Best-effort only
        }
        // Some packet variants expose individual game profiles only; no safe bulk filter here.
    }

    private void handlePlayerInfo(PacketEvent event) {
        Player viewer = event.getPlayer();
        if (viewer != null && vanishManager.canSee(viewer.getUniqueId())) {
            return;
        }
        try {
            List<PlayerInfoData> dataList = event.getPacket().getPlayerInfoDataLists().read(0);
            if (dataList == null || dataList.isEmpty()) {
                return;
            }
            Set<UUID> vanished = vanishManager.getHiddenTabListPlayerIds();
            List<PlayerInfoData> filtered = new ArrayList<>();
            for (PlayerInfoData data : dataList) {
                if (!vanished.contains(data.getProfileId())) {
                    filtered.add(data);
                }
            }
            event.getPacket().getPlayerInfoDataLists().write(0, filtered);
        } catch (Exception ignored) {
            // Best-effort only
        }
    }

    private void handlePlayerInfoRemove(PacketEvent event) {
        Player viewer = event.getPlayer();
        if (viewer == null || !vanishManager.canSee(viewer.getUniqueId())) {
            return;
        }
        try {
            List<UUID> ids = event.getPacket().getUUIDLists().read(0);
            if (ids == null || ids.isEmpty()) {
                return;
            }
            Set<UUID> vanished = vanishManager.getVanishedIds();
            List<UUID> filtered = new ArrayList<>();
            for (UUID id : ids) {
                if (!vanished.contains(id)) {
                    filtered.add(id);
                }
            }
            event.getPacket().getUUIDLists().write(0, filtered);
        } catch (Exception ignored) {
            // Best-effort only
        }
    }

    private void handleServerInfo(PacketEvent event) {
        if (!settings.hideFromServerList) {
            return;
        }
        try {
            WrappedServerPing ping = event.getPacket().getServerPings().read(0);
            if (ping == null) {
                return;
            }
            int online = ping.getPlayersOnline();
            int hidden = vanishManager.getHiddenTabListCount();
            ping.setPlayersOnline(Math.max(0, online - hidden));

            List<WrappedGameProfile> sample = ping.getPlayers();
            if (sample != null) {
                List<WrappedGameProfile> filtered = new ArrayList<>(sample);
                Set<String> hiddenNames = vanishManager.getHiddenTabListNames();
                filtered.removeIf(profile -> profile.getName() != null
                    && hiddenNames.contains(profile.getName().toLowerCase(Locale.ROOT)));
                ping.setPlayers(filtered);
            }
            event.getPacket().getServerPings().write(0, ping);
        } catch (Exception ignored) {
            // Best-effort only
        }
    }

    private void handleBlockAction(PacketEvent event) {
        if (!settings.silentChests) {
            return;
        }
        Player viewer = event.getPlayer();
        if (viewer == null) {
            return;
        }
        try {
            BlockPosition position = event.getPacket().getBlockPositionModifier().read(0);
            if (position == null) {
                return;
            }
            UUID worldId = vanishManager.getPlayerWorld(viewer.getUniqueId());
            if (worldId != null && vanishManager.isSilentContainer(worldId, position.getX(), position.getY(), position.getZ(), viewer.getUniqueId())) {
                event.setCancelled(true);
            }
        } catch (Exception ignored) {
            // Best-effort only
        }
    }

    @Override
    public void update(Player viewer, Set<String> vanishedEntries, String prefix) {
        if (protocolManager == null || nametagDisabled) {
            return;
        }
        UUID viewerId = viewer.getUniqueId();
        Set<String> previous = viewerNametags.get(viewerId);
        try {
            if (previous == null) {
                sendCreateTeam(viewer, vanishedEntries, prefix);
                viewerNametags.put(viewerId, new HashSet<>(vanishedEntries));
                return;
            }

            sendUpdateTeam(viewer, prefix);
            Set<String> toAdd = new HashSet<>(vanishedEntries);
            toAdd.removeAll(previous);
            if (!toAdd.isEmpty()) {
                sendPlayers(viewer, 3, toAdd);
            }

            Set<String> toRemove = new HashSet<>(previous);
            toRemove.removeAll(vanishedEntries);
            if (!toRemove.isEmpty()) {
                sendPlayers(viewer, 4, toRemove);
            }

            if (toAdd.isEmpty() && toRemove.isEmpty()) {
                return;
            }
            viewerNametags.put(viewerId, new HashSet<>(vanishedEntries));
        } catch (Exception e) {
            disableNametag(e);
        }
    }

    @Override
    public void clear(Player viewer) {
        if (protocolManager == null || nametagDisabled) {
            return;
        }
        UUID viewerId = viewer.getUniqueId();
        if (viewerNametags.remove(viewerId) == null) {
            return;
        }
        try {
            PacketContainer packet = createTeamPacket(1, null, null);
            protocolManager.sendServerPacket(viewer, packet);
        } catch (Exception e) {
            disableNametag(e);
        }
    }

    @Override
    public void forget(UUID viewerId) {
        viewerNametags.remove(viewerId);
        viewerHiddenPlayers.remove(viewerId);
    }

    @Override
    public void hide(Player viewer, Player target) {
        if (protocolManager == null) {
            return;
        }
        UUID viewerId = viewer.getUniqueId();
        UUID targetId = target.getUniqueId();
        Set<UUID> hidden = viewerHiddenPlayers.computeIfAbsent(viewerId, ignored -> ConcurrentHashMap.newKeySet());
        if (!hidden.add(targetId)) {
            return;
        }
        try {
            PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.PLAYER_INFO_REMOVE);
            packet.getUUIDLists().write(0, List.of(targetId));
            protocolManager.sendServerPacket(viewer, packet);
        } catch (Exception ignored) {
            // Best-effort only
        }
        sendCustomChatCompletionUpdate(viewer, target.getName(), false);
    }

    @Override
    public void show(Player viewer, Player target) {
        if (protocolManager == null) {
            return;
        }
        UUID viewerId = viewer.getUniqueId();
        UUID targetId = target.getUniqueId();
        Set<UUID> hidden = viewerHiddenPlayers.get(viewerId);
        if (hidden == null || !hidden.remove(targetId)) {
            return;
        }
        try {
            PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.PLAYER_INFO);
            setAddPlayerInfoAction(packet);
            WrappedGameProfile profile = WrappedGameProfile.fromPlayer(target);
            EnumWrappers.NativeGameMode gameMode = EnumWrappers.NativeGameMode.fromBukkit(target.getGameMode());
            int ping = target.getPing();
            WrappedChatComponent displayName;
            try {
                String json = net.kyori.adventure.text.serializer.gson.GsonComponentSerializer.gson().serialize(target.playerListName());
                displayName = WrappedChatComponent.fromJson(json);
            } catch (Exception ignored) {
                displayName = WrappedChatComponent.fromLegacyText(target.getName());
            }
            PlayerInfoData data = new PlayerInfoData(profile, ping, gameMode, displayName);
            packet.getPlayerInfoDataLists().write(0, List.of(data));
            protocolManager.sendServerPacket(viewer, packet);
        } catch (Exception ignored) {
            // Best-effort only
        }
        sendCustomChatCompletionUpdate(viewer, target.getName(), true);
    }

    private void sendCustomChatCompletionUpdate(Player viewer, String name, boolean add) {
        if (protocolManager == null || name == null || name.isBlank()) {
            return;
        }
        try {
            PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.CUSTOM_CHAT_COMPLETIONS);
            if (!setCustomChatAction(packet, add ? "ADD" : "REMOVE")) {
                return;
            }
            if (!writeCustomChatEntries(packet, name)) {
                return;
            }
            protocolManager.sendServerPacket(viewer, packet);
        } catch (Exception ignored) {
            // Best-effort only
        }
    }

    private boolean setCustomChatAction(PacketContainer packet, String actionName) {
        try {
            StructureModifier<Object> modifier = packet.getModifier();
            for (int i = 0; i < modifier.size(); i++) {
                Class<?> type = modifier.getField(i).getType();
                if (!type.isEnum()) {
                    continue;
                }
                String name = type.getSimpleName().toUpperCase(Locale.ROOT);
                if (!name.contains("COMPLETION") && !name.contains("CHAT")) {
                    continue;
                }
                modifier.write(i, enumValue(type, actionName));
                return true;
            }
        } catch (Exception ignored) {
            // Best-effort only
        }
        return false;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private Enum<?> enumValue(Class<?> type, String actionName) {
        return Enum.valueOf(type.asSubclass(Enum.class), actionName);
    }

    @SuppressWarnings("rawtypes")
    private boolean writeCustomChatEntries(PacketContainer packet, String name) {
        try {
            StructureModifier<String[]> arrays = packet.getStringArrays();
            if (arrays.size() > 0) {
                arrays.write(0, new String[]{name});
                return true;
            }
        } catch (Exception ignored) {
            // Continue to list-based writers
        }
        try {
            StructureModifier<List> lists = packet.getSpecificModifier(List.class);
            if (lists.size() > 0) {
                lists.write(0, List.of(name));
                return true;
            }
        } catch (Exception ignored) {
            // Best-effort only
        }
        return false;
    }

    private void setAddPlayerInfoAction(PacketContainer packet) {
        try {
            if (packet.getPlayerInfoAction().size() > 0) {
                packet.getPlayerInfoAction().write(0, EnumWrappers.PlayerInfoAction.ADD_PLAYER);
                return;
            }
        } catch (Exception ignored) {
            // Continue to set as enum set
        }
        try {
            if (packet.getPlayerInfoActions().size() > 0) {
                packet.getPlayerInfoActions().write(0, java.util.EnumSet.of(EnumWrappers.PlayerInfoAction.ADD_PLAYER));
            }
        } catch (Exception ignored) {
            // Best-effort only
        }
    }

    private void sendCreateTeam(Player viewer, Set<String> entries, String prefix) {
        PacketContainer packet = createTeamPacket(0, buildParameters(prefix), entries);
        protocolManager.sendServerPacket(viewer, packet);
    }

    private void sendUpdateTeam(Player viewer, String prefix) {
        PacketContainer packet = createTeamPacket(2, buildParameters(prefix), null);
        protocolManager.sendServerPacket(viewer, packet);
    }

    private void sendPlayers(Player viewer, int mode, Collection<String> entries) {
        PacketContainer packet = createTeamPacket(mode, null, entries);
        protocolManager.sendServerPacket(viewer, packet);
    }

    private PacketContainer createTeamPacket(int mode, WrappedTeamParameters parameters, Collection<String> entries) {
        PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.SCOREBOARD_TEAM);
        packet.getStrings().write(0, NAMETAG_TEAM);
        writeMode(packet, mode);
        if (parameters != null) {
            Optional<WrappedTeamParameters> optional = Optional.of(parameters);
            packet.getOptionalTeamParameters().write(0, optional);
        }
        if (entries != null) {
            writeEntries(packet, entries);
        }
        return packet;
    }

    private WrappedTeamParameters buildParameters(String prefix) {
        String safePrefix = prefix == null ? "" : prefix;
        EnumWrappers.TeamVisibility visibility = safePrefix.isEmpty()
            ? EnumWrappers.TeamVisibility.NEVER
            : EnumWrappers.TeamVisibility.ALWAYS;
        return WrappedTeamParameters.newBuilder()
            .displayName(WrappedChatComponent.fromText("Vanish"))
            .prefix(WrappedChatComponent.fromLegacyText(safePrefix))
            .suffix(WrappedChatComponent.fromText(""))
            .nametagVisibility(visibility)
            .collisionRule(EnumWrappers.TeamCollisionRule.NEVER)
            .color(EnumWrappers.ChatFormatting.GRAY)
            .options(0)
            .build();
    }

    private void writeMode(PacketContainer packet, int mode) {
        StructureModifier<Integer> ints = packet.getIntegers();
        if (ints.size() > 0) {
            ints.write(0, mode);
            return;
        }
        StructureModifier<Object> modifier = packet.getModifier();
        for (int i = 0; i < modifier.size(); i++) {
            Class<?> type = modifier.getField(i).getType();
            if (type == int.class || type == Integer.class) {
                modifier.write(i, mode);
                return;
            }
            if (type.isEnum()) {
                Object[] constants = type.getEnumConstants();
                if (constants != null && mode >= 0 && mode < constants.length) {
                    modifier.write(i, constants[mode]);
                    return;
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void writeEntries(PacketContainer packet, Collection<String> entries) {
        StructureModifier<Object> modifier = packet.getModifier();
        for (int i = 0; i < modifier.size(); i++) {
            Class<?> type = modifier.getField(i).getType();
            if (Collection.class.isAssignableFrom(type)) {
                modifier.write(i, new ArrayList<>(entries));
                return;
            }
        }
    }

    private void disableNametag(Exception e) {
        nametagDisabled = true;
        viewerNametags.clear();
        plugin.getLogger().warning("Disabled nametag packet handling: " + e.getMessage());
    }

    private void addByName(Set<PacketType> packetTypes, String name) {
        for (PacketType type : PacketType.fromName(name)) {
            if (type.isServer()) {
                packetTypes.add(type);
            }
        }
    }

    private void discoverPacketTypes(Set<PacketType> packetTypes) {
        for (PacketType type : PacketType.values()) {
            if (!type.isServer()) {
                continue;
            }
            String name = type.name().toUpperCase(Locale.ROOT);
            String classNames = String.join(" ", type.getClassNames()).toUpperCase(Locale.ROOT);
            boolean isSuggestion = name.contains("SUGGEST")
                || name.contains("TAB_COMPLETE")
                || name.contains("COMMAND_COMPLET")
                || classNames.contains("SUGGEST")
                || classNames.contains("TABCOMPLETE")
                || classNames.contains("COMMANDSUGGEST");
            boolean isPlayerInfo = name.contains("PLAYER_INFO")
                || classNames.contains("PLAYERINFO");
            if (isSuggestion) {
                suggestionPacketTypes.add(type);
                packetTypes.add(type);
            }
            if (isPlayerInfo) {
                playerInfoPacketTypes.add(type);
                packetTypes.add(type);
            }
        }
        suggestionPacketTypes.add(PacketType.Play.Server.TAB_COMPLETE);
        suggestionPacketTypes.add(PacketType.Play.Server.CUSTOM_CHAT_COMPLETIONS);
        playerInfoPacketTypes.add(PacketType.Play.Server.PLAYER_INFO);
        playerInfoPacketTypes.add(PacketType.Play.Server.PLAYER_INFO_REMOVE);
    }

    private boolean isVisibleSuggestion(String suggestion, Set<String> vanished) {
        if (suggestion == null) {
            return true;
        }
        String normalized = PlainTextComponentSerializer.plainText().serialize(
            LegacyComponentSerializer.legacySection().deserialize(suggestion)
        );
        normalized = normalized.toLowerCase(Locale.ROOT);
        return !matchesVanished(normalized, vanished);
    }

    private boolean matchesVanished(String normalized, Set<String> vanished) {
        if (normalized.isEmpty()) {
            return false;
        }
        if (vanished.contains(normalized)) {
            return true;
        }
        for (String name : vanished) {
            if (!name.isEmpty() && normalized.startsWith(name)) {
                return true;
            }
        }
        if (normalized.charAt(0) == '@' && normalized.length() > 2) {
            String trimmed = normalized.substring(1);
            if (vanished.contains(trimmed)) {
                return true;
            }
            for (String name : vanished) {
                if (!name.isEmpty() && trimmed.startsWith(name)) {
                    return true;
                }
            }
        }
        int colon = normalized.indexOf(':');
        if (colon >= 0 && colon + 1 < normalized.length()) {
            String trimmed = normalized.substring(colon + 1);
            if (vanished.contains(trimmed)) {
                return true;
            }
            for (String name : vanished) {
                if (!name.isEmpty() && trimmed.startsWith(name)) {
                    return true;
                }
            }
        }
        return false;
    }
}
