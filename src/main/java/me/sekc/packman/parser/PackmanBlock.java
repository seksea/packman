package me.sekc.packman.parser;

import com.google.gson.*;
import io.papermc.paper.registry.data.InstrumentRegistryEntry;
import io.papermc.paper.registry.keys.InstrumentKeys;
import io.papermc.paper.registry.keys.tags.InstrumentTagKeys;
import me.sekc.packman.Packman;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Instrument;
import org.bukkit.Material;
import org.bukkit.block.data.type.NoteBlock;
import org.bukkit.configuration.file.YamlConfiguration;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PackmanBlock {
	public File texturePath;
	public String type;

	public static class NoteblockID {
		private int id;

		public NoteblockID(int id) {
			this.id = id;
		}

		public NoteblockID(String instrument, int note) {
			int i = 0;
			for (String ins : instruments) {
				if (ins.equals(instrument)) {
					break;
				}
				i++;
			}

			if (i == instruments.size()) {
				throw new RuntimeException("You have more blocks than the limit!!! Packman cannot add any more blocks, please remove some!!");
			}

			this.id = (i * 24) + note;
		}

		static List<String> instruments = List.of(
				"banjo",
				"basedrum",
				"bass",
				"bell",
				"bit",
				"chime",
				"cow_bell",
				"creeper",
				"custom_head",
				"didgeridoo",
				"dragon",
				"flute",
				"guitar",
				"harp",
				"hat",
				"iron_xylophone",
				"piglin",
				"pling",
				"skeleton",
				"snare",
				"trumpet",
				"trumpet_exposed",
				"trumpet_oxidized",
				"trumpet_weathered",
				"wither_skeleton",
				"xylophone",
				"zombie"
		);

		public int toID() {
			return id;
		}

		public Map.Entry<String, Integer> toInstrumentNote() {
			return Map.entry(instruments.get(id/24), id % 24);
		}
	}

	PackmanBlock(HashMap<String, ?> config, File pathToConfig) { // Hashmap is from yaml `getList`
		texturePath = new File(pathToConfig.getParentFile() + "/" + config.get("texture"));
		type = (String)config.get("type");
	}

	static public void parseBlocksYml(Packman plugin, PackmanPackParser parser, String packName, File pathToPack) {
		File blocksYmlFile = new File(pathToPack + "/blocks/blocks.yml");
		if (blocksYmlFile.exists()) {
			plugin.getLogger().info("Parsing " + blocksYmlFile);
			YamlConfiguration blockYml =  YamlConfiguration.loadConfiguration(blocksYmlFile);

			List<?> blockConfigs = blockYml.getList("blocks");
			for (Object blockConfigObj : blockConfigs) {
				if ((blockConfigObj instanceof HashMap<?,?>)) {
					HashMap<String,?> blockConfig = (HashMap<String,?>)blockConfigObj;

					String blockName = (String)blockConfig.get("name");
					plugin.getLogger().info("Parsing block " + blockName);
					parser.allParsedBlocks.put(Map.entry(packName, blockName), new PackmanBlock(blockConfig, blocksYmlFile));
				}
			}
		}
	}

	static public void generateBlocks(Packman plugin, PackmanPackParser parser, File pathToGenerateAt) {
		if (parser.allParsedBlocks.isEmpty())
			return;

		File noteblockIDJsonFile = new File(plugin.getDataFolder() + "/noteblockids.json");
		JsonObject noteblockIDJson = new JsonObject();
		if (noteblockIDJsonFile.exists()) {
			plugin.getLogger().info("Loading previous noteblockid data from packman/noteblockids.json");

			try (FileReader reader = new FileReader(noteblockIDJsonFile)) {
				JsonElement jsonElement = JsonParser.parseReader(reader);
				noteblockIDJson = jsonElement.getAsJsonObject();
			} catch (IOException e) {
				throw new RuntimeException("Failed to read file: " + noteblockIDJsonFile);
			}
		} else {
			plugin.getLogger().warning("Couldn't find noteblockid data at packman/noteblockids.json, creating the data");

			try (FileWriter writer = new FileWriter(noteblockIDJsonFile)) {
				writer.write(noteblockIDJson.toString());
			} catch (IOException e) {
				throw new RuntimeException("Failed to create file: " + noteblockIDJsonFile);
			}
		}

		plugin.getLogger().info("Generating blocks");

		File noteblockBlockstatesFolder = new File(pathToGenerateAt + "/assets/minecraft/blockstates/");
		if (!noteblockBlockstatesFolder.mkdirs()) {
			throw new RuntimeException("Failed to create folder: " + noteblockBlockstatesFolder);
		}
		File noteblockBlockstatesFile = new File(noteblockBlockstatesFolder + "/note_block.json");

		JsonObject noteblockBlockstatesJson = new JsonObject();
		JsonArray noteblockBlockstatesMultipartJson = new JsonArray();

		File blockModelFolder = new File(pathToGenerateAt + "/assets/packman/models/block");
		if (!blockModelFolder.mkdirs()) {
			throw new RuntimeException("Failed to create folder: " + blockModelFolder);
		}

		File blockTexturesFolder = new File(pathToGenerateAt + "/assets/packman/textures/block");
		if (!blockTexturesFolder.mkdirs()) {
			throw new RuntimeException("Failed to create folder: " + blockTexturesFolder);
		}

		for (Map.Entry<Map.Entry<String, String>, PackmanBlock> block : parser.allParsedBlocks.entrySet()) {
			String packName = block.getKey().getKey();
			String blockName = block.getKey().getValue();

			NoteblockID noteblockID = null; // this blocks' noteblockID, either from noteblockids.json, or if it's a newly added pack then it is generated from unused ids in the noteblockID json

			// search the noteblockID json for this block
			for (Map.Entry<String, JsonElement> obj : noteblockIDJson.asMap().entrySet()) {
				if (obj.getValue().getAsString().equals(packName + ":" + blockName)) {
					noteblockID = new NoteblockID(Integer.parseInt(obj.getKey()));
				}
			}

			if (noteblockID == null) { // if not in the noteblockids.json, use the next available id
				noteblockID = new NoteblockID(noteblockIDJson.size());

				noteblockIDJson.add(Integer.toString(noteblockID.toID()), new JsonPrimitive(packName + ":" + blockName));
			}

			plugin.getLogger().info("Generating block " + packName + " " + blockName);

			// Generate the noteblock override
			JsonObject blockNoteblockOverrideJson = new JsonObject();

			JsonObject blockNoteblockOverrideWhenJson = new JsonObject();
			Map.Entry<String, Integer> instrumentNote = noteblockID.toInstrumentNote();
			blockNoteblockOverrideWhenJson.add("instrument", new JsonPrimitive(instrumentNote.getKey()));
			blockNoteblockOverrideWhenJson.add("note", new JsonPrimitive(instrumentNote.getValue()));
			blockNoteblockOverrideWhenJson.add("powered", new JsonPrimitive(false));
			blockNoteblockOverrideJson.add("when", blockNoteblockOverrideWhenJson);

			JsonObject blockNoteblockOverrideApplyJson = new JsonObject();
			blockNoteblockOverrideApplyJson.add("model", new JsonPrimitive("packman:block/" + packName + "." + blockName));
			blockNoteblockOverrideJson.add("apply", blockNoteblockOverrideApplyJson);

			noteblockBlockstatesMultipartJson.add(blockNoteblockOverrideJson);

			File blockModelFile = new File(blockModelFolder + "/"  + packName + "." + blockName + ".json");

			// Generate the blocks' custom model
			JsonObject blockModelJson = new JsonObject();
			blockModelJson.add("parent", new JsonPrimitive("minecraft:block/cube_all"));
			JsonObject texturesJson = new JsonObject();
			texturesJson.add("all", new JsonPrimitive("packman:block/" + packName + "." + blockName));
			blockModelJson.add("textures", texturesJson);

			// Write to filepackName + "." + blockName
			try (FileWriter writer = new FileWriter(blockModelFile)) {
				writer.write(blockModelJson.toString());
			} catch (IOException e) {
				throw new RuntimeException("Failed to create file: " + blockModelFile);
			}

			// copy texture to pack
			try {
				FileUtils.copyFile(block.getValue().texturePath, new File(blockTexturesFolder + "/"  + packName + "." + blockName + ".png"));
			} catch (Exception e) {
				throw new RuntimeException("Failed to copy texture to pack: " + block.getValue().texturePath + " -> " + new File(blockTexturesFolder + "/" + packName + "." + blockName + ".png") + ": " + e);
			}
		}

		// write the new noteblock data
		plugin.getLogger().info("updating packman/noteblockids.json");

		try (FileWriter writer = new FileWriter(noteblockIDJsonFile)) {
			writer.write(noteblockIDJson.toString());
		} catch (IOException e) {
			throw new RuntimeException("Failed to create file: " + noteblockIDJsonFile);
		}

		// Create the noteblock override
		noteblockBlockstatesJson.add("multipart", noteblockBlockstatesMultipartJson);
		try (FileWriter writer = new FileWriter(noteblockBlockstatesFile)) {
			writer.write(noteblockBlockstatesJson.toString());
		} catch (IOException e) {
			throw new RuntimeException("Failed to create file " + noteblockBlockstatesFile + " err: " + e);
		}
	}
}
