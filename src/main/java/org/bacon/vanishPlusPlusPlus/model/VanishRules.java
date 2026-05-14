package org.bacon.vanishPlusPlusPlus.model;

import org.bukkit.configuration.ConfigurationSection;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public final class VanishRules {
    public static final String KEY_CAN_BREAK_BLOCKS = "can_break_blocks";
    public static final String KEY_CAN_PLACE_BLOCKS = "can_place_blocks";
    public static final String KEY_CAN_INTERACT = "can_interact";
    public static final String KEY_CAN_HIT_ENTITIES = "can_hit_entities";
    public static final String KEY_CAN_PICKUP_ITEMS = "can_pickup_items";
    public static final String KEY_CAN_DROP_ITEMS = "can_drop_items";
    public static final String KEY_CAN_CHAT = "can_chat";
    public static final String KEY_CAN_TRIGGER_WORLD_EVENTS = "can_trigger_world_events";
    public static final String KEY_CAN_BE_TARGETED_BY_MOBS = "can_be_targeted_by_mobs";
    public static final String KEY_CAN_OPEN_CONTAINERS_SILENTLY = "can_open_containers_silently";
    public static final String KEY_CAN_USE_VISIBLE_ITEMS = "can_use_visible_items";
    public static final String KEY_CAN_RECEIVE_POTION_EFFECTS = "can_receive_potion_effects";
    public static final String KEY_BROADCAST_FAKE_MESSAGES = "broadcast_fake_messages";
    public static final String KEY_SHOW_IN_TABLIST_AND_PLAYERCOUNT = "show_in_tablist_and_playercount";
    public static final String KEY_SHOW_IN_TAB_COMPLETE = "show_in_tab_complete";
    public static final String KEY_DOUBLE_SNEAK_SPECTATOR = "double_sneak_spectator";
    public static final String KEY_FORCE_SURVIVAL_ON_UNVANISH = "force_survival_on_unvanish";
    public static final String KEY_BLOCK_ADVANCEMENTS = "block_advancements";
    public static final String KEY_CAN_TRIGGER_PHYSICAL = "can_trigger_physical";
    public static final String KEY_MOB_TARGETING = "mob_targeting";
    private static final String KEY_BROADCAST_FAKE_QUIT_LEGACY = "broadcast_fake_quit";
    private static final String KEY_BROADCAST_FAKE_JOIN_LEGACY = "broadcast_fake_join";

    private volatile boolean canBreakBlocks;
    private volatile boolean canPlaceBlocks;
    private volatile boolean canInteract;
    private volatile boolean canHitEntities;
    private volatile boolean canPickupItems;
    private volatile boolean canDropItems;
    private volatile boolean canChat;
    private volatile boolean canTriggerWorldEvents;
    private volatile boolean canBeTargetedByMobs;
    private volatile boolean canOpenContainersSilently;
    private volatile boolean canUseVisibleItems;
    private volatile boolean canReceivePotionEffects;
    private volatile boolean broadcastFakeMessages;
    private volatile boolean showInTabListAndPlayerCount;
    private volatile boolean showInTabComplete;
    private volatile boolean doubleSneakSpectator;
    private volatile boolean forceSurvivalOnUnvanish;
    private volatile boolean blockAdvancements;

    public VanishRules() {
    }

    public VanishRules copy() {
        VanishRules copy = new VanishRules();
        copy.canBreakBlocks = canBreakBlocks;
        copy.canPlaceBlocks = canPlaceBlocks;
        copy.canInteract = canInteract;
        copy.canHitEntities = canHitEntities;
        copy.canPickupItems = canPickupItems;
        copy.canDropItems = canDropItems;
        copy.canChat = canChat;
        copy.canTriggerWorldEvents = canTriggerWorldEvents;
        copy.canBeTargetedByMobs = canBeTargetedByMobs;
        copy.canOpenContainersSilently = canOpenContainersSilently;
        copy.canUseVisibleItems = canUseVisibleItems;
        copy.canReceivePotionEffects = canReceivePotionEffects;
        copy.broadcastFakeMessages = broadcastFakeMessages;
        copy.showInTabListAndPlayerCount = showInTabListAndPlayerCount;
        copy.showInTabComplete = showInTabComplete;
        copy.doubleSneakSpectator = doubleSneakSpectator;
        copy.forceSurvivalOnUnvanish = forceSurvivalOnUnvanish;
        copy.blockAdvancements = blockAdvancements;
        return copy;
    }

    public static VanishRules fromConfig(ConfigurationSection section) {
        VanishRules rules = new VanishRules();
        if (section == null) {
            return rules;
        }
        rules.canBreakBlocks = section.getBoolean(KEY_CAN_BREAK_BLOCKS, false);
        rules.canPlaceBlocks = section.getBoolean(KEY_CAN_PLACE_BLOCKS, false);
        rules.canInteract = section.getBoolean(KEY_CAN_INTERACT, true);
        rules.canHitEntities = section.getBoolean(KEY_CAN_HIT_ENTITIES, false);
        rules.canPickupItems = section.getBoolean(KEY_CAN_PICKUP_ITEMS, false);
        rules.canDropItems = section.getBoolean(KEY_CAN_DROP_ITEMS, false);
        rules.canChat = section.getBoolean(KEY_CAN_CHAT, false);
        rules.canTriggerWorldEvents = section.getBoolean(KEY_CAN_TRIGGER_WORLD_EVENTS,
            section.getBoolean(KEY_CAN_TRIGGER_PHYSICAL, false));
        rules.canBeTargetedByMobs = section.getBoolean(KEY_CAN_BE_TARGETED_BY_MOBS,
            section.getBoolean(KEY_MOB_TARGETING, false));
        rules.canOpenContainersSilently = section.getBoolean(KEY_CAN_OPEN_CONTAINERS_SILENTLY, true);
        rules.canUseVisibleItems = section.getBoolean(KEY_CAN_USE_VISIBLE_ITEMS, false);
        rules.canReceivePotionEffects = section.getBoolean(KEY_CAN_RECEIVE_POTION_EFFECTS, false);
        rules.showInTabListAndPlayerCount = section.getBoolean(KEY_SHOW_IN_TABLIST_AND_PLAYERCOUNT, false);
        rules.showInTabComplete = section.getBoolean(KEY_SHOW_IN_TAB_COMPLETE, false);
        rules.doubleSneakSpectator = section.getBoolean(KEY_DOUBLE_SNEAK_SPECTATOR, true);
        rules.forceSurvivalOnUnvanish = section.getBoolean(KEY_FORCE_SURVIVAL_ON_UNVANISH, true);
        rules.blockAdvancements = section.getBoolean(KEY_BLOCK_ADVANCEMENTS, true);
        if (section.contains(KEY_BROADCAST_FAKE_MESSAGES)) {
            rules.broadcastFakeMessages = section.getBoolean(KEY_BROADCAST_FAKE_MESSAGES, true);
        } else {
            boolean legacyQuit = section.getBoolean(KEY_BROADCAST_FAKE_QUIT_LEGACY, true);
            boolean legacyJoin = section.getBoolean(KEY_BROADCAST_FAKE_JOIN_LEGACY, true);
            rules.broadcastFakeMessages = legacyQuit || legacyJoin;
        }
        return rules;
    }

    public void writeTo(ConfigurationSection section) {
        section.set(KEY_CAN_BREAK_BLOCKS, canBreakBlocks);
        section.set(KEY_CAN_PLACE_BLOCKS, canPlaceBlocks);
        section.set(KEY_CAN_INTERACT, canInteract);
        section.set(KEY_CAN_HIT_ENTITIES, canHitEntities);
        section.set(KEY_CAN_PICKUP_ITEMS, canPickupItems);
        section.set(KEY_CAN_DROP_ITEMS, canDropItems);
        section.set(KEY_CAN_CHAT, canChat);
        section.set(KEY_CAN_TRIGGER_WORLD_EVENTS, canTriggerWorldEvents);
        section.set(KEY_CAN_BE_TARGETED_BY_MOBS, canBeTargetedByMobs);
        section.set(KEY_CAN_OPEN_CONTAINERS_SILENTLY, canOpenContainersSilently);
        section.set(KEY_CAN_USE_VISIBLE_ITEMS, canUseVisibleItems);
        section.set(KEY_CAN_RECEIVE_POTION_EFFECTS, canReceivePotionEffects);
        section.set(KEY_BROADCAST_FAKE_MESSAGES, broadcastFakeMessages);
        section.set(KEY_SHOW_IN_TABLIST_AND_PLAYERCOUNT, showInTabListAndPlayerCount);
        section.set(KEY_SHOW_IN_TAB_COMPLETE, showInTabComplete);
        section.set(KEY_DOUBLE_SNEAK_SPECTATOR, doubleSneakSpectator);
        section.set(KEY_FORCE_SURVIVAL_ON_UNVANISH, forceSurvivalOnUnvanish);
        section.set(KEY_BLOCK_ADVANCEMENTS, blockAdvancements);
    }

    public boolean updateRule(String key, boolean value) {
        String normalized = Objects.requireNonNull(key, "key").toLowerCase(Locale.ROOT);
        switch (normalized) {
            case KEY_CAN_BREAK_BLOCKS -> canBreakBlocks = value;
            case KEY_CAN_PLACE_BLOCKS -> canPlaceBlocks = value;
            case KEY_CAN_INTERACT -> canInteract = value;
            case KEY_CAN_HIT_ENTITIES -> canHitEntities = value;
            case KEY_CAN_PICKUP_ITEMS -> canPickupItems = value;
            case KEY_CAN_DROP_ITEMS -> canDropItems = value;
            case KEY_CAN_CHAT -> canChat = value;
            case KEY_CAN_TRIGGER_WORLD_EVENTS, KEY_CAN_TRIGGER_PHYSICAL -> canTriggerWorldEvents = value;
            case KEY_CAN_BE_TARGETED_BY_MOBS, KEY_MOB_TARGETING -> canBeTargetedByMobs = value;
            case KEY_CAN_OPEN_CONTAINERS_SILENTLY -> canOpenContainersSilently = value;
            case KEY_CAN_USE_VISIBLE_ITEMS -> canUseVisibleItems = value;
            case KEY_CAN_RECEIVE_POTION_EFFECTS -> canReceivePotionEffects = value;
            case KEY_BROADCAST_FAKE_MESSAGES, KEY_BROADCAST_FAKE_QUIT_LEGACY, KEY_BROADCAST_FAKE_JOIN_LEGACY -> broadcastFakeMessages = value;
            case KEY_SHOW_IN_TABLIST_AND_PLAYERCOUNT -> showInTabListAndPlayerCount = value;
            case KEY_SHOW_IN_TAB_COMPLETE -> showInTabComplete = value;
            case KEY_DOUBLE_SNEAK_SPECTATOR -> doubleSneakSpectator = value;
            case KEY_FORCE_SURVIVAL_ON_UNVANISH -> forceSurvivalOnUnvanish = value;
            case KEY_BLOCK_ADVANCEMENTS -> blockAdvancements = value;
            default -> {
                return false;
            }
        }
        return true;
    }

    public Map<String, Boolean> asMap() {
        Map<String, Boolean> map = new java.util.LinkedHashMap<>();
        map.put(KEY_CAN_BREAK_BLOCKS, canBreakBlocks);
        map.put(KEY_CAN_PLACE_BLOCKS, canPlaceBlocks);
        map.put(KEY_CAN_INTERACT, canInteract);
        map.put(KEY_CAN_HIT_ENTITIES, canHitEntities);
        map.put(KEY_CAN_PICKUP_ITEMS, canPickupItems);
        map.put(KEY_CAN_DROP_ITEMS, canDropItems);
        map.put(KEY_CAN_CHAT, canChat);
        map.put(KEY_CAN_TRIGGER_WORLD_EVENTS, canTriggerWorldEvents);
        map.put(KEY_CAN_BE_TARGETED_BY_MOBS, canBeTargetedByMobs);
        map.put(KEY_CAN_OPEN_CONTAINERS_SILENTLY, canOpenContainersSilently);
        map.put(KEY_CAN_USE_VISIBLE_ITEMS, canUseVisibleItems);
        map.put(KEY_CAN_RECEIVE_POTION_EFFECTS, canReceivePotionEffects);
        map.put(KEY_BROADCAST_FAKE_MESSAGES, broadcastFakeMessages);
        map.put(KEY_SHOW_IN_TABLIST_AND_PLAYERCOUNT, showInTabListAndPlayerCount);
        map.put(KEY_SHOW_IN_TAB_COMPLETE, showInTabComplete);
        map.put(KEY_DOUBLE_SNEAK_SPECTATOR, doubleSneakSpectator);
        map.put(KEY_FORCE_SURVIVAL_ON_UNVANISH, forceSurvivalOnUnvanish);
        map.put(KEY_BLOCK_ADVANCEMENTS, blockAdvancements);
        return map;
    }

    public boolean canBreakBlocks() {
        return canBreakBlocks;
    }

    public boolean canPlaceBlocks() {
        return canPlaceBlocks;
    }

    public boolean canInteract() {
        return canInteract;
    }

    public boolean canHitEntities() {
        return canHitEntities;
    }

    public boolean canPickupItems() {
        return canPickupItems;
    }

    public boolean canDropItems() {
        return canDropItems;
    }

    public boolean canChat() {
        return canChat;
    }

    public boolean canTriggerWorldEvents() {
        return canTriggerWorldEvents;
    }

    public boolean canBeTargetedByMobs() {
        return canBeTargetedByMobs;
    }

    public boolean canOpenContainersSilently() {
        return canOpenContainersSilently;
    }

    public boolean canUseVisibleItems() {
        return canUseVisibleItems;
    }

    public boolean canReceivePotionEffects() {
        return canReceivePotionEffects;
    }

    public boolean broadcastFakeMessages() {
        return broadcastFakeMessages;
    }

    public boolean showInTabListAndPlayerCount() {
        return showInTabListAndPlayerCount;
    }

    public boolean showInTabComplete() {
        return showInTabComplete;
    }

    public boolean doubleSneakSpectator() {
        return doubleSneakSpectator;
    }

    public boolean forceSurvivalOnUnvanish() {
        return forceSurvivalOnUnvanish;
    }

    public boolean blockAdvancements() {
        return blockAdvancements;
    }

    public void setCanPickupItems(boolean canPickupItems) {
        this.canPickupItems = canPickupItems;
    }

}
