package org.bacon.vanishPlusPlusPlus.listener;

import com.destroystokyo.paper.event.brigadier.AsyncPlayerSendSuggestionsEvent;
import com.destroystokyo.paper.event.server.AsyncTabCompleteEvent;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import org.bacon.vanishPlusPlusPlus.Settings;
import org.bacon.vanishPlusPlusPlus.VanishManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChatTabCompleteEvent;
import org.bukkit.event.server.TabCompleteEvent;

import java.util.Locale;
import java.util.Set;

public final class TabCompleteListener implements Listener {
    private final VanishManager vanishManager;
    private final Settings settings;

    public TabCompleteListener(VanishManager vanishManager, Settings settings) {
        this.vanishManager = vanishManager;
        this.settings = settings;
    }

    @EventHandler(ignoreCancelled = true)
    public void onTabComplete(AsyncTabCompleteEvent event) {
        if (event.isCommand() && event.getSender() instanceof org.bukkit.entity.Player player) {
            String buffer = event.getBuffer();
            if (isCommandRootBuffer(buffer)) {
                filterCommandSuggestions(event, player.getUniqueId());
            }
        }
        if (!settings.hideFromTabComplete) {
            return;
        }
        if (event.getSender() instanceof org.bukkit.entity.Player player) {
            if (vanishManager.canSeeVanishedInTabComplete(player.getUniqueId())) {
                return;
            }
        }
        Set<String> vanished = vanishManager.getVanishedTokens();
        if (!event.getCompletions().isEmpty()) {
            java.util.List<String> filtered = new java.util.ArrayList<>();
            for (String entry : event.getCompletions()) {
                if (!isVanishedSuggestion(entry, vanished)) {
                    filtered.add(entry);
                }
            }
            event.setCompletions(filtered);
        }
        if (event.completions() != null && !event.completions().isEmpty()) {
            java.util.List<AsyncTabCompleteEvent.Completion> filtered = new java.util.ArrayList<>();
            for (AsyncTabCompleteEvent.Completion completion : event.completions()) {
                if (!isVanishedSuggestion(completion.suggestion(), vanished)) {
                    filtered.add(completion);
                }
            }
            event.completions(filtered);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onSendSuggestions(AsyncPlayerSendSuggestionsEvent event) {
        Suggestions suggestions = event.getSuggestions();
        if (suggestions == null || suggestions.isEmpty()) {
            return;
        }
        java.util.UUID playerId = event.getPlayer().getUniqueId();
        boolean filterCommands = event.getBuffer() != null
            && event.getBuffer().startsWith("/")
            && isCommandRootBuffer(event.getBuffer());
        boolean filterVanished = settings.hideFromTabComplete && !vanishManager.canSeeVanishedInTabComplete(playerId);
        if (!filterCommands && !filterVanished) {
            return;
        }

        Set<String> vanished = filterVanished ? vanishManager.getVanishedTokens() : Set.of();
        if (!filterCommands && vanished.isEmpty()) {
            return;
        }

        java.util.List<Suggestion> filtered = new java.util.ArrayList<>();
        for (Suggestion suggestion : suggestions.getList()) {
            String text = suggestion.getText();
            if (filterCommands && shouldHideCommandSuggestion(text, playerId)) {
                continue;
            }
            if (filterVanished && isVanishedSuggestion(text, vanished)) {
                continue;
            }
            filtered.add(suggestion);
        }
        if (filtered.size() != suggestions.getList().size()) {
            event.setSuggestions(new Suggestions(suggestions.getRange(), filtered));
        }
    }

    @EventHandler(ignoreCancelled = true)
    @SuppressWarnings("deprecation")
    public void onLegacyChatTabComplete(PlayerChatTabCompleteEvent event) {
        if (!settings.hideFromTabComplete) {
            return;
        }
        if (vanishManager.canSeeVanishedInTabComplete(event.getPlayer().getUniqueId())) {
            return;
        }
        Set<String> vanished = vanishManager.getVanishedTokens();
        event.getTabCompletions().removeIf(entry -> isVanishedSuggestion(entry, vanished));
    }

    @EventHandler(ignoreCancelled = true)
    public void onLegacyTabComplete(TabCompleteEvent event) {
        if (!(event.getSender() instanceof org.bukkit.entity.Player player)) {
            return;
        }
        if (isCommandRootBuffer(event.getBuffer())) {
            event.getCompletions().removeIf(entry -> shouldHideCommandSuggestion(entry, player.getUniqueId()));
        }
        if (!settings.hideFromTabComplete) {
            return;
        }
        if (vanishManager.canSeeVanishedInTabComplete(player.getUniqueId())) {
            return;
        }
        Set<String> vanished = vanishManager.getVanishedTokens();
        event.getCompletions().removeIf(entry -> isVanishedSuggestion(entry, vanished));
    }

    private void filterCommandSuggestions(AsyncTabCompleteEvent event, java.util.UUID playerId) {
        event.getCompletions().removeIf(entry -> shouldHideCommandSuggestion(entry, playerId));
        if (event.completions() != null) {
            event.completions().removeIf(completion -> shouldHideCommandSuggestion(completion.suggestion(), playerId));
        }
    }

    private boolean shouldHideCommandSuggestion(String suggestion, java.util.UUID playerId) {
        String name = normalizeCommand(suggestion);
        return switch (name) {
            case "vanish", "v" -> !vanishManager.canUseVanish(playerId);
            case "vanishrules", "vrules", "vsettings" -> !vanishManager.canUseRules(playerId);
            case "vanishchat", "vchat" -> !vanishManager.canChat(playerId);
            case "vanishpickup", "vpickup" -> !vanishManager.canUsePickup(playerId);
            case "vanishignore", "vignore" -> !vanishManager.canIgnoreWarning(playerId);
            case "vlist", "vanishlist" -> !vanishManager.canUseList(playerId);
            default -> false;
        };
    }

    private boolean isCommandRootBuffer(String buffer) {
        if (buffer == null) {
            return false;
        }
        String trimmed = buffer.startsWith("/") ? buffer.substring(1) : buffer;
        return !trimmed.contains(" ");
    }

    private String normalizeCommand(String raw) {
        String name = raw == null ? "" : raw.trim();
        name = name.startsWith("/") ? name.substring(1) : name;
        int colon = name.indexOf(':');
        if (colon >= 0 && colon + 1 < name.length()) {
            name = name.substring(colon + 1);
        }
        int space = name.indexOf(' ');
        if (space >= 0) {
            name = name.substring(0, space);
        }
        return name.toLowerCase(Locale.ROOT);
    }

    private boolean isVanishedSuggestion(String suggestion, Set<String> vanished) {
        if (suggestion == null) {
            return false;
        }
        String normalized = stripSectionColors(suggestion);
        normalized = normalized.toLowerCase(Locale.ROOT);
        return matchesVanished(normalized, vanished);
    }

    private String stripSectionColors(String input) {
        return input.replaceAll("(?i)\u00A7[0-9A-FK-ORX]", "");
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
