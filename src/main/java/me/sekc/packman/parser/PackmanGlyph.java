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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PackmanGlyph {
	public File texturePath;
	public int height;
	public int ascent;

	PackmanGlyph(HashMap<String, ?> config, File pathToConfig) { // Hashmap is from yaml `getList`
		texturePath = new File(pathToConfig.getParentFile() + "/" + config.get("texture"));
		height = (int)config.get("height");
		ascent = (int)config.get("ascent");

		if (ascent > height) {
			Packman.getPlugin(Packman.class).getLogger().warning("Glyph with texture " + texturePath + " has ascent (" + ascent + ") > height(" + height + ")!!! clamping ascent to be equal to height.");
			ascent = height;
		}
	}

	static public void parseGlyphsYml(Packman plugin, PackmanPackParser parser, String packName, File pathToPack) {
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
					parser.allParsedGlyphs.put(Map.entry(packName, glyphName), new PackmanGlyph(glyphConfig, glyphYmlFile));
				}
			}
		}
	}

	//					// new curGlyph, glyphProviderObj
	static private Map.Entry<Character, JsonObject> generateSpaceProviderGlyphs(PackmanPackParser parser, char curGlyph) {
		JsonObject glyphObject = new JsonObject();
		glyphObject.add("type", new JsonPrimitive("space"));
		JsonObject advancesArray = new JsonObject();

		for (int i = 0; i <= 6; i++) { // 0, 1, 2, 4, 8, ..., 64
			int shift = Math.powExact(2, i);
			advancesArray.add("" + (char)curGlyph, new JsonPrimitive(shift));
			parser.spaceProviderGlyphs.put(shift, curGlyph);
			curGlyph++;

			shift = -Math.powExact(2, i);
			advancesArray.add("" + (char)curGlyph, new JsonPrimitive(shift));
			parser.spaceProviderGlyphs.put(shift, curGlyph);
			curGlyph++;
		}

		glyphObject.add("advances", advancesArray);

		return Map.entry(curGlyph, glyphObject);
	}

	static public void generateGlyphs(Packman plugin, PackmanPackParser parser, File pathToGenerateAt) {
		parser.glyphToCharMap.clear(); // clear the glyph map as we will repopulate it

		if (parser.allParsedGlyphs.isEmpty())
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

		// Add space providers
		char curGlyph = (char)plugin.getConfig().getInt("resource-pack.glyphs.range-min");

		Map.Entry<Character, JsonObject> spaceProviderJson = generateSpaceProviderGlyphs(parser, curGlyph);
		curGlyph = spaceProviderJson.getKey();

		fontDefaultJsonProvidersList.add(spaceProviderJson.getValue());

		// Add glyphs
		for (Map.Entry<Map.Entry<String, String>, PackmanGlyph> glyph : parser.allParsedGlyphs.entrySet()) {
			String packName = glyph.getKey().getKey();
			String glyphName = glyph.getKey().getValue();

			File fontTexturesPackFolder = new File(fontTexturesFolder + "/" + packName);
			if (!fontTexturesPackFolder.exists() && !fontTexturesPackFolder.mkdirs()) {
				throw new RuntimeException("Failed to create folder: " + fontTexturesFolder);
			}

			// copy texture to pack
			try {
				FileUtils.copyFile(glyph.getValue().texturePath, new File(fontTexturesPackFolder + "/" + glyphName + ".png"));
			} catch (Exception e) {
				throw new RuntimeException("Failed to copy texture to pack: " + glyph.getValue().texturePath + " -> " + new File(fontTexturesPackFolder + "/" + glyphName + ".png") + ": " + e);
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
			parser.glyphToCharMap.put(Map.entry(packName, glyphName), curGlyph);

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
}
