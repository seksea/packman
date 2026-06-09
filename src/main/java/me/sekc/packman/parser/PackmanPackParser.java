package me.sekc.packman.parser;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import me.sekc.packman.Packman;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.util.FileUtil;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class PackmanPackParser {
	Packman plugin;

	// The key is {PACKNAME, GLYPHNAME}
	Map<Map.Entry<String, String>, PackmanGlyph> allParsedGlyphs = new HashMap<>(); // Glyphs from all parsed resource packs

	// The key is {PACKNAME, GLYPHNAME}
	public Map<Map.Entry<String, String>, Character> glyphToCharMap = new HashMap<>(); // Which glyph = which char? this is reset when generating glyphs

	public PackmanPackParser(Packman plugin) {
		this.plugin = plugin;
	}

	public void parseResourcePack(String packName, File pathToPack) { // This will
		plugin.getLogger().info("Parsing packman pack \"" + packName + "\" at: " + pathToPack.getPath());


		if (!parsePackmanYml(packName, pathToPack)) {
			return; // no packman.yml, don't parse this pack!
		}
		parseGlyphsYml(packName, pathToPack);
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
		plugin.getLogger().info("Pack description: " + packmanYml.getString("description"));
		plugin.getLogger().info("Pack version: " + packmanYml.getString("version"));

		return true;
	}

	private void parseGlyphsYml(String packName, File pathToPack) {
		File glyphYmlFile = new File(pathToPack + "/glyphs/glyphs.yml");
		if (glyphYmlFile.exists()) {
			plugin.getLogger().info("Parsing " + glyphYmlFile);
			YamlConfiguration glyphYml =  YamlConfiguration.loadConfiguration(glyphYmlFile);

			List<?> glyphConfigs = glyphYml.getList("glyphs");
			for (Object glyphConfigObj : glyphConfigs) {
				if ((glyphConfigObj instanceof HashMap<?,?>)) {
					HashMap<String,?> glyphConfig = (HashMap<String,?>)glyphConfigObj;

					String glyphName = (String)glyphConfig.get("name");
					plugin.getLogger().info("Parsing glyph " + glyphName);
					this.allParsedGlyphs.put(Map.entry(packName, glyphName), new PackmanGlyph(glyphConfig, glyphYmlFile));
				}
			}
		}
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
		generateGlyphs(tempFolder);

		// zip it up!
		if (!outputZip.delete()) { // delete old zip
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

	private void generateGlyphs(File pathToGenerateAt) {
		this.glyphToCharMap.clear(); // clear the glyph map as we will repopulate it

		if (this.allParsedGlyphs.isEmpty())
			return;

		plugin.getLogger().info("Generating Glyphs");
		File fontFolder = new File(pathToGenerateAt + "/assets/minecraft/font");
		if (!fontFolder.mkdirs()) {
			throw new RuntimeException("Failed to create folder: " + fontFolder);
		}
		File fontDefaultJsonFile = new File(fontFolder + "/default.json");

		File fontTexturesFolder = new File(pathToGenerateAt + "/assets/minecraft/textures/packman/glyphs");
		if (!fontTexturesFolder.mkdirs()) {
			throw new RuntimeException("Failed to create folder: " + fontTexturesFolder);
		}

		// Generate contents
		JsonObject fontDefaultJson = new JsonObject();
		JsonArray fontDefaultJsonProvidersList = new JsonArray();

		char curGlyph = (char)plugin.getConfig().getInt("resource-pack.glyphs.range-min");
		for (Map.Entry<Map.Entry<String, String>, PackmanGlyph> glyph : this.allParsedGlyphs.entrySet()) {
			String packName = glyph.getKey().getKey();
			String glyphName = glyph.getKey().getValue();

			File fontTexturesPackFolder = new File(fontTexturesFolder + "/" + packName);
			if (!fontTexturesPackFolder.exists() && !fontTexturesPackFolder.mkdirs()) {
				throw new RuntimeException("Failed to create folder: " + fontTexturesFolder);
			}

			// copy texture to pack
			try {
				FileUtils.copyFile(glyph.getValue().imagePath, new File(fontTexturesPackFolder + "/" + glyphName + ".png"));
			} catch (Exception e) {
				throw new RuntimeException("Failed to copy texture to pack: " + glyph.getValue().imagePath + " -> " + new File(fontTexturesPackFolder + "/" + glyphName + ".png") + ": " + e);
			}

			// Add the glyph to the font/default.json
			JsonObject glyphObject = new JsonObject();
			glyphObject.add("type", new JsonPrimitive("bitmap"));
			glyphObject.add("file", new JsonPrimitive("minecraft:packman/glyphs/" + packName + "/" + glyphName + ".png"));
			glyphObject.add("ascent", new JsonPrimitive(glyph.getValue().ascent));
			glyphObject.add("height", new JsonPrimitive(glyph.getValue().height));
			JsonArray charsArray = new JsonArray();
			charsArray.add("" + (char)curGlyph);
			glyphObject.add("chars", charsArray);

			fontDefaultJsonProvidersList.add(glyphObject);

			// Add the char to the glyphToCharMap
			this.glyphToCharMap.put(Map.entry(packName, glyphName), curGlyph);

			curGlyph++;
		}

		fontDefaultJson.add("providers", fontDefaultJsonProvidersList);

		// Write to file
		try (FileWriter writer = new FileWriter(fontDefaultJsonFile)) {
			writer.write(fontDefaultJson.toString());
		} catch (IOException e) {
			throw new RuntimeException("Failed to create file: " + fontDefaultJsonFile);
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
