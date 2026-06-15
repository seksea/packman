package me.sekc.packman;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.EventManager;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import me.sekc.packman.commands.CommandManager;
import me.sekc.packman.parser.PackmanItem;
import me.sekc.packman.parser.PackmanPackParser;
import me.sekc.packman.placeholders.CustomPlaceholders;
import me.sekc.packman.placeholders.PlaceholderAPIPlaceholders;
import me.sekc.packman.server.ResourcePackServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.*;

public final class Packman extends JavaPlugin {
	public ResourcePackServer resourcePackServer;
	public PackmanPackParser packmanPackParser;
	public Map<String, File> packmanPacks = new HashMap<>(); // Packs that will be merged to form the final pack when "generatePack()" is ran

	public byte[] resourcePackChecksum; // The SHA-1 hash of the latest pack.zip
	public UUID resourcePackUUID; // UUID of the current pack, we need to use this for removing the resource pack.

	CustomPlaceholders customPlaceholders;
	PlaceholderAPIPlaceholders papiPlaceholders;

	public void setPack(String packName, File pathToPack) { // Sets the pack, so if already exists then it'll update, you can call this from your own plugin to add packs from your plugins' folder
		getLogger().info("Set pack " + packName + " = " + pathToPack);
		packmanPacks.put(packName, pathToPack);
	}

	public ItemStack getCustomItemStack(String pack_name, String item_name) {
		PackmanItem packmanItem = packmanPackParser.allParsedItems.get(Map.entry(pack_name, item_name));

		ItemStack result = ItemStack.of(packmanItem.baseMaterial);
		ItemMeta meta = result.getItemMeta();

		meta.setItemModel(new NamespacedKey("packman", pack_name + "_" + item_name));
		meta.itemName(packmanItem.displayName);
		meta.lore(packmanItem.lore);

		result.setItemMeta(meta);
		return result;
	}

	@Override
	public void onLoad() {
		// Building, loading, and initializing the library is necessary when bundling.
		PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this));
		PacketEvents.getAPI().load();

		EventManager events = PacketEvents.getAPI().getEventManager();
		events.registerListener(new PacketEventsListener(this), PacketListenerPriority.MONITOR);
	}

	@Override
	public void onEnable() {
		PacketEvents.getAPI().init();

		customPlaceholders = new CustomPlaceholders(this);

		if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) {
			getLogger().warning("Could not find PlaceholderAPI, it will not be used.");
		} else {
			getLogger().info("Creating PlaceholderAPI expansion...");
			papiPlaceholders = new PlaceholderAPIPlaceholders(this);
			papiPlaceholders.register();
		}

		if (!(new File(getDataFolder() + "/config.yml").exists())) {
			saveResource("config.yml", false);
		}

		addPacksFromDataFolder();

		getServer().getPluginManager().registerEvents(new EventListener(this), this);

		CommandManager.registerCommands(this);

		// Run after 1 tick to ensure all plugins have enabled and thus added their packs
		Bukkit.getScheduler().runTaskLater(this, () -> {
			// Generate the pack to dataPath/pack.zip
			generatePack();

			startHttpServer();
		}, 1L);
	}

	@Override
	public void onDisable() {
		PacketEvents.getAPI().terminate();
	}

	public void reload() {
		getLogger().info("Removing old pack from all players");
		Collection<? extends Player> players = Bukkit.getOnlinePlayers();

		for (Player player : players) {
			player.removeResourcePack(resourcePackUUID);
		}

		getLogger().info("Reloading config.");
		reloadConfig();

		getLogger().info("Regenerating Packman packs.");
		// Generate the pack to dataPath/pack.zip
		generatePack();

		getLogger().info("Restarting the HTTP server.");
		// Restart the HTTPServer
		resourcePackServer.stopServer();
		startHttpServer();

		for (Player player : players) {
			player.setResourcePack( // Tell the players' client to download the pack from our HTTP server
				resourcePackUUID,
				"http://" + getConfig().getString("resource-pack-server.server-ip") + ":" + getConfig().getInt("resource-pack-server.port"),
				resourcePackChecksum,
				"test",
				true
			);
		}
	}

	private void addPacksFromDataFolder() {
		File packsFolder = new File(getDataFolder() + "/packs");

		if (!packsFolder.exists() && !packsFolder.mkdirs()) {
			throw new RuntimeException("Could not create packs folder at: " + packsFolder);
		}

		for (File pack : packsFolder.listFiles()) {
			setPack(pack.getName(), pack);
		}
	}

	private void generatePack() {
		getLogger().info("Generating pack...");

		// Parse the packman packs
		packmanPackParser = new PackmanPackParser(this);

		for (Map.Entry<String, File> pack : packmanPacks.entrySet()) {
			packmanPackParser.parseResourcePack(pack.getKey(), pack.getValue());
		}

		// Generate the Minecraft resource pack to pack.zip
		File tempFolder = new File(getDataFolder() + "/temp_pack");
		packmanPackParser.generateMinecraftResourcePack(
			tempFolder,
			new File(getDataFolder() + "/pack.zip")
		);

		getLogger().info("Getting SHA-1 hash.");

		// get the SHA-1 hash
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-1");
			try (InputStream fis = new FileInputStream(new File(getDataFolder() + "/pack.zip"))) {
				int n = 0;
				byte[] buffer = new byte[8192];
				while (n != -1) {
					n = fis.read(buffer);
					if (n > 0) {
						digest.update(buffer, 0, n);
					}
				}
				this.resourcePackChecksum = digest.digest();
				getLogger().info("SHA-1 hash is: " + Base64.getEncoder().encodeToString(this.resourcePackChecksum));
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		this.resourcePackUUID = UUID.randomUUID();
		getLogger().info("The UUID of this pack is: " + this.resourcePackUUID.toString());
	}

	private void startHttpServer() {
		try {
			resourcePackServer = new ResourcePackServer(this, getConfig().getInt("resource-pack-server.port"));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		resourcePackServer.startServer();
	}

	public Component getMessage(String name, List<Map.Entry<String, String>> placeholders) {
		String result = getConfig().getString("messages."+name);

		if (result == null) result = "MISSING message " + name + " IN config.yml";

		if (placeholders != null) {
			for (Map.Entry<String, String> placeholder: placeholders) {
				result = result.replace(placeholder.getKey(), placeholder.getValue());
			}
		}

		return MiniMessage.miniMessage().deserialize(result);
	}

	public Component getMessage(String name) {
		return getMessage(name, null);
	}
}
