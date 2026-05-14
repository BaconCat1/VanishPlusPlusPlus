package org.bacon.vanishPlusPlusPlus.hook;

import org.bacon.vanishPlusPlusPlus.Settings;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;
import java.util.UUID;

public final class TabHook {
    private final JavaPlugin plugin;
    private final Settings settings;
    private Object tabAPI;
    private Object formatManager;
    private Method getPlayerMethod;
    private Method setPrefixMethod;
    private Method setNameMethod;
    private Method setSuffixMethod;

    public TabHook(JavaPlugin plugin, Settings settings) {
        this.plugin = plugin;
        this.settings = settings;
    }

    public void enable() {
        Plugin tabPlugin = plugin.getServer().getPluginManager().getPlugin("TAB");
        if (tabPlugin == null || !tabPlugin.isEnabled()) {
            return;
        }
        try {
            Class<?> tabApiClass = Class.forName("me.neznamy.tab.api.TabAPI");
            Method getInstance = tabApiClass.getMethod("getInstance");
            tabAPI = getInstance.invoke(null);
            getPlayerMethod = tabApiClass.getMethod("getPlayer", UUID.class);

            Method getManager = findMethod(tabApiClass, "getTabListFormatManager");
            if (getManager == null) {
                getManager = findMethod(tabApiClass, "getTablistFormatManager");
            }
            if (getManager == null) {
                return;
            }
            formatManager = getManager.invoke(tabAPI);
            Class<?> managerClass = formatManager.getClass();
            setPrefixMethod = findTwoArgumentMethod(managerClass, "setPrefix");
            setNameMethod = findTwoArgumentMethod(managerClass, "setName");
            setSuffixMethod = findTwoArgumentMethod(managerClass, "setSuffix");
        } catch (Throwable ignored) {
            tabAPI = null;
            formatManager = null;
        }
    }

    public boolean isActive() {
        return tabAPI != null && formatManager != null && getPlayerMethod != null;
    }

    public void apply(Player player, boolean vanished) {
        if (!isActive()) {
            return;
        }
        UUID playerId = player.getUniqueId();
        String playerName = player.getName();
        plugin.getServer().getGlobalRegionScheduler().run(plugin, task -> {
            try {
                Object tabPlayer = getPlayerMethod.invoke(tabAPI, playerId);
                if (tabPlayer == null) {
                    return;
                }
                if (vanished) {
                    invoke(setPrefixMethod, formatManager, tabPlayer, settings.tabPrefix);
                    invoke(setNameMethod, formatManager, tabPlayer, "§7§o" + playerName);
                } else {
                    invoke(setPrefixMethod, formatManager, tabPlayer, null);
                    invoke(setNameMethod, formatManager, tabPlayer, null);
                    invoke(setSuffixMethod, formatManager, tabPlayer, null);
                }
            } catch (Throwable ignored) {
                // Best-effort only
            }
        });
    }

    private static Method findMethod(Class<?> type, String name) {
        try {
            return type.getMethod(name);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    private static Method findTwoArgumentMethod(Class<?> type, String name) {
        for (Method method : type.getMethods()) {
            if (method.getName().equals(name) && method.getParameterCount() == 2) {
                return method;
            }
        }
        return null;
    }

    private static void invoke(Method method, Object target, Object arg1, Object arg2) throws Exception {
        if (method != null) {
            method.invoke(target, arg1, arg2);
        }
    }
}
