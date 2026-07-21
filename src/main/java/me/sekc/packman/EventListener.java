package me.sekc.packman;

import net.kyori.adventure.resource.ResourcePackRequest;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
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

	@EventHandler
	public void onPlayerInteractEvent(PlayerInteractEvent event) {
		if (event.getAction().isRightClick()) {
			if (event.getMaterial().equals(Material.NOTE_BLOCK)) {
				event.setCancelled(true); // Don't let players place note blocks
				event.getPlayer().sendMessage(plugin.getMessage("noteblock-warning"));
			}
			if (event.getClickedBlock().getBlockData().getMaterial().equals(Material.NOTE_BLOCK)) {
				event.setCancelled(true); // Player right-clicked a note block (custom block)!
			}
		}

		if (event.getAction().isLeftClick()) {
			if (event.getClickedBlock().getBlockData().getMaterial().equals(Material.NOTE_BLOCK)) {
				event.setCancelled(true); // Player left-clicked a note block (custom block)!
			}
		}
	}
}
