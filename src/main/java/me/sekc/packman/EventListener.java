package me.sekc.packman;

import net.kyori.adventure.resource.ResourcePackRequest;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;


public class EventListener implements Listener {
	Packman plugin;

	EventListener(Packman plugin) {
		this.plugin = plugin;
	}

	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event) {
		event.getPlayer().setResourcePack( // Tell the players' client to download the pack from our HTTP server
			plugin.resourcePackUUID,
			"http://" + plugin.getConfig().getString("resource-pack-server.server-ip") + ":" + plugin.getConfig().getInt("resource-pack-server.port"),
			plugin.resourcePackChecksum,
			"test",
			true
		);
	}

}
