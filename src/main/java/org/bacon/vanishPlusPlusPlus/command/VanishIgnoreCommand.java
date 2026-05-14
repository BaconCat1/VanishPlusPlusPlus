package org.bacon.vanishPlusPlusPlus.command;

import org.bacon.vanishPlusPlusPlus.Permissions;
import org.bacon.vanishPlusPlusPlus.Settings;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class VanishIgnoreCommand implements CommandExecutor, TabCompleter {
    private final JavaPlugin plugin;
    private final Settings settings;

    public VanishIgnoreCommand(JavaPlugin plugin, Settings settings) {
        this.plugin = plugin;
        this.settings = settings;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission(Permissions.IGNORE_WARNING)) {
            sender.sendMessage(settings.msgNoPermission);
            return true;
        }
        boolean current = plugin.getConfig().getBoolean("warnings.protocollib-missing", true);
        boolean next = !current;
        plugin.getConfig().set("warnings.protocollib-missing", next);
        plugin.saveConfig();
        sender.sendMessage(next ? settings.msgIgnoreWarningDisabled : settings.msgIgnoreWarningEnabled);
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (!sender.hasPermission(Permissions.IGNORE_WARNING)) {
            return List.of();
        }
        return List.of();
    }
}
