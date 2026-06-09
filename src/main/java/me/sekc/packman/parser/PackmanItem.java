package me.sekc.packman.parser;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import me.sekc.packman.Packman;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PackmanItem {
	public File texturePath;
	public Component displayName;
	public List<Component> lore = new ArrayList<>();
	public Material baseMaterial;

	PackmanItem(HashMap<String, ?> config, File pathToConfig) { // Hashmap is from yaml `getList`
		texturePath = new File(pathToConfig.getParentFile() + "/" + config.get("texture"));
		displayName = MiniMessage.miniMessage().deserialize((String)config.get("display_name"));
		baseMaterial = Material.getMaterial((String)config.get("base_material"));
		if (baseMaterial == null)
			throw new RuntimeException("base_material " + baseMaterial + " is invalid.");

		String[] loreLines = ((String)config.get("lore")).stripLeading().stripTrailing().split("\\n");

		for (String loreLine : loreLines) {
			lore.add(MiniMessage.miniMessage().deserialize(loreLine));
		}
	}

	static public void parseItemsYml(Packman plugin, PackmanPackParser parser, String packName, File pathToPack) {
		File itemsYmlFile = new File(pathToPack + "/items/items.yml");
		if (itemsYmlFile.exists()) {
			plugin.getLogger().info("Parsing " + itemsYmlFile);
			YamlConfiguration itemYml =  YamlConfiguration.loadConfiguration(itemsYmlFile);

			List<?> itemConfigs = itemYml.getList("items");
			for (Object itemConfigObj : itemConfigs) {
				if ((itemConfigObj instanceof HashMap<?,?>)) {
					HashMap<String,?> itemConfig = (HashMap<String,?>)itemConfigObj;

					String itemName = (String)itemConfig.get("name");
					plugin.getLogger().info("Parsing item " + itemName);
					parser.allParsedItems.put(Map.entry(packName, itemName), new PackmanItem(itemConfig, itemsYmlFile));
				}
			}
		}
	}

	static public void generateItems(Packman plugin, PackmanPackParser parser, File pathToGenerateAt) {
		if (parser.allParsedItems.isEmpty())
			return;

		plugin.getLogger().info("Generating items");

		File itemsFolder = new File(pathToGenerateAt + "/assets/packman/items");
		if (!itemsFolder.mkdirs()) {
			throw new RuntimeException("Failed to create folder: " + itemsFolder);
		}

		File customModelsFolder = new File(pathToGenerateAt + "/assets/packman/models/item");
		if (!customModelsFolder.mkdirs()) {
			throw new RuntimeException("Failed to create folder: " + customModelsFolder);
		}

		File itemTexturesFolder = new File(pathToGenerateAt + "/assets/packman/textures/item");
		if (!itemTexturesFolder.mkdirs()) {
			throw new RuntimeException("Failed to create folder: " + itemTexturesFolder);
		}

		for (Map.Entry<Map.Entry<String, String>, PackmanItem> item : parser.allParsedItems.entrySet()) {
			String packName = item.getKey().getKey();
			String itemName = item.getKey().getValue();

			plugin.getLogger().info("Generating item " + packName + " " + itemName);

			File itemDeclarationFile = new File(itemsFolder + "/"  + packName + "_" + itemName + ".json");

			// Generate the items declaration
			JsonObject itemDeclarationJson = new JsonObject();
			JsonObject itemDeclarationModelJson = new JsonObject();
			itemDeclarationModelJson.add("type", new JsonPrimitive("minecraft:model"));
			itemDeclarationModelJson.add("model", new JsonPrimitive("packman:item/" + packName + "_" + itemName));
			itemDeclarationJson.add("model", itemDeclarationModelJson);

			// Write to file
			try (FileWriter writer = new FileWriter(itemDeclarationFile)) {
				writer.write(itemDeclarationJson.toString());
			} catch (IOException e) {
				throw new RuntimeException("Failed to create file: " + itemDeclarationFile);
			}


			File customModelFile = new File(customModelsFolder + "/"  + packName + "_" + itemName + ".json");

			// Generate the items custom model
			JsonObject customModelJson = new JsonObject();
			customModelJson.add("parent", new JsonPrimitive("minecraft:item/generated"));
			JsonObject texturesJson = new JsonObject();
			texturesJson.add("layer0", new JsonPrimitive("packman:item/" + packName + "_" + itemName));
			customModelJson.add("textures", texturesJson);

			// Write to file
			try (FileWriter writer = new FileWriter(customModelFile)) {
				writer.write(customModelJson.toString());
			} catch (IOException e) {
				throw new RuntimeException("Failed to create file: " + customModelJson);
			}

			// copy texture to pack
			try {
				FileUtils.copyFile(item.getValue().texturePath, new File(itemTexturesFolder + "/"  + packName + "_" + itemName + ".png"));
			} catch (Exception e) {
				throw new RuntimeException("Failed to copy texture to pack: " + item.getValue().texturePath + " -> " + new File(itemTexturesFolder + "/" + packName + "_" + itemName + ".png") + ": " + e);
			}
		}
	}
}
