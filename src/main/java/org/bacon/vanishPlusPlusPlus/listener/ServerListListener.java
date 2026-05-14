package org.bacon.vanishPlusPlusPlus.listener;

import com.destroystokyo.paper.event.server.PaperServerListPingEvent;
import org.bacon.vanishPlusPlusPlus.Settings;
import org.bacon.vanishPlusPlusPlus.VanishManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public final class ServerListListener implements Listener {
    private final VanishManager vanishManager;
    private final Settings settings;

    public ServerListListener(VanishManager vanishManager, Settings settings) {
        this.vanishManager = vanishManager;
        this.settings = settings;
    }

    @EventHandler
    public void onPing(PaperServerListPingEvent event) {
        if (!settings.hideFromServerList) {
            return;
        }
        int hidden = vanishManager.getHiddenTabListCount();
        int online = event.getNumPlayers();
        int visible = Math.max(0, online - hidden);
        event.setNumPlayers(visible);
    }
}
