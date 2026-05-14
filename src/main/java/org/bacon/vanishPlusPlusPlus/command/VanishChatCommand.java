package org.bacon.vanishPlusPlusPlus.command;

import org.bacon.vanishPlusPlusPlus.Permissions;
import org.bacon.vanishPlusPlusPlus.Settings;
import org.bacon.vanishPlusPlusPlus.VanishManager;
import org.bacon.vanishPlusPlusPlus.model.PlayerData;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class VanishChatCommand implements CommandExecutor, TabCompleter {
    private final VanishManager vanishManager;
    private final Settings settings;

    public VanishChatCommand(VanishManager vanishManager, Settings settings) {
        this.vanishManager = vanishManager;
        this.settings = settings;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }
        if (!player.hasPermission(Permissions.CHAT)) {
            player.sendMessage(settings.msgNoPermission);
            return true;
        }
        if (args.length == 0 || !args[0].equalsIgnoreCase("confirm")) {
            player.sendMessage("Usage: /vchat confirm");
            return true;
        }

        PlayerData data = vanishManager.getData(player);
        String pending = data.getPendingChatMessage();
        if (pending == null) {
            player.sendMessage(settings.msgNoChatPending);
            return true;
        }

        data.setAllowNextChat(true);
        player.chat(pending);
        player.sendMessage(settings.msgChatSent);
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (!sender.hasPermission(Permissions.CHAT)) {
            return List.of();
        }
        if (args.length == 1) {
            return TabCompleteUtil.filterByPrefix(List.of("confirm"), args[0]);
        }
        return List.of();
    }
}
