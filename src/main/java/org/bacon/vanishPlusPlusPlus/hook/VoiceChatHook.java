package org.bacon.vanishPlusPlusPlus.hook;

import de.maxhenkel.voicechat.api.BukkitVoicechatService;
import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import de.maxhenkel.voicechat.api.events.SoundPacketEvent;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.packets.Packet;
import org.bacon.vanishPlusPlusPlus.Settings;
import org.bacon.vanishPlusPlusPlus.VanishManager;

import java.util.UUID;

@SuppressWarnings("SpellCheckingInspection")
public final class VoiceChatHook implements VoicechatPlugin {
    private final VanishManager vanishManager;
    private final Settings settings;

    private VoiceChatHook(VanishManager vanishManager, Settings settings) {
        this.vanishManager = vanishManager;
        this.settings = settings;
    }

    public static void register(org.bukkit.plugin.java.JavaPlugin plugin, VanishManager vanishManager, Settings settings) {
        if (!settings.voiceChatEnabled) {
            return;
        }
        BukkitVoicechatService service = plugin.getServer().getServicesManager().load(BukkitVoicechatService.class);
        if (service != null) {
            service.registerPlugin(new VoiceChatHook(vanishManager, settings));
        }
    }

    @Override
    public String getPluginId() {
        return "vanishplusplusplus";
    }

    @Override
    public void initialize(VoicechatApi ignored) {
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(MicrophonePacketEvent.class, this::onMicrophonePacket);
        registration.registerEvent(SoundPacketEvent.class, this::onSoundPacket);
    }

    private void onMicrophonePacket(MicrophonePacketEvent event) {
        if (!settings.voiceChatIsolate) {
            return;
        }
        VoicechatConnection connection = event.getSenderConnection();
        if (connection == null || connection.getPlayer() == null) {
            return;
        }
        UUID senderId = connection.getPlayer().getUuid();
        if (vanishManager.isVanished(senderId)) {
            event.cancel();
        }
    }

    private void onSoundPacket(SoundPacketEvent<? extends Packet> event) {
        if (!settings.voiceChatIsolate) {
            return;
        }
        VoicechatConnection receiver = event.getReceiverConnection();
        if (receiver == null || receiver.getPlayer() == null) {
            return;
        }
        UUID receiverId = receiver.getPlayer().getUuid();
        if (vanishManager.isVanished(receiverId)) {
            event.cancel();
            return;
        }
        VoicechatConnection sender = event.getSenderConnection();
        if (sender != null && sender.getPlayer() != null) {
            UUID senderId = sender.getPlayer().getUuid();
            if (vanishManager.isVanished(senderId)) {
                event.cancel();
            }
        }
    }
}
