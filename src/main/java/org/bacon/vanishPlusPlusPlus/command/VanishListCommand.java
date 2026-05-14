package org.bacon.vanishPlusPlusPlus.command;

import org.bacon.vanishPlusPlusPlus.Permissions;
import org.bacon.vanishPlusPlusPlus.Settings;
import org.bacon.vanishPlusPlusPlus.VanishManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class VanishListCommand implements CommandExecutor, TabCompleter {
    private final VanishManager vanishManager;
    private final Settings settings;

    public VanishListCommand(VanishManager vanishManager, Settings settings) {
        this.vanishManager = vanishManager;
        this.settings = settings;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission(Permissions.LIST)) {
            sender.sendMessage(settings.msgNoPermission);
            return true;
        }

        List<String> vanished = Bukkit.getOnlinePlayers().stream()
            .filter(vanishManager::isVanished)
            .map(Player::getName)
            .sorted(String.CASE_INSENSITIVE_ORDER)
            .toList();

        if (vanished.isEmpty()) {
            sender.sendMessage(Component.text("No players are currently vanished.", NamedTextColor.AQUA));
            return true;
        }

        sender.sendMessage(Component.text("Vanished players (" + vanished.size() + "): ", NamedTextColor.AQUA)
            .append(Component.text(String.join(", ", vanished), NamedTextColor.GRAY)));
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        return List.of();
    }
}
