package org.bacon.vanishPlusPlusPlus.storage;

import org.bacon.vanishPlusPlusPlus.model.PlayerData;
import org.bacon.vanishPlusPlusPlus.model.VanishRules;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class DataStore {
    private final JavaPlugin plugin;
    private final File file;

    public DataStore(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "data.yml");
    }

    public Map<UUID, PlayerData> load(VanishRules defaultRules) {
        if (!file.exists()) {
            return new HashMap<>();
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection players = config.getConfigurationSection("players");
        Map<UUID, PlayerData> data = new HashMap<>();
        if (players == null) {
            return data;
        }
        for (String key : players.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                ConfigurationSection section = players.getConfigurationSection(key);
                if (section == null) {
                    continue;
                }
                VanishRules rules = defaultRules.copy();
                ConfigurationSection rulesSection = section.getConfigurationSection("rules");
                if (rulesSection != null) {
                    applyRules(rules, rulesSection);
                }
                PlayerData playerData = new PlayerData(rules);
                playerData.setVanished(section.getBoolean("vanished", false));
                data.put(uuid, playerData);
            } catch (IllegalArgumentException ignored) {
                // Skip invalid UUIDs
            }
        }
        return data;
    }

    public void saveSync(Map<UUID, PlayerData> data) {
        YamlConfiguration config = new YamlConfiguration();
        ConfigurationSection players = config.createSection("players");
        for (Map.Entry<UUID, PlayerData> entry : data.entrySet()) {
            ConfigurationSection section = players.createSection(entry.getKey().toString());
            PlayerData playerData = entry.getValue();
            section.set("vanished", playerData.isVanished());
            ConfigurationSection rulesSection = section.createSection("rules");
            playerData.getRules().writeTo(rulesSection);
        }
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save vanish data: " + e.getMessage());
        }
    }

    private void applyRules(VanishRules rules, ConfigurationSection section) {
        if (section.contains(VanishRules.KEY_CAN_BREAK_BLOCKS)) {
            rules.updateRule(VanishRules.KEY_CAN_BREAK_BLOCKS, section.getBoolean(VanishRules.KEY_CAN_BREAK_BLOCKS));
        }
        if (section.contains(VanishRules.KEY_CAN_PLACE_BLOCKS)) {
            rules.updateRule(VanishRules.KEY_CAN_PLACE_BLOCKS, section.getBoolean(VanishRules.KEY_CAN_PLACE_BLOCKS));
        }
        if (section.contains(VanishRules.KEY_CAN_INTERACT)) {
            rules.updateRule(VanishRules.KEY_CAN_INTERACT, section.getBoolean(VanishRules.KEY_CAN_INTERACT));
        }
        if (section.contains(VanishRules.KEY_CAN_HIT_ENTITIES)) {
            rules.updateRule(VanishRules.KEY_CAN_HIT_ENTITIES, section.getBoolean(VanishRules.KEY_CAN_HIT_ENTITIES));
        }
        if (section.contains(VanishRules.KEY_CAN_PICKUP_ITEMS)) {
            rules.updateRule(VanishRules.KEY_CAN_PICKUP_ITEMS, section.getBoolean(VanishRules.KEY_CAN_PICKUP_ITEMS));
        }
        if (section.contains(VanishRules.KEY_CAN_DROP_ITEMS)) {
            rules.updateRule(VanishRules.KEY_CAN_DROP_ITEMS, section.getBoolean(VanishRules.KEY_CAN_DROP_ITEMS));
        }
        if (section.contains(VanishRules.KEY_CAN_CHAT)) {
            rules.updateRule(VanishRules.KEY_CAN_CHAT, section.getBoolean(VanishRules.KEY_CAN_CHAT));
        }
        boolean hasWorldEventsRule = section.contains(VanishRules.KEY_CAN_TRIGGER_WORLD_EVENTS);
        if (hasWorldEventsRule) {
            rules.updateRule(VanishRules.KEY_CAN_TRIGGER_WORLD_EVENTS, section.getBoolean(VanishRules.KEY_CAN_TRIGGER_WORLD_EVENTS));
        }
        if (!hasWorldEventsRule && section.contains(VanishRules.KEY_CAN_TRIGGER_PHYSICAL)) {
            rules.updateRule(VanishRules.KEY_CAN_TRIGGER_PHYSICAL, section.getBoolean(VanishRules.KEY_CAN_TRIGGER_PHYSICAL));
        }
        boolean hasMobTargetRule = section.contains(VanishRules.KEY_CAN_BE_TARGETED_BY_MOBS);
        if (hasMobTargetRule) {
            rules.updateRule(VanishRules.KEY_CAN_BE_TARGETED_BY_MOBS, section.getBoolean(VanishRules.KEY_CAN_BE_TARGETED_BY_MOBS));
        }
        if (!hasMobTargetRule && section.contains(VanishRules.KEY_MOB_TARGETING)) {
            rules.updateRule(VanishRules.KEY_MOB_TARGETING, section.getBoolean(VanishRules.KEY_MOB_TARGETING));
        }
        if (section.contains(VanishRules.KEY_CAN_OPEN_CONTAINERS_SILENTLY)) {
            rules.updateRule(VanishRules.KEY_CAN_OPEN_CONTAINERS_SILENTLY, section.getBoolean(VanishRules.KEY_CAN_OPEN_CONTAINERS_SILENTLY));
        }
        if (section.contains(VanishRules.KEY_CAN_USE_VISIBLE_ITEMS)) {
            rules.updateRule(VanishRules.KEY_CAN_USE_VISIBLE_ITEMS, section.getBoolean(VanishRules.KEY_CAN_USE_VISIBLE_ITEMS));
        }
        if (section.contains(VanishRules.KEY_CAN_RECEIVE_POTION_EFFECTS)) {
            rules.updateRule(VanishRules.KEY_CAN_RECEIVE_POTION_EFFECTS, section.getBoolean(VanishRules.KEY_CAN_RECEIVE_POTION_EFFECTS));
        }
        if (section.contains(VanishRules.KEY_SHOW_IN_TABLIST_AND_PLAYERCOUNT)) {
            rules.updateRule(VanishRules.KEY_SHOW_IN_TABLIST_AND_PLAYERCOUNT, section.getBoolean(VanishRules.KEY_SHOW_IN_TABLIST_AND_PLAYERCOUNT));
        }
        if (section.contains(VanishRules.KEY_SHOW_IN_TAB_COMPLETE)) {
            rules.updateRule(VanishRules.KEY_SHOW_IN_TAB_COMPLETE, section.getBoolean(VanishRules.KEY_SHOW_IN_TAB_COMPLETE));
        }
        if (section.contains(VanishRules.KEY_DOUBLE_SNEAK_SPECTATOR)) {
            rules.updateRule(VanishRules.KEY_DOUBLE_SNEAK_SPECTATOR, section.getBoolean(VanishRules.KEY_DOUBLE_SNEAK_SPECTATOR));
        }
        if (section.contains(VanishRules.KEY_FORCE_SURVIVAL_ON_UNVANISH)) {
            rules.updateRule(VanishRules.KEY_FORCE_SURVIVAL_ON_UNVANISH, section.getBoolean(VanishRules.KEY_FORCE_SURVIVAL_ON_UNVANISH));
        }
        if (section.contains(VanishRules.KEY_BLOCK_ADVANCEMENTS)) {
            rules.updateRule(VanishRules.KEY_BLOCK_ADVANCEMENTS, section.getBoolean(VanishRules.KEY_BLOCK_ADVANCEMENTS));
        }
        if (section.contains(VanishRules.KEY_BROADCAST_FAKE_MESSAGES)) {
            rules.updateRule(VanishRules.KEY_BROADCAST_FAKE_MESSAGES, section.getBoolean(VanishRules.KEY_BROADCAST_FAKE_MESSAGES));
        } else {
            boolean legacyQuit = section.getBoolean("broadcast_fake_quit", true);
            boolean legacyJoin = section.getBoolean("broadcast_fake_join", true);
            rules.updateRule(VanishRules.KEY_BROADCAST_FAKE_MESSAGES, legacyQuit || legacyJoin);
        }
    }
}
