package me.sekc.packman;

import java.util.Map;

public class CustomPlaceholders {
	Packman plugin;

	public CustomPlaceholders(Packman plugin) {
		this.plugin = plugin;
	}

	public String parseString(String input) {
		String resultString = input;

		int curIndex = 0;
		while (true) {
			String tagStart = "[[pmglyph:";
			String tagEnd = "]]";
			int startIndex = resultString.indexOf(tagStart, curIndex);
			if (startIndex == -1) break;

			int endIndex = resultString.indexOf(tagEnd, startIndex);
			if (endIndex == -1) break;

			String thisTagArgString = resultString.substring(startIndex + tagStart.length(), endIndex);
			plugin.getLogger().info(resultString + ": " + startIndex + " -> " + endIndex + " " + thisTagArgString);

			String[] thisTagArgs = thisTagArgString.split(":");
			if (thisTagArgs.length != 2) {
				throw new RuntimeException("Malformed tag, [[pmglyph: takes 2 arguments: " + tagStart + thisTagArgString + tagEnd);
			}

			Character glyphChar = plugin.packmanPackParser.glyphToCharMap.get(Map.entry(thisTagArgs[0], thisTagArgs[1]));
			String glyphString = glyphChar == null ? "INVALID GLYPH" : String.valueOf(glyphChar);

			resultString = resultString.substring(0, startIndex) + glyphString + resultString.substring(endIndex + tagEnd.length());
		};

		return resultString;
	}
}
