package me.sekc.packman;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.EventManager;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import me.sekc.packman.commands.CommandManager;
import me.sekc.packman.parser.PackmanDialog;
import me.sekc.packman.parser.PackmanItem;
import me.sekc.packman.parser.PackmanPackParser;
import me.sekc.packman.placeholders.CustomPlaceholders;
import me.sekc.packman.placeholders.PlaceholderAPIPlaceholders;
import me.sekc.packman.server.ResourcePackServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.ServerLinks;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.net.URI;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.util.*;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

public final class Packman extends JavaPlugin {
	public ResourcePackServer resourcePackServer;
	public PackmanPackParser packmanPackParser;
	public Map<String, File> packmanPacks = new HashMap<>(); // Packs that will be merged to form the final pack when "generatePack()" is ran

	public byte[] resourcePackChecksum; // The SHA-1 hash of the latest pack.zip
	public UUID resourcePackUUID; // UUID of the current pack, we need to use this for removing the resource pack.

	CustomPlaceholders customPlaceholders;
	PlaceholderAPIPlaceholders papiPlaceholders;

	/**
	 * Sets the pack, so if already exists then it'll update, you can call this from your own plugin to add packs from your plugins' folder (preferably in `onEnable`)
	 * @param packName The name of the new pack that you will add.
	 * @param pathToPack The path to the pack.
	 */
	static public void setPack(String packName, File pathToPack) {
		Packman plugin = Packman.getPlugin(Packman.class);
		plugin.getLogger().info("Set pack " + packName + " = " + pathToPack);
		plugin.packmanPacks.put(packName, pathToPack);
	}

	/**
	 * Gets the character that represents a glyph in a packman pack.
	 * <p>You can then use this character anywhere and the client will render it as the custom glyph,
	 * this is used for custom chest GUIs, emojis, etc</p>
	 *
	 * @param packName The name of the pack you want to get a glyph from
	 * @param glyphName The name of the glyph you want to get
	 * @return The resulting glyph, will be null if no pack/glyph match is found!
	 */
	static public Character getGlyphFromPack(String packName, String glyphName) {
		Packman plugin = Packman.getPlugin(Packman.class);
		return plugin.packmanPackParser.glyphToCharMap.getOrDefault(Map.entry(packName, glyphName), null);
	}

	/**
	 * Generates a sequence of glyphs that will cause whatever text follows it to be shifted right/left by a specified amount
	 * <p>This is helpful for aligning chest GUIs</p>
	 *
	 * @param shift How much should this string shift the text? positive = right, negative = left
	 * @return The string of glyphs that will cause text to be shifted.
	 */
	static public String getShiftGlyphString(int shift) {
		Packman plugin = Packman.getPlugin(Packman.class);

		List<Integer> possibleShiftList = List.of(64, 32, 16, 8, 4, 2, 1); // must be sorted biggest -> smallest
		boolean positive = shift > 0;
		int remaining = Math.abs(shift);
		String glyphString = "";
		while (remaining > 0) {
			for (int possibleShift: possibleShiftList) {
				if (possibleShift <= remaining) {
					remaining -= possibleShift;
					Character glyphChar = plugin.packmanPackParser.spaceProviderGlyphs.get(positive ? possibleShift : -possibleShift);
					glyphString += glyphChar;
				}
			}
		}
		return glyphString;
	}

	/**
	 * Gets a custom item from the pack and returns it as an item stack
	 * <p>This allows you to gift players custom items, or show it in chest GUIs</p>
	 *
	 * @param packName The name of the pack you want to get an item from
	 * @param itemName The name of the item you want to get
	 * @return The custom item as an ItemStack
	 */
	static public ItemStack getCustomItemStack(String packName, String itemName) {
		Packman plugin = Packman.getPlugin(Packman.class);

		PackmanItem packmanItem = plugin.packmanPackParser.allParsedItems.get(Map.entry(packName, itemName));

		ItemStack result = ItemStack.of(packmanItem.baseMaterial);
		ItemMeta meta = result.getItemMeta();

		meta.setItemModel(new NamespacedKey("packman", packName + "_" + itemName));
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
		Metrics metrics = new Metrics(this, 32013);

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
		getLogger().info("Unregistering dialogs");
		PackmanDialog.unregisterAllDialogs();
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
			Packman.setPack(pack.getName(), pack);
		}
	}

	private void generatePack() {
		getLogger().info("Generating pack...");

		// Parse the packman packs
		packmanPackParser = new PackmanPackParser(this);

		// Delete .tempZips as we will re-extract them
		File tempZipsFolder = new File(getDataFolder() + "/packs/.tempZips");
		try {
			Files.walkFileTree(tempZipsFolder.toPath(), new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					Files.delete(file);
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
					Files.delete(dir);
					return FileVisitResult.CONTINUE;
				}
			});
		} catch (Exception e) {
			getLogger().warning(e.toString());
		}

		for (Map.Entry<String, File> pack : packmanPacks.entrySet()) {
			String extension = "";

			String packName = pack.getKey().split("\\.")[0];
			String pathString = pack.getValue().getPath();
			int i = pathString.lastIndexOf('.');
			if (i > 0) {
				extension = pathString.substring(i+1);
			}

			if (extension.equals("zip")) { // unzip to ".tempZips"
				pathString = new File(tempZipsFolder + File.separator + packName).getPath();
				FileInputStream fis;
				byte[] buffer = new byte[1024];
				try {
					fis = new FileInputStream(pack.getValue());
					ZipInputStream zis = new ZipInputStream(fis);
					ZipEntry ze = zis.getNextEntry();
					while(ze != null){
						if (!ze.isDirectory()) {
							String fileName = ze.getName();
							File newFile = new File(pathString + File.separator + fileName);
							getLogger().info("Unzipping to " + newFile.getAbsolutePath());
							//create directories for sub directories in zip
							new File(newFile.getParent()).mkdirs();
							FileOutputStream fos = new FileOutputStream(newFile);
							int len;
							while ((len = zis.read(buffer)) > 0) {
								fos.write(buffer, 0, len);
							}
							fos.close();
							//close this ZipEntry
							zis.closeEntry();
						}
						ze = zis.getNextEntry();
					}
					//close last ZipEntry
					zis.closeEntry();
					zis.close();
					fis.close();
				} catch (IOException e) {
					getLogger().warning("Failed to unzip " + pack.getValue() + " from " + pack.getKey() + ", error: " + e);
				}

			}

			packmanPackParser.parseResourcePack(pack.getKey(), new File(pathString));
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
