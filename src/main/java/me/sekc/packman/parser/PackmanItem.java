package me.sekc.packman.parser;

import java.io.File;
import java.util.HashMap;

public class PackmanItem {
	public File imagePath;

	PackmanItem(HashMap<String, ?> config, File pathToConfig) { // Hashmap is from yaml `getList`
		imagePath = new File(pathToConfig.getParentFile() + "/" + config.get("image"));
	}
}
