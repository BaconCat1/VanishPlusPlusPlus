package org.bacon.vanishPlusPlusPlus;

import org.bacon.vanishPlusPlusPlus.model.VanishRules;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class Settings {
    public final String msgVanish;
    public final String msgUnvanish;
    public final String msgNoPermission;
    public final String msgPlayerNotFound;
    public final String msgVanishedOther;
    public final String msgUnvanishedOther;
    public final String msgChatLocked;
    public final String msgChatSent;
    public final String msgNoChatPending;
    public final String msgPickupEnabled;
    public final String msgPickupDisabled;
    public final String msgIgnoreWarningEnabled;
    public final String msgIgnoreWarningDisabled;

    public final String tabPrefix;
    public final String nametagPrefix;
    public final boolean actionBarEnabled;
    public final String actionBarText;

    public final boolean hideRealQuitMessages;
    public final boolean hideRealJoinMessages;
    public final boolean broadcastFakeMessages;
    public final String fakeQuitMessage;
    public final String fakeJoinMessage;

    public final boolean silentChests;
    public final boolean ignoreProjectiles;
    public final boolean preventPotionEffects;
    public final boolean preventRaidTrigger;
    public final boolean preventSculkSensors;
    public final boolean hideFromTabComplete;
    public final boolean preventAccidentalChat;
    public final boolean hideFromServerList;
    public final boolean disableBlockTriggering;
    public final boolean disableMobTargeting;
    public final boolean zeroCollision;

    public final boolean voiceChatEnabled;
    public final boolean voiceChatIsolate;

    public final int heartbeatSeconds;
    public final boolean warnProtocolLibMissing;
    public final Set<String> messageCommands;

    public final VanishRules defaultRules;

    private Settings(FileConfiguration config) {
        ConfigurationSection messages = config.getConfigurationSection("messages");
        msgVanish = color(messages, "vanish", "&6You are now vanished.");
        msgUnvanish = color(messages, "unvanish", "&6You are no longer vanished.");
        msgNoPermission = color(messages, "no-permission", "&cYou do not have permission to use this command.");
        msgPlayerNotFound = color(messages, "player-not-found", "&cPlayer not found.");
        msgVanishedOther = color(messages, "vanished-other", "&6You have vanished %player%.");
        msgUnvanishedOther = color(messages, "unvanished-other", "&6You have unvanished %player%.");
        msgChatLocked = color(messages, "chat-locked", "&cYou are vanished! &7Type &b/vchat confirm &7to send this message.");
        msgChatSent = color(messages, "chat-sent", "&aMessage sent.");
        msgNoChatPending = color(messages, "no-chat-pending", "&cYou have no pending chat messages to confirm.");
        msgPickupEnabled = color(messages, "pickup-enabled", "&eItem pickup is now &aENABLED&e.");
        msgPickupDisabled = color(messages, "pickup-disabled", "&eItem pickup is now &cDISABLED&e.");
        msgIgnoreWarningEnabled = color(messages, "ignore-warning-enabled", "&aProtocolLib warnings disabled.");
        msgIgnoreWarningDisabled = color(messages, "ignore-warning-disabled", "&eProtocolLib warnings enabled.");

        ConfigurationSection appearance = config.getConfigurationSection("vanish-appearance");
        tabPrefix = color(appearance, "tab-prefix", "&7&o[VANISHED] ");
        nametagPrefix = color(appearance, "nametag-prefix", "");
        ConfigurationSection actionBar = appearance != null ? appearance.getConfigurationSection("action-bar") : null;
        actionBarEnabled = actionBar != null && actionBar.getBoolean("enabled", true);
        actionBarText = color(actionBar, "text", "&bYou are currently VANISHED");

        ConfigurationSection effects = config.getConfigurationSection("vanish-effects");
        hideRealQuitMessages = effects == null || effects.getBoolean("hide-real-quit-messages", true);
        hideRealJoinMessages = effects == null || effects.getBoolean("hide-real-join-messages", true);
        boolean defaultBroadcast = true;
        if (effects != null && !effects.contains("broadcast-fake-messages")) {
            boolean legacyQuit = effects.getBoolean("broadcast-fake-quit", true);
            boolean legacyJoin = effects.getBoolean("broadcast-fake-join", true);
            defaultBroadcast = legacyQuit || legacyJoin;
        }
        broadcastFakeMessages = effects == null ? true : effects.getBoolean("broadcast-fake-messages", defaultBroadcast);
        fakeQuitMessage = color(effects, "fake-quit-message", "&e%player% left the game");
        fakeJoinMessage = color(effects, "fake-join-message", "&e%player% joined the game");

        ConfigurationSection features = config.getConfigurationSection("invisibility-features");
        silentChests = features == null || features.getBoolean("silent-chests", true);
        ignoreProjectiles = features == null || features.getBoolean("ignore-projectiles", true);
        preventPotionEffects = features == null || features.getBoolean("prevent-potion-effects", true);
        preventRaidTrigger = features == null || features.getBoolean("prevent-raid-trigger", true);
        preventSculkSensors = features == null || features.getBoolean("prevent-sculk-sensors", true);
        hideFromTabComplete = features == null || features.getBoolean("hide-from-tab-complete", true);
        preventAccidentalChat = features == null || features.getBoolean("prevent-accidental-chat", true);
        hideFromServerList = features == null || features.getBoolean("hide-from-server-list", true);
        disableBlockTriggering = features == null || features.getBoolean("disable-block-triggering", true);
        disableMobTargeting = features == null || features.getBoolean("disable-mob-targeting", true);
        zeroCollision = features == null || features.getBoolean("zero-collision", true);

        ConfigurationSection hooks = config.getConfigurationSection("hooks");
        ConfigurationSection voiceChat = hooks != null ? hooks.getConfigurationSection("simple-voice-chat") : null;
        voiceChatEnabled = voiceChat == null || voiceChat.getBoolean("enabled", true);
        voiceChatIsolate = voiceChat == null || voiceChat.getBoolean("isolate-vanished-players", true);

        ConfigurationSection heartbeat = config.getConfigurationSection("heartbeat");
        heartbeatSeconds = heartbeat != null ? Math.max(1, heartbeat.getInt("interval-seconds", 2)) : 2;

        ConfigurationSection warnings = config.getConfigurationSection("warnings");
        warnProtocolLibMissing = warnings == null || warnings.getBoolean("protocollib-missing", true);

        List<String> rawCommands = config.getStringList("message-commands");
        if (rawCommands.isEmpty()) {
            rawCommands = List.of("msg", "w", "pm", "message", "tell");
        }
        messageCommands = new HashSet<>();
        for (String command : rawCommands) {
            if (command != null && !command.isBlank()) {
                messageCommands.add(command.toLowerCase(Locale.ROOT));
            }
        }

        ConfigurationSection defaults = config.getConfigurationSection("default-rules");
        defaultRules = VanishRules.fromConfig(defaults);
    }

    public static Settings from(FileConfiguration config) {
        return new Settings(config);
    }

    private static String color(ConfigurationSection section, String key, String fallback) {
        String raw = fallback;
        if (section != null) {
            raw = section.getString(key, fallback);
        }
        return translateAlternateColorCodes(raw);
    }

    private static String translateAlternateColorCodes(String raw) {
        char[] chars = raw.toCharArray();
        for (int i = 0; i < chars.length - 1; i++) {
            if (chars[i] == '&' && isColorCode(chars[i + 1])) {
                chars[i] = '\u00A7';
                chars[i + 1] = Character.toLowerCase(chars[i + 1]);
            }
        }
        return new String(chars);
    }

    private static boolean isColorCode(char value) {
        return (value >= '0' && value <= '9')
            || (value >= 'a' && value <= 'f')
            || (value >= 'A' && value <= 'F')
            || value == 'k' || value == 'K'
            || value == 'l' || value == 'L'
            || value == 'm' || value == 'M'
            || value == 'n' || value == 'N'
            || value == 'o' || value == 'O'
            || value == 'r' || value == 'R';
    }
}
