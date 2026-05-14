package org.bacon.vanishPlusPlusPlus.command;

import org.bacon.vanishPlusPlusPlus.Permissions;
import org.bacon.vanishPlusPlusPlus.Settings;
import org.bacon.vanishPlusPlusPlus.VanishManager;
import org.bacon.vanishPlusPlusPlus.model.VanishRules;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.List;
import java.util.Map;

public final class VanishRulesCommand implements CommandExecutor, TabCompleter {
    private static final String RESET = "reset";
    private final VanishManager vanishManager;
    private final Settings settings;

    public VanishRulesCommand(VanishManager vanishManager, Settings settings) {
        this.vanishManager = vanishManager;
        this.settings = settings;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        boolean canRules = sender.hasPermission(Permissions.RULES);
        boolean canRulesOthers = sender.hasPermission(Permissions.RULES_OTHERS);
        if (!canRules && !canRulesOthers) {
            sender.sendMessage(settings.msgNoPermission);
            return true;
        }

        Player target = null;
        int ruleIndex = 0;
        if (args.length >= 1) {
            Player maybeTarget = Bukkit.getPlayer(args[0]);
            if (maybeTarget != null && canRulesOthers) {
                target = maybeTarget;
                ruleIndex = 1;
            }
        }

        if (target == null) {
            if (!canRules) {
                sender.sendMessage(settings.msgNoPermission);
                return true;
            }
            if (!(sender instanceof Player player)) {
                sender.sendMessage("Usage: /vrules <player> <rule|reset> [true|false]");
                return true;
            }
            target = player;
        }

        VanishRules rules = vanishManager.getRules(target);
        if (args.length <= ruleIndex) {
            sendRules(sender, target, rules);
            return true;
        }

        String ruleKey = args[ruleIndex].toLowerCase(Locale.ROOT);
        if (ruleKey.equals(RESET)) {
            vanishManager.getData(target).setRules(settings.defaultRules.copy());
            vanishManager.applyPickupSetting(target);
            if (vanishManager.isVanished(target)) {
                vanishManager.refreshListName(target);
                vanishManager.updateVisibilityForAll();
            }
            vanishManager.queueSave();
            sender.sendMessage(Component.text("Rules reset to defaults for " + target.getName() + ".", NamedTextColor.YELLOW));
            if (!sender.equals(target)) {
                target.sendMessage(Component.text("Your vanish rules were reset to defaults.", NamedTextColor.YELLOW));
            }
            return true;
        }

        boolean value;
        if (args.length > ruleIndex + 1) {
            value = Boolean.parseBoolean(args[ruleIndex + 1]);
        } else {
            Map<String, Boolean> current = rules.asMap();
            Boolean existing = current.get(ruleKey);
            if (existing == null) {
                sender.sendMessage(Component.text("Unknown rule: " + ruleKey, NamedTextColor.RED));
                sendRules(sender, target, rules);
                return true;
            }
            value = !existing;
        }

        if (!rules.updateRule(ruleKey, value)) {
            sender.sendMessage(Component.text("Unknown rule: " + ruleKey, NamedTextColor.RED));
            sendRules(sender, target, rules);
            return true;
        }

        if (ruleKey.equals(VanishRules.KEY_CAN_PICKUP_ITEMS)) {
            vanishManager.applyPickupSetting(target);
        }
        if (ruleKey.equals(VanishRules.KEY_SHOW_IN_TABLIST_AND_PLAYERCOUNT)
            || ruleKey.equals(VanishRules.KEY_SHOW_IN_TAB_COMPLETE)) {
            if (vanishManager.isVanished(target)) {
                vanishManager.refreshListName(target);
                vanishManager.updateVisibilityForAll();
            }
        }
        if (ruleKey.equals(VanishRules.KEY_DOUBLE_SNEAK_SPECTATOR)
            || ruleKey.equals(VanishRules.KEY_FORCE_SURVIVAL_ON_UNVANISH)) {
            vanishManager.enforceVanishState(target);
        }
        vanishManager.queueSave();
        sender.sendMessage(Component.text("Rule '" + ruleKey + "' set to " + value + " for " + target.getName() + ".", NamedTextColor.YELLOW));
        if (!sender.equals(target)) {
            target.sendMessage(Component.text("Rule '" + ruleKey + "' set to " + value + ".", NamedTextColor.YELLOW));
        }
        return true;
    }

    private void sendRules(CommandSender sender, Player target, VanishRules rules) {
        sender.sendMessage(Component.text("Rules for " + target.getName() + ":", NamedTextColor.AQUA));
        for (Map.Entry<String, Boolean> entry : rules.asMap().entrySet()) {
            sender.sendMessage(Component.text("- " + entry.getKey() + ": " + entry.getValue(), NamedTextColor.GRAY));
        }
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        boolean canRules = sender.hasPermission(Permissions.RULES);
        boolean canRulesOthers = sender.hasPermission(Permissions.RULES_OTHERS);
        if (!canRules && !canRulesOthers) {
            return List.of();
        }

        if (args.length == 1) {
            if (canRulesOthers) {
                List<String> combined = new java.util.ArrayList<>();
                combined.addAll(TabCompleteUtil.matchPlayers(args[0]));
                combined.addAll(TabCompleteUtil.matchRuleKeys(args[0]));
                combined.addAll(TabCompleteUtil.filterByPrefix(List.of(RESET), args[0]));
                return combined;
            }
            List<String> combined = new java.util.ArrayList<>(TabCompleteUtil.matchRuleKeys(args[0]));
            combined.addAll(TabCompleteUtil.filterByPrefix(List.of(RESET), args[0]));
            return combined;
        }

        if (args.length == 2) {
            if (canRulesOthers && Bukkit.getPlayerExact(args[0]) != null) {
                List<String> combined = new java.util.ArrayList<>(TabCompleteUtil.matchRuleKeys(args[1]));
                combined.addAll(TabCompleteUtil.filterByPrefix(List.of(RESET), args[1]));
                return combined;
            }
            if (RESET.equalsIgnoreCase(args[0])) {
                return List.of();
            }
            return TabCompleteUtil.matchBooleans(args[1]);
        }

        if (args.length == 3) {
            if (canRulesOthers && Bukkit.getPlayerExact(args[0]) != null && RESET.equalsIgnoreCase(args[1])) {
                return List.of();
            }
            return TabCompleteUtil.matchBooleans(args[2]);
        }

        return List.of();
    }
}
