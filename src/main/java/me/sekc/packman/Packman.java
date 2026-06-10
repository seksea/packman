package me.sekc.packman;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.EventManager;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import me.sekc.packman.commands.CommandManager;
import me.sekc.packman.parser.PackmanGlyph;
import me.sekc.packman.parser.PackmanItem;
import me.sekc.packman.parser.PackmanPackParser;
import me.sekc.packman.server.ResourcePackServer;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public final class Packman extends JavaPlugin {
	public ResourcePackServer resourcePackServer;
	public PackmanPackParser packmanPackParser;
	Map<String, File> packmanPacks = new HashMap<>(); // packs that will be merged to form the final pack when "generatePack()" is ran

	public byte[] resourcePackChecksum; // The SHA-1 hash of the latest pack.zip

	CustomPlaceholders placeholders;

	public void setPack(String packName, File pathToPack) { // sets the pack, so if already exists then it'll update
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

		placeholders = new CustomPlaceholders(this);

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
		getLogger().info("Reloading config.");
		reloadConfig();

		getLogger().info("Regenerating Packman packs.");
		// Generate the pack to dataPath/pack.zip
		generatePack();

		getLogger().info("Restarting the HTTP server.");
		// Restart the HTTPServer
		resourcePackServer.stopServer();
		startHttpServer();
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
	}

	private void startHttpServer() {
		try {
			resourcePackServer = new ResourcePackServer(this, getConfig().getInt("resource-pack-server.port"));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		resourcePackServer.startServer();
	}
}
