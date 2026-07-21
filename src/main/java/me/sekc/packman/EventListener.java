package me.sekc.packman;

import net.kyori.adventure.resource.ResourcePackRequest;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;


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
		Block clickedBlock = event.getClickedBlock();

		ItemStack item = event.getItem();
		if (item == null) {
			// we have to do this check, as if you right-click with an item and the base item is not an item that
			// you can normally right-click with (e.g feather), then it seems to always come back null.
			if (event.getHand() == EquipmentSlot.HAND)
				item = event.getPlayer().getInventory().getItemInMainHand();
			else if (event.getHand() == EquipmentSlot.OFF_HAND)
				item = event.getPlayer().getInventory().getItemInOffHand();
		}

		if (item.isEmpty()) item = null; // if air or 0 stack just set it to null

		if (event.getAction().isRightClick()) {
			if (event.getMaterial().equals(Material.NOTE_BLOCK)) {
				event.setCancelled(true); // Don't let players place  note blocks
				event.getPlayer().sendMessage(plugin.getMessage("noteblock-warning"));
			}

			if (clickedBlock != null) {
				// The player has right-clicked a block
				if (clickedBlock.getBlockData().getMaterial().equals(Material.NOTE_BLOCK)) {
					event.setCancelled(true); // Player right-clicked a note block (custom block)!
				}
			}

			if (item != null) {
				Packman.PackmanItemData customItem = Packman.getCustomItemFromItemStack(item);
				if (customItem != null) { // there is a custom item in the hand!
					plugin.getLogger().info("Player right clicked with a packman item in hand: " + customItem.packName + "." + customItem.itemName);
				}
			}
		} else if (event.getAction().isLeftClick()) {
			if (clickedBlock != null) {
				// The player has left-clicked a block
				if (clickedBlock.getBlockData().getMaterial().equals(Material.NOTE_BLOCK)) {
					event.setCancelled(true); // Player left-clicked a note block (custom block)!
				}
			}
		}
	}
}
