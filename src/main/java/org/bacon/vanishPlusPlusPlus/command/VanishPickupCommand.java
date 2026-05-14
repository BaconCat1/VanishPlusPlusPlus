package org.bacon.vanishPlusPlusPlus.command;

import org.bacon.vanishPlusPlusPlus.Permissions;
import org.bacon.vanishPlusPlusPlus.Settings;
import org.bacon.vanishPlusPlusPlus.VanishManager;
import org.bacon.vanishPlusPlusPlus.model.VanishRules;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class VanishPickupCommand implements CommandExecutor, TabCompleter {
    private final VanishManager vanishManager;
    private final Settings settings;

    public VanishPickupCommand(VanishManager vanishManager, Settings settings) {
        this.vanishManager = vanishManager;
        this.settings = settings;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        Player target;
        if (args.length > 0) {
            if (!sender.hasPermission(Permissions.PICKUP_OTHERS)) {
                sender.sendMessage(settings.msgNoPermission);
                return true;
            }
            target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage(settings.msgPlayerNotFound);
                return true;
            }
        } else {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("This command can only be used by players.");
                return true;
            }
            if (!player.hasPermission(Permissions.PICKUP)) {
                player.sendMessage(settings.msgNoPermission);
                return true;
            }
            target = player;
        }

        VanishRules rules = vanishManager.getRules(target);
        rules.setCanPickupItems(!rules.canPickupItems());
        vanishManager.applyPickupSetting(target);
        vanishManager.queueSave();

        String message = rules.canPickupItems() ? settings.msgPickupEnabled : settings.msgPickupDisabled;
        sender.sendMessage(message);
        if (!sender.equals(target)) {
            target.sendMessage(message);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (!sender.hasPermission(Permissions.PICKUP) && !sender.hasPermission(Permissions.PICKUP_OTHERS)) {
            return List.of();
        }
        if (args.length == 1 && sender.hasPermission(Permissions.PICKUP_OTHERS)) {
            return TabCompleteUtil.matchPlayers(args[0]);
        }
        return List.of();
    }
}
