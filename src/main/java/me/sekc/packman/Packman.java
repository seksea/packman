package me.sekc.packman;

import me.sekc.packman.parser.PackmanGlyph;
import me.sekc.packman.parser.PackmanPackParser;
import me.sekc.packman.server.ResourcePackServer;
import org.bukkit.Bukkit;
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
	ResourcePackServer resourcePackServer;
	Map<String, File> packmanPacks = new HashMap<>(); // packs that will be merged to form the final pack when "generatePack()" is ran

	public byte[] resourcePackChecksum; // The SHA-1 hash of the latest pack.zip

	public void setPack(String packName, File pathToPack) { // sets the pack, so if already exists then it'll update
		getLogger().info("Set pack " + packName + " = " + pathToPack);
		packmanPacks.put(packName, pathToPack);
	}

	@Override
	public void onEnable() {
		addPacksFromDataFolder();

		getServer().getPluginManager().registerEvents(new EventListener(this), this);

		// Run after 1 tick to ensure all plugins have enabled and thus added their packs
		Bukkit.getScheduler().runTaskLater(this, () -> {
			// Generate the pack to dataPath/pack.zip
			generatePack();

			startHttpServer();
		}, 1L);
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
		PackmanPackParser parser = new PackmanPackParser(this);

		for (Map.Entry<String, File> pack : packmanPacks.entrySet()) {
			parser.parseResourcePack(pack.getKey(), pack.getValue());
		}

		// Generate the Minecraft resource pack to pack.zip
		File tempFolder = new File(getDataFolder() + "/temp_pack");
		parser.generateMinecraftResourcePack(
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
