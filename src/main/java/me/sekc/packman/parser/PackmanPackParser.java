package me.sekc.packman.parser;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import me.sekc.packman.Packman;
import org.bukkit.configuration.file.YamlConfiguration;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class PackmanPackParser {
	Packman plugin;

	// The key is {PACKNAME, GLYPHNAME}
	public Map<Map.Entry<String, String>, PackmanGlyph> allParsedGlyphs = new HashMap<>(); // Glyphs from all parsed resource packs

	// The key is {PACKNAME, ITEMNAME}
	public Map<Map.Entry<String, String>, PackmanItem> allParsedItems = new HashMap<>(); // Items from all parsed resource packs

	// The key is {PACKNAME, GLYPHNAME}
	public Map<Map.Entry<String, String>, Character> glyphToCharMap = new HashMap<>(); // Which glyph = which char? this is reset when generating glyphs

	public Set<String> packNames = new HashSet<>();

	public PackmanPackParser(Packman plugin) {
		this.plugin = plugin;
	}

	public void parseResourcePack(String packName, File pathToPack) { // This will
		plugin.getLogger().info("Parsing packman pack \"" + packName + "\" at: " + pathToPack.getPath());

		packNames.add(packName);

		if (!parsePackmanYml(packName, pathToPack)) {
			return; // no packman.yml, don't parse this pack!
		}
		PackmanGlyph.parseGlyphsYml(plugin, this, packName, pathToPack);
		PackmanItem.parseItemsYml(plugin, this, packName, pathToPack);
	}

	private boolean parsePackmanYml(String packName, File pathToPack) {
		File packmanYmlFile = new File(pathToPack + "/packman.yml");
		if (!packmanYmlFile.exists()) {
			plugin.getLogger().warning("Pack " + packName + " is invalid! Missing " + packmanYmlFile);
			return false;
		}

		plugin.getLogger().info("Parsing " + packmanYmlFile);
		YamlConfiguration packmanYml = YamlConfiguration.loadConfiguration(packmanYmlFile);

		plugin.getLogger().info("Pack name: " + packmanYml.getString("name"));
		plugin.getLogger().info("Pack description: " + packmanYml.getString("description").stripTrailing());
		plugin.getLogger().info("Pack version: " + packmanYml.getString("version"));

		return true;
	}

	public void generateMinecraftResourcePack(File tempFolder, File outputZip) {
		// delete old temp_pack
		try {
			if (tempFolder.exists())
				FileUtils.deleteDirectory(tempFolder);
		} catch (IOException e) {
			throw new RuntimeException("Failed to delete old pack: " + e);
		}

		// Create new!
		if (!tempFolder.mkdirs()) {
			throw new RuntimeException("Failed to create folder: " + tempFolder);
		}

		generatePackMcmeta(tempFolder);
		PackmanGlyph.generateGlyphs(plugin, this, tempFolder);
		PackmanItem.generateItems(plugin, this, tempFolder);

		// zip it up!
		if (outputZip.exists() && !outputZip.delete()) { // delete old zip
			throw new RuntimeException("Failed to delete old " + outputZip);
		}

		plugin.getLogger().info("Compressing " + tempFolder + " to " + outputZip);
		try {
			pack(tempFolder, outputZip);
		} catch (IOException e) {
			throw new RuntimeException("Failed to zip: " + e);
		}
	}

	private void generatePackMcmeta(File pathToGenerateAt) {
		plugin.getLogger().info("Generating pack.mcmeta");
		File packMcmetaFile = new File(pathToGenerateAt + "/pack.mcmeta");

		try (FileWriter writer = new FileWriter(packMcmetaFile)) {
			writer.write("{\"pack\": {"
				+ "\"description\": \"" + plugin.getConfig().getString("resource-pack.description") + "\""
				+ ", \"min_format\": " + plugin.getConfig().getInt("resource-pack.min-format")
				+ ", \"max_format\": " + plugin.getConfig().getInt("resource-pack.max-format")
				+ "}}\n");
		} catch (IOException e) {
			throw new RuntimeException("Failed to create file: " + packMcmetaFile);
		}
	}

	public static void pack(File sourceDir, File outZip) throws IOException {
		try (ZipOutputStream zs = new ZipOutputStream(Files.newOutputStream(outZip.toPath()))) {
			Files.walk(sourceDir.toPath())
				.filter(path -> !Files.isDirectory(path))
				.forEach(path -> {
					ZipEntry zipEntry = new ZipEntry(sourceDir.toPath().relativize(path).toString());
					try {
						zs.putNextEntry(zipEntry);
						Files.copy(path, zs);
						zs.closeEntry();
					} catch (IOException e) {
						throw new RuntimeException("Failed to zip: " + e);
					}
				});
		}
	}
}
