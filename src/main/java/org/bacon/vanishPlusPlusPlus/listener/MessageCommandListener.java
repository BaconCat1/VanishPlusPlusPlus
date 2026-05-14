package org.bacon.vanishPlusPlusPlus.listener;

import org.bacon.vanishPlusPlusPlus.Permissions;
import org.bacon.vanishPlusPlusPlus.Settings;
import org.bacon.vanishPlusPlusPlus.VanishManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;

import java.util.Locale;

public final class MessageCommandListener implements Listener {
    private final VanishManager vanishManager;
    private final Settings settings;

    public MessageCommandListener(VanishManager vanishManager, Settings settings) {
        this.vanishManager = vanishManager;
        this.settings = settings;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player sender = event.getPlayer();
        if (sender.hasPermission(Permissions.SEE) || sender.hasPermission(Permissions.TAB_COMPLETE_MESSAGE_BYPASS)) {
            return;
        }
        String message = event.getMessage();
        if (message.isEmpty() || message.charAt(0) != '/') {
            return;
        }
        String[] parts = message.substring(1).split("\\s+");
        if (parts.length < 2) {
            return;
        }
        String command = normalize(parts[0]);
        if (!settings.messageCommands.contains(command)) {
            return;
        }
        String targetToken = normalizeTarget(parts[1]);
        if (targetToken.isEmpty()) {
            return;
        }
        if (isVanishedTarget(targetToken)) {
            event.setCancelled(true);
        }
    }

    private String normalize(String command) {
        String name = command.toLowerCase(Locale.ROOT);
        int colon = name.indexOf(':');
        if (colon >= 0 && colon + 1 < name.length()) {
            name = name.substring(colon + 1);
        }
        return name;
    }

    private String normalizeTarget(String raw) {
        String name = raw.trim();
        if (name.startsWith("@")) {
            name = name.substring(1);
        }
        int colon = name.indexOf(':');
        if (colon >= 0 && colon + 1 < name.length()) {
            name = name.substring(colon + 1);
        }
        return name.toLowerCase(Locale.ROOT);
    }

    private boolean isVanishedTarget(String targetToken) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!vanishManager.isVanished(player)) {
                continue;
            }
            String name = player.getName().toLowerCase(Locale.ROOT);
            if (name.equals(targetToken) || name.startsWith(targetToken)) {
                return true;
            }
            String display = PlainTextComponentSerializer.plainText().serialize(player.displayName());
            if (!display.isEmpty()) {
                display = stripSectionColors(display).toLowerCase(Locale.ROOT);
                if (display.equals(targetToken) || display.startsWith(targetToken)) {
                    return true;
                }
            }
            String listName = PlainTextComponentSerializer.plainText().serialize(player.playerListName());
            if (!listName.isEmpty()) {
                listName = stripSectionColors(listName).toLowerCase(Locale.ROOT);
                if (listName.equals(targetToken) || listName.startsWith(targetToken)) {
                    return true;
                }
            }
        }
        return vanishManager.getVanishedNames().contains(targetToken);
    }

    private String stripSectionColors(String input) {
        return input.replaceAll("(?i)\u00A7[0-9A-FK-ORX]", "");
    }
}
