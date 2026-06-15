package me.sekc.packman;

import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerOpenWindow;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetTitleText;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class PacketEventsListener implements PacketListener {
	Packman plugin;
	PacketEventsListener(Packman plugin) {
		super();
		this.plugin = plugin;
	}

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacketType() == PacketType.Play.Server.OPEN_WINDOW) {
			WrapperPlayServerOpenWindow packet = new WrapperPlayServerOpenWindow(event);

			// Replace glyphs in the title of container GUIs
			String title = MiniMessage.miniMessage().serialize(packet.getTitle());
			title = plugin.customPlaceholders.parseString(title);
			packet.setTitle(MiniMessage.miniMessage().deserialize(title));

			event.markForReEncode(true);
		}
    }
}