package org.bacon.vanishPlusPlusPlus.command;

import org.bacon.vanishPlusPlusPlus.Permissions;
import org.bacon.vanishPlusPlusPlus.Settings;
import org.bacon.vanishPlusPlusPlus.VanishManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class VanishCommand implements CommandExecutor, TabCompleter {
    private final VanishManager vanishManager;
    private final Settings settings;

    public VanishCommand(VanishManager vanishManager, Settings settings) {
        this.vanishManager = vanishManager;
        this.settings = settings;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length > 0) {
            if (!sender.hasPermission(Permissions.VANISH_OTHERS)) {
                sender.sendMessage(settings.msgNoPermission);
                return true;
            }
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage(settings.msgPlayerNotFound);
                return true;
            }
            vanishManager.toggleVanish(target);
            if (vanishManager.isVanished(target)) {
                sender.sendMessage(settings.msgVanishedOther.replace("%player%", target.getName()));
                target.sendMessage(settings.msgVanish);
            } else {
                sender.sendMessage(settings.msgUnvanishedOther.replace("%player%", target.getName()));
                target.sendMessage(settings.msgUnvanish);
            }
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }
        if (!player.hasPermission(Permissions.VANISH)) {
            player.sendMessage(settings.msgNoPermission);
            return true;
        }

        vanishManager.toggleVanish(player);
        if (vanishManager.isVanished(player)) {
            player.sendMessage(settings.msgVanish);
        } else {
            player.sendMessage(settings.msgUnvanish);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (!sender.hasPermission(Permissions.VANISH) && !sender.hasPermission(Permissions.VANISH_OTHERS)) {
            return List.of();
        }
        if (args.length == 1 && sender.hasPermission(Permissions.VANISH_OTHERS)) {
            return TabCompleteUtil.matchPlayers(args[0]);
        }
        return List.of();
    }
}
