package me.sekc.packman.parser;

import java.io.File;
import java.util.HashMap;

public class PackmanGlyph {
	public File imagePath;
	public int height;
	public int ascent;

	PackmanGlyph(HashMap<String, ?> config, File pathToConfig) { // Hashmap is from yaml `getList`
		imagePath = new File(pathToConfig.getParentFile() + "/" + config.get("image"));
		height = (int)config.get("height");
		ascent = (int)config.get("ascent");
	}
}
