package org.bacon.vanishPlusPlusPlus.command;

import org.bacon.vanishPlusPlusPlus.model.VanishRules;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

final class TabCompleteUtil {
    private static final List<String> RULE_KEYS = List.of(
        VanishRules.KEY_CAN_BREAK_BLOCKS,
        VanishRules.KEY_CAN_PLACE_BLOCKS,
        VanishRules.KEY_CAN_INTERACT,
        VanishRules.KEY_CAN_HIT_ENTITIES,
        VanishRules.KEY_CAN_PICKUP_ITEMS,
        VanishRules.KEY_CAN_DROP_ITEMS,
        VanishRules.KEY_CAN_CHAT,
        VanishRules.KEY_CAN_TRIGGER_WORLD_EVENTS,
        VanishRules.KEY_CAN_BE_TARGETED_BY_MOBS,
        VanishRules.KEY_CAN_OPEN_CONTAINERS_SILENTLY,
        VanishRules.KEY_CAN_USE_VISIBLE_ITEMS,
        VanishRules.KEY_CAN_RECEIVE_POTION_EFFECTS,
        VanishRules.KEY_BROADCAST_FAKE_MESSAGES,
        VanishRules.KEY_SHOW_IN_TABLIST_AND_PLAYERCOUNT,
        VanishRules.KEY_SHOW_IN_TAB_COMPLETE,
        VanishRules.KEY_DOUBLE_SNEAK_SPECTATOR,
        VanishRules.KEY_FORCE_SURVIVAL_ON_UNVANISH,
        VanishRules.KEY_BLOCK_ADVANCEMENTS
    );

    private TabCompleteUtil() {
    }

    static List<String> matchRuleKeys(String token) {
        return filterByPrefix(RULE_KEYS, token);
    }

    static List<String> matchBooleans(String token) {
        return filterByPrefix(List.of("true", "false"), token);
    }

    static List<String> matchPlayers(String token) {
        List<String> names = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            names.add(player.getName());
        }
        return filterByPrefix(names, token);
    }

    static List<String> filterByPrefix(Collection<String> options, String token) {
        String needle = token == null ? "" : token.toLowerCase(Locale.ROOT);
        List<String> result = new ArrayList<>();
        for (String option : options) {
            if (option.toLowerCase(Locale.ROOT).startsWith(needle)) {
                result.add(option);
            }
        }
        return result;
    }
}
